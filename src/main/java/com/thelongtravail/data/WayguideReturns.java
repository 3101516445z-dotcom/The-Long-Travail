package com.thelongtravail.data;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import java.util.*;

// 离线、死亡或掉落被取消时保留返还记录；不加载玩家或区块。
public final class WayguideReturns extends SavedData {
    private record Return(UUID owner, ItemStack item, boolean ready) {}
    private final Map<UUID, Return> pending = new LinkedHashMap<>();
    private final Map<UUID, LinkedHashSet<UUID>> readyByOwner = new HashMap<>();
    private record Wake(UUID owner, long due, long order) {}
    private final NavigableSet<Wake> queue = new TreeSet<>(Comparator.comparingLong(Wake::due).thenComparingLong(Wake::order));
    private final Map<UUID, Wake> scheduled = new HashMap<>();
    private final Set<UUID> online = new HashSet<>();
    private final Map<UUID, Integer> playerAttempts = new HashMap<>();
    private MinecraftServer server;
    private UUID processing, inFlight;
    private boolean draining, budgetInitialized;
    private int budgetTick, attempts, checks;
    private long order;
    private static final int GLOBAL_LIMIT = 16, PLAYER_LIMIT = 4, CHECK_LIMIT = 32, RETRY_TICKS = 20;
    private long nextWarning;
    public static WayguideReturns get(MinecraftServer server) {
        var data = server.overworld().getDataStorage().computeIfAbsent(WayguideReturns::load, WayguideReturns::new, "the_long_travail_wayguide_returns");
        if (data.server == null) {
            data.server = server;
            // 加载时只遍历在线玩家一次，不遍历所有离线返还记录。
            for (var player : server.getPlayerList().getPlayers()) data.activate(player);
        }
        return data;
    }
    public void enqueue(UUID entity, UUID owner, ItemStack item) {
        if (!entity.equals(inFlight) && !pending.containsKey(entity)) put(entity, new Return(owner, item.copy(), true));
    }
    public void hold(UUID entity, UUID owner, ItemStack item) {
        // 同一记录的物品正在交接时，回调不能重新托管或更换其主人。
        if (entity.equals(inFlight)) return;
        put(entity, new Return(owner, item.copy(), false));
    }
    public void refund(UUID entity) {
        var job = pending.get(entity);
        if (job != null && !job.ready) put(entity, new Return(job.owner, job.item, true));
    }
    public void release(UUID entity) { if (!entity.equals(inFlight)) remove(entity); }
    public int pendingRecords() { return pending.size(); }
    private void unindex(UUID entity, Return job) {
        if (job == null || !job.ready) return;
        var ids = readyByOwner.get(job.owner);
        if (ids != null && ids.remove(entity) && ids.isEmpty()) {
            readyByOwner.remove(job.owner); unschedule(job.owner);
        }
    }
    private void put(UUID entity, Return job) {
        var old = pending.put(entity, job);
        // 剩余数量更新不改变队列位置或重试时间。
        if (old == null || old.ready != job.ready || !old.owner.equals(job.owner)) {
            unindex(entity, old);
            if (job.ready) {
                readyByOwner.computeIfAbsent(job.owner, unused -> new LinkedHashSet<>()).add(entity);
                if (server != null) schedule(job.owner, now());
            }
        }
        setDirty();
    }
    private void remove(UUID entity) {
        var old = pending.remove(entity);
        if (old != null) { unindex(entity, old); setDirty(); }
    }
    private long now() { return server.overworld().getGameTime(); }
    private void unschedule(UUID owner) {
        var wake = scheduled.remove(owner);
        if (wake != null) queue.remove(wake);
    }
    private void schedule(UUID owner, long due) {
        if (!online.contains(owner) || !readyByOwner.containsKey(owner)
                || owner.equals(processing) || scheduled.containsKey(owner)) return;
        var wake = new Wake(owner, due, order++);
        scheduled.put(owner, wake); queue.add(wake);
    }
    // 登录、重生和换维度只唤醒调度；执行时重新获取当前玩家对象。
    public void activate(ServerPlayer player) {
        online.add(player.getUUID());
        schedule(player.getUUID(), server == null ? player.serverLevel().getGameTime() : now());
    }
    public void suspend(ServerPlayer player) {
        online.remove(player.getUUID()); unschedule(player.getUUID());
    }
    public void deliver(ServerPlayer player) {
        if (server == null) server = player.server;
        activate(player);
        // 正常玩家以 PlayerList 中的当前实例为准；直接调用也支持未加入列表的测试玩家。
        drain(now(), server.getTickCount(), id -> {
            var current = server.getPlayerList().getPlayer(id);
            return current != null ? current : id.equals(player.getUUID()) ? player : null;
        });
    }
    public void tick(MinecraftServer server) {
        drain(server.overworld().getGameTime(), server.getTickCount(), server.getPlayerList()::getPlayer);
    }
    private void drain(long now, int tick, java.util.function.Function<UUID, ServerPlayer> players) {
        if (draining) return;
        draining = true;
        try {
            if (!budgetInitialized || budgetTick != tick) {
                budgetInitialized = true; budgetTick = tick;
                attempts = checks = 0; playerAttempts.clear();
            }
            while (attempts < GLOBAL_LIMIT && checks < CHECK_LIMIT && !queue.isEmpty() && queue.first().due <= now) {
                Wake wake = queue.pollFirst(); scheduled.remove(wake.owner); checks++;
                var player = players.apply(wake.owner);
                if (player == null || !player.isAlive() || player.isRemoved()) { online.remove(wake.owner); continue; }
                if (playerAttempts.getOrDefault(wake.owner, 0) >= PLAYER_LIMIT) { schedule(wake.owner, now + 1); continue; }
                var ids = readyByOwner.get(wake.owner);
                if (ids == null || ids.isEmpty()) continue;
                UUID key = ids.iterator().next();
                Return job = pending.get(key);
                attempts++; playerAttempts.merge(wake.owner, 1, Integer::sum);
                processing = wake.owner; inFlight = key;
                boolean success = false;
                try { success = deliverOne(player, key, job); }
                catch (RuntimeException error) {
                    if (now >= nextWarning) {
                        nextWarning = now + 1200;
                        com.thelongtravail.TheLongTravail.LOGGER.error("Wayguide return retry failed; saved items remain pending", error);
                    }
                } finally {
                    processing = null; inFlight = null;
                    // 每轮一条，失败记录移到本玩家末尾，并延后该玩家的重试。
                    var remaining = readyByOwner.get(wake.owner);
                    if (remaining != null && remaining.remove(key)) remaining.add(key);
                    schedule(wake.owner, now + (success ? 0 : RETRY_TICKS));
                }
            }
        } finally { draining = false; }
    }
    private boolean deliverOne(ServerPlayer player, UUID key, Return job) {
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(new PlayerMainInvWrapper(player.getInventory()), job.item.copy(), false);
        // 插入部分先从权威记录扣除，再调用可能被取消或抛异常的实体生成回调。
        if (remainder.isEmpty()) remove(key);
        else put(key, new Return(job.owner, remainder, true));
        player.inventoryMenu.broadcastChanges();
        if (remainder.isEmpty()) return true;
        var drop = new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), remainder.copy());
        drop.setNoPickUpDelay(); drop.setTarget(player.getUUID());
        if (!player.serverLevel().addFreshEntity(drop)) return false;
        remove(key);
        return true;
    }
    private static WayguideReturns load(CompoundTag root) {
        var data = new WayguideReturns(); var list = root.getList("Returns", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            var tag = list.getCompound(i); var item = ItemStack.of(tag.getCompound("Item"));
            if (tag.hasUUID("Entity") && tag.hasUUID("Owner") && !item.isEmpty())
                // 搜索任务不跨重启恢复；所有托管记录在加载后转为可返还。
                data.put(tag.getUUID("Entity"), new Return(tag.getUUID("Owner"), item, true));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag root) {
        var list = new ListTag(); pending.forEach((id, job) -> {
            var tag = new CompoundTag(); tag.putUUID("Entity", id); tag.putUUID("Owner", job.owner);
            tag.put("Item", job.item.save(new CompoundTag())); list.add(tag);
        }); root.put("Returns", list); return root;
    }
}

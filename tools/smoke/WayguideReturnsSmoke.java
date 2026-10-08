package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.data.WayguideReturns;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;

// 真实物品插入/掉落回调，调度时钟可控；不改世界时间或持久化正式队列。
public final class WayguideReturnsSmoke {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static Object field(WayguideReturns q, String name) throws Exception {
        var f = WayguideReturns.class.getDeclaredField(name); f.setAccessible(true); return f.get(q);
    }
    private static void drain(WayguideReturns q, long now, int tick, Map<UUID, ServerPlayer> players) throws Exception {
        var m = WayguideReturns.class.getDeclaredMethod("drain", long.class, int.class, Function.class);
        m.setAccessible(true); m.invoke(q, now, tick, (Function<UUID, ServerPlayer>) players::get);
    }
    private static WayguideReturns load(CompoundTag tag) throws Exception {
        var m = WayguideReturns.class.getDeclaredMethod("load", CompoundTag.class); m.setAccessible(true);
        return (WayguideReturns) m.invoke(null, tag);
    }
    private static FakePlayer player(ServerLevel level, UUID id) {
        var p = new FakePlayer(level, new GameProfile(id, "ReturnProbe"));
        var wire = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        p.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), wire, p) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
        };
        return p;
    }
    private static ItemStack item() {
        var s = new ItemStack(ModRegistry.WAYGUIDE.get());
        s.getOrCreateTag().putString("ExternalData", "kept");
        s.setHoverName(net.minecraft.network.chat.Component.literal("Returned Wayguide")); return s;
    }
    private static int count(ServerPlayer p) {
        return p.getInventory().items.stream().filter(s -> s.is(ModRegistry.WAYGUIDE.get())).mapToInt(ItemStack::getCount).sum();
    }
    private static final class Drops {
        boolean cancel, fail, reenter;
        int calls;
        WayguideReturns queue; ServerPlayer player; UUID key;
        final List<ItemEntity> spawned = new ArrayList<>();
        @SubscribeEvent public void join(EntityJoinLevelEvent e) {
            if (!(e.getEntity() instanceof ItemEntity drop) || !drop.getItem().is(ModRegistry.WAYGUIDE.get())) return;
            calls++;
            if (reenter) {
                queue.refund(key); queue.enqueue(key, player.getUUID(), item()); queue.deliver(player);
            }
            if (fail) throw new IllegalStateException("expected return drop fixture");
            if (cancel) e.setCanceled(true); else spawned.add(drop);
        }
    }
    public static void run(ServerLevel level) throws Exception {
        long now = level.getGameTime();
        Map<UUID, ServerPlayer> players = new LinkedHashMap<>();
        var q = new WayguideReturns();
        for (int i = 0; i < 10000; i++) q.enqueue(UUID.randomUUID(), UUID.randomUUID(), item());
        for (int i = 0; i < 6; i++) {
            var p = player(level, UUID.randomUUID()); players.put(p.getUUID(), p);
            for (int j = 0; j < 8; j++) q.enqueue(UUID.randomUUID(), p.getUUID(), item());
            q.activate(p);
        }
        drain(q, now, 100, players);
        check(players.values().stream().mapToInt(WayguideReturnsSmoke::count).sum() == 16, "global sixteen attempts");
        check(players.values().stream().allMatch(p -> count(p) >= 2 && count(p) <= 4), "round-robin fairness");
        check((int)field(q, "checks") <= 32 && ((Map<?, ?>)field(q, "scheduled")).size() <= 6, "offline backlog excluded from dispatch");
        drain(q, now, 100, players);
        check(players.values().stream().mapToInt(WayguideReturnsSmoke::count).sum() == 16, "repeated same-tick drain shares budget");
        for (int t = 101; t < 106; t++) drain(q, now + t - 100, t, players);
        check(players.values().stream().allMatch(p -> count(p) == 8) && q.pendingRecords() == 10000, "all online owners finish without touching offline items");
        check(((Map<?, ?>)field(q, "scheduled")).isEmpty(), "no offline scheduling residue");

        var direct = new WayguideReturns(); var p = player(level, UUID.randomUUID());
        for (int i = 0; i < 9; i++) direct.enqueue(UUID.randomUUID(), p.getUUID(), item());
        for (int i = 0; i < 6; i++) direct.deliver(p);
        direct.tick(level.getServer());
        check(count(p) == 4 && direct.pendingRecords() == 5, "immediate and periodic calls share per-player budget");

        var saved = new WayguideReturns(); UUID held = UUID.randomUUID(); UUID owner = UUID.randomUUID();
        saved.hold(held, owner, item());
        var resurrected = load(saved.save(new CompoundTag()));
        var old = player(level, owner); var replacement = player(level, owner);
        resurrected.activate(old); resurrected.suspend(old); resurrected.activate(replacement);
        drain(resurrected, now, 1, Map.of(owner, replacement));
        check(count(old) == 0 && count(replacement) == 1 && resurrected.pendingRecords() == 0, "held record loads ready and resolves current player");
        var returned = replacement.getInventory().items.stream().filter(s -> s.is(ModRegistry.WAYGUIDE.get())).findFirst().orElseThrow();
        check(returned.getTag().getString("ExternalData").equals("kept") && returned.getHoverName().getString().equals("Returned Wayguide"), "custom item data preserved");
        var heldOnly = new WayguideReturns(); heldOnly.hold(held, owner, item()); heldOnly.activate(replacement);
        drain(heldOnly, now, 1, Map.of(owner, replacement)); check(heldOnly.pendingRecords() == 1, "live search not refunded");
        heldOnly.release(held); check(((Map<?, ?>)field(heldOnly, "readyByOwner")).isEmpty(), "release removes index");
        var dead = player(level, owner); dead.setHealth(0); heldOnly.enqueue(held, owner, item()); heldOnly.activate(dead);
        drain(heldOnly, now, 2, Map.of(owner, dead)); check(heldOnly.pendingRecords() == 1, "death retains item");
        heldOnly.activate(replacement); drain(heldOnly, now, 3, Map.of(owner, replacement));
        check(heldOnly.pendingRecords() == 0, "respawn wakes pending owner");

        var drops = new Drops(); MinecraftForge.EVENT_BUS.register(drops);
        try {
            var full = player(level, UUID.randomUUID());
            for (int i = 0; i < full.getInventory().items.size(); i++) full.getInventory().items.set(i, new ItemStack(Items.STONE, 64));
            full.getAbilities().instabuild = true;
            var retry = new WayguideReturns(); UUID key = UUID.randomUUID(); retry.enqueue(key, full.getUUID(), item()); retry.activate(full);
            drops.cancel = true; drops.queue = retry; drops.player = full; drops.key = key; drops.reenter = true;
            drain(retry, now, 200, Map.of(full.getUUID(), full));
            check(drops.calls == 1 && retry.pendingRecords() == 1, "creative full inventory and reentry retain exactly one record");
            for (int t = 1; t < 20; t++) drain(retry, now + t, 200 + t, Map.of(full.getUUID(), full));
            check(drops.calls == 1, "failed delivery delayed twenty ticks");
            drops.cancel = false; drops.reenter = false;
            drain(retry, now + 20, 220, Map.of(full.getUUID(), full));
            check(drops.calls == 2 && retry.pendingRecords() == 0 && drops.spawned.size() == 1, "retry drops once");

            // 部分入包后掉落回调抛异常，重试不能再返还已入包的数量。
            var partial = new WayguideReturns(); var stack = item(); int max = stack.getMaxStackSize();
            check(max > 1, "stackable Wayguide fixture");
            var existing = stack.copy(); existing.setCount(max - 1); full.getInventory().items.set(0, existing);
            stack.setCount(2); key = UUID.randomUUID(); partial.enqueue(key, full.getUUID(), stack); partial.activate(full);
            drops.queue = partial; drops.key = key; drops.fail = true;
            drain(partial, now, 300, Map.of(full.getUUID(), full));
            check(count(full) == max && partial.pendingRecords() == 1, "partial insert committed before callback exception");
            var snapshot = partial.save(new CompoundTag());
            check(ItemStack.of(snapshot.getList("Returns", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0).getCompound("Item")).getCount() == 1, "only remainder persisted");
            drops.fail = false; drain(partial, now + 20, 320, Map.of(full.getUUID(), full));
            check(partial.pendingRecords() == 0 && count(full) == max && drops.spawned.get(drops.spawned.size() - 1).getItem().getCount() == 1, "exception retry returns only remainder");
        } finally { MinecraftForge.EVENT_BUS.unregister(drops); drops.spawned.forEach(ItemEntity::discard); }
        System.out.println("WAYGUIDE_RETURNS_PASS: 10000 offline records, owner index, 16/4 shared budgets, fairness, restart/respawn, held/release, data preservation, creative overflow, callback reentry, delayed retry, partial insertion exception");
    }
}

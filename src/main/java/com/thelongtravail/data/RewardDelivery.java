package com.thelongtravail.data;

import com.thelongtravail.config.TravailConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;

// 所有维度共用由主世界保存的持久化队列，不主动加载目标区块。
public final class RewardDelivery extends SavedData {
    private static final String NAME = "the_long_travail_rewards";
    private final ArrayDeque<Job> jobs = new ArrayDeque<>();
    private int reserved;
    private long lastTick = Long.MIN_VALUE;
    private long nextWarning;
    private static final class Job {
        final ResourceKey<Level> dimension;
        final Vec3 position;
        final ItemStack template;
        int count;
        Job(ResourceKey<Level> dimension, Vec3 position, ItemStack template, int count) {
            this.dimension = dimension; this.position = position; this.template = template.copy();
            this.template.setCount(1); this.count = count;
        }
    }
    public static RewardDelivery get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(RewardDelivery::load, RewardDelivery::new, NAME);
    }
    // 造成伤害前预留名额，防止嵌套死亡监听器抢占。
    public Reservation reserve() {
        if (jobs.size() + reserved >= TravailConfig.REWARD_QUEUE_CAPACITY.get()) return null;
        reserved++;
        return new Reservation();
    }
    public final class Reservation implements AutoCloseable {
        private boolean closed;
        public void submit(ServerLevel level, Vec3 position, ItemStack item, int count) {
            if (closed) throw new IllegalStateException("Reward reservation already closed");
            if (!item.isEmpty() && count > 0) {
                if (TravailConfig.REWARD_OVERFLOW.get() == TravailConfig.RewardOverflow.LIMIT)
                    count = (int) Math.min(count, (long) Math.max(1, item.getMaxStackSize()) * TravailConfig.REWARD_LIMIT.get());
                // 仅合并目的地和物品数据均相同的记录，且每条记录都须遵守数量上限。
                Job tail = jobs.peekLast();
                if (tail != null && tail.dimension.equals(level.dimension()) && tail.position.equals(position)
                        && ItemStack.isSameItemSameTags(tail.template, item) && (long) tail.count + count <= 4096) tail.count += count;
                else jobs.addLast(new Job(level.dimension(), position, item, count));
                setDirty();
            }
            close();
        }
        @Override public void close() { if (!closed) { closed = true; reserved--; } }
    }
    public void warnFull(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (now < nextWarning && now >= nextWarning - 1200) return;
        nextWarning = now + 1200;
        com.thelongtravail.TheLongTravail.LOGGER.warn("Reward queue full ({} records): pausing automatic phantom kills; fishing item bonuses may be skipped. Pending rewards remain saved.", jobs.size());
    }
    public void tick(MinecraftServer server) {
        long now = server.getTickCount();
        if (now == lastTick) return;
        lastTick = now;
        int budget = TravailConfig.REWARD_ENTITIES_PER_TICK.get();
        int attempts = Math.min(jobs.size() + budget, budget * 4);
        while (budget > 0 && attempts-- > 0 && !jobs.isEmpty()) {
            Job job = jobs.removeFirst();
            try {
            ServerLevel level = server.getLevel(job.dimension);
            if (level != null && level.hasChunkAt(BlockPos.containing(job.position))) {
                int count = Math.min(job.count, Math.max(1, job.template.getMaxStackSize()));
                ItemStack stack = job.template.copy(); stack.setCount(count);
                ItemEntity entity = new ItemEntity(level, job.position.x, job.position.y, job.position.z, stack);
                // 生成失败或被取消也消耗尝试额度，但不能丢弃奖励。
                budget--;
                if (level.addFreshEntity(entity)) { job.count -= count; setDirty(); }
            }
            } finally {
                // 事件监听器可能抛出异常，异常退出时仍须保留正在投递的记录。
                if (job.count > 0) jobs.addLast(job);
            }
        }
    }
    public int pendingRecords() { return jobs.size(); }
    public long pendingItems() { return jobs.stream().mapToLong(job -> job.count).sum(); }
    public static RewardDelivery load(CompoundTag root) {
        RewardDelivery queue = new RewardDelivery();
        ListTag entries = root.getList("Jobs", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag tag = entries.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("Dimension"));
            ItemStack item = ItemStack.of(tag.getCompound("Item"));
            Vec3 pos = new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
            int count = tag.getInt("Count");
            if (id == null || item.isEmpty() || count <= 0 || !Double.isFinite(pos.x) || !Double.isFinite(pos.y) || !Double.isFinite(pos.z)) {
                com.thelongtravail.TheLongTravail.LOGGER.warn("Ignoring invalid saved reward record {}", i);
                continue;
            }
            queue.jobs.addLast(new Job(ResourceKey.create(Registries.DIMENSION, id), pos, item, count));
        }
        return queue;
    }
    @Override public CompoundTag save(CompoundTag root) {
        ListTag entries = new ListTag();
        for (Job job : jobs) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dimension", job.dimension.location().toString());
            tag.putDouble("X", job.position.x); tag.putDouble("Y", job.position.y); tag.putDouble("Z", job.position.z);
            tag.put("Item", job.template.save(new CompoundTag())); tag.putInt("Count", job.count);
            entries.add(tag);
        }
        root.put("Jobs", entries); return root;
    }
}

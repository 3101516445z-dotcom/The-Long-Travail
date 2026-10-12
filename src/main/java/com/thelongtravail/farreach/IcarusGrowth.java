package com.thelongtravail.farreach;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.boundless.TimeStopManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

// 预留的生长工具，尚未接入伊卡洛斯或事件总线；调用方须自行检查生效条件。
public final class IcarusGrowth {
    private static final int RADIUS = 8;
    private static final int HEIGHT = 4;
    private static final int INTERVAL_TICKS = 20;
    public static final TagKey<Block> EXTRA =
            TagKey.create(
                    Registries.BLOCK, new ResourceLocation(TheLongTravail.MODID, "icarus_growth"));
    private static final Map<ServerLevel, Map<Long, Long>> LAST = new WeakHashMap<>();

    public static boolean eligible(BlockState state) {
        Block b = state.getBlock();
        return state.isRandomlyTicking()
                && (b instanceof CropBlock
                        || b instanceof StemBlock
                        || b instanceof NetherWartBlock
                        || b instanceof SaplingBlock
                        || state.is(EXTRA));
    }

    public static boolean grow(ServerLevel level, BlockPos pos, long now) {
        if (!level.hasChunkAt(pos) || TimeStopManager.frozen(level, pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (!eligible(state)) return false;
        // 树木可能长到树苗所在区块之外，须先确认周围 3×3 个区块已加载。
        if (state.getBlock() instanceof SaplingBlock && !level.isAreaLoaded(pos, 16)) return false;
        Map<Long, Long> times = LAST.computeIfAbsent(level, k -> new HashMap<>());
        int interval = INTERVAL_TICKS;
        if (now - times.getOrDefault(pos.asLong(), now - interval) < interval) return false;
        times.put(pos.asLong(), now);
        state.randomTick(level, pos, level.random);
        return true;
    }

    public static void tick(ServerLevel level, java.util.function.Predicate<ServerPlayer> source) {
        long now = level.getGameTime();
        int radius = RADIUS, height = HEIGHT;
        int interval = INTERVAL_TICKS;
        int phase = Math.floorMod(now, interval);
        for (ServerPlayer player : level.players()) {
            if (!source.test(player) || TimeStopManager.frozen(player)) continue;
            BlockPos center = player.blockPosition();
            // 按固定世界坐标将扫描区域划分为条带，避免玩家移动时反复扫描同一片区域。
            for (int dx = -radius; dx <= radius; dx++)
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius) continue;
                    int x = center.getX() + dx, z = center.getZ() + dz;
                    if (Math.floorMod(x * 31L + z, interval) != phase) continue;
                    for (int dy = -height; dy <= height; dy++)
                        grow(level, new BlockPos(x, center.getY() + dy, z), now);
                }
        }
        Map<Long, Long> times = LAST.get(level);
        if (times != null && now % 200 == 0) {
            times.values().removeIf(t -> now - t > Math.max(200, interval));
            if (times.isEmpty()) LAST.remove(level);
        }
    }

    private IcarusGrowth() {}
}

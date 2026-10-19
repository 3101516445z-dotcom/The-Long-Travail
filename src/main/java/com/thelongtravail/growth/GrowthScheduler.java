package com.thelongtravail.growth;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.boundless.TimeStopManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.lang.ref.WeakReference;
import java.util.*;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class GrowthScheduler {
    public static final TagKey<Block> INCLUDE = tag("growth_include"), EXCLUDE = tag("growth_exclude");
    private static final int SCANS = 2048, CALLS = 128, TREES = 1, MAX_SOURCES = 1024, MAX_RECENT = 65536;

    public record Policy(int radius, int height, int interval) {
        public Policy {
            if (radius < 1 || radius > 32 || height < 0 || height > 16 || interval < 1 || interval > 1200000)
                throw new IllegalArgumentException("Invalid growth policy");
        }
    }

    private record SourceKey(ResourceLocation kind, UUID owner) {}

    private static final class Source {
        final WeakReference<LivingEntity> owner;
        Policy policy;
        long heartbeat;
        int cursor;

        Source(LivingEntity entity, Policy policy, long now) {
            owner = new WeakReference<>(entity);
            this.policy = policy;
            heartbeat = now;
        }
    }

    private record Position(ServerLevel level, long pos) {}

    private static final Map<MinecraftServer, State> STATES = new WeakHashMap<>();

    private static final class State {
        final LinkedHashMap<SourceKey, Source> sources = new LinkedHashMap<>();
        final LinkedHashMap<Position, Long> recent = new LinkedHashMap<>();
        int rotation;
        final Map<GrowthAdapter, Long> warnings = new IdentityHashMap<>();
    }

    private static final List<GrowthAdapter> ADAPTERS = new ArrayList<>();
    private static final GrowthAdapter NATIVE =
            new GrowthAdapter() {
                public boolean supports(BlockState state) {
                    Block b = state.getBlock();
                    return b instanceof CropBlock
                            || b instanceof SaplingBlock
                            || b instanceof StemBlock
                            || b instanceof NetherWartBlock
                            || b instanceof CocoaBlock
                            || b instanceof SweetBerryBushBlock
                            || b instanceof SugarCaneBlock
                            || b instanceof CactusBlock
                            || b instanceof BambooStalkBlock
                            || b instanceof BambooSaplingBlock
                            || b instanceof KelpBlock
                            || b instanceof WeepingVinesBlock
                            || b instanceof TwistingVinesBlock
                            || state.is(INCLUDE);
                }

                public boolean heavy(BlockState state) {
                    return state.getBlock() instanceof SaplingBlock;
                }

                public boolean ready(ServerLevel level, BlockPos pos, BlockState state) {
                    return level.isAreaLoaded(pos, heavy(state) ? 16 : 1);
                }

                public void attempt(ServerLevel level, BlockPos pos, BlockState state) {
                    state.randomTick(level, pos, level.random);
                }
            };

    public static void register(GrowthAdapter adapter) {
        ADAPTERS.add(Objects.requireNonNull(adapter));
    }

    private static TagKey<Block> tag(String path) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation(TheLongTravail.MODID, path));
    }

    public static void submit(ResourceLocation kind, LivingEntity entity, Policy policy) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        State state = STATES.computeIfAbsent(level.getServer(), s -> new State());
        SourceKey key = new SourceKey(kind, entity.getUUID());
        Source source = state.sources.get(key);
        if (source == null || source.owner.get() != entity) {
            if (state.sources.size() >= MAX_SOURCES) return;
            state.sources.put(key, new Source(entity, policy, level.getServer().getTickCount()));
        } else {
            source.policy = policy;
            source.heartbeat = level.getServer().getTickCount();
        }
    }

    public static void remove(ResourceLocation kind, LivingEntity entity) {
        if (entity.getServer() != null) {
            State s = STATES.get(entity.getServer());
            if (s != null) s.sources.remove(new SourceKey(kind, entity.getUUID()));
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        STATES.remove(event.getServer());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        State s = STATES.get(event.getServer());
        if (s == null) return;
        long now = event.getServer().getTickCount();
        s.sources.values().removeIf(v -> v.owner.get() == null || now - v.heartbeat > 1);
        var entries = new ArrayList<>(s.sources.values());
        if (entries.isEmpty()) {
            s.recent.clear();
            return;
        }
        int scans = SCANS, calls = CALLS, trees = TREES;
        Set<GrowthAdapter> failed = Collections.newSetFromMap(new IdentityHashMap<>());
        var adapters = List.copyOf(ADAPTERS);
        int start = Math.floorMod(s.rotation++, entries.size());
        for (int i = 0; i < entries.size() && scans > 0 && calls > 0; i++) {
            Source source = entries.get((start + i) % entries.size());
            LivingEntity owner = source.owner.get();
            if (owner == null
                    || !owner.isAlive()
                    || owner.isRemoved()
                    || TimeStopManager.frozen(owner)
                    || !(owner.level() instanceof ServerLevel level)) continue;
            Policy p = source.policy;
            int side = 2 * p.radius + 1, high = 2 * p.height + 1, total = side * side * high;
            int allowance = Math.min(scans, Math.max(1, (total + p.interval - 1) / p.interval));
            BlockPos center = owner.blockPosition();
            for (int n = 0; n < allowance && calls > 0; n++) {
                scans--;
                int k = Math.floorMod(source.cursor++, total);
                int dx = k % side - p.radius, dz = k / side % side - p.radius, dy = k / (side * side) - p.height;
                if (dx * dx + dz * dz > p.radius * p.radius) continue;
                BlockPos pos = center.offset(dx, dy, dz);
                if (!level.hasChunkAt(pos) || TimeStopManager.frozen(level, pos)) continue;
                BlockState state = level.getBlockState(pos);
                if (!state.isRandomlyTicking() || state.is(EXCLUDE)) continue;
                GrowthAdapter adapter = null;
                boolean selectionFailed = false;
                for (var candidate : adapters) {
                    if (failed.contains(candidate)) continue;
                    try {
                        if (candidate.supports(state)) {
                            adapter = candidate;
                            break;
                        }
                    } catch (RuntimeException ex) {
                        failure(s, failed, candidate, level, pos, ex);
                        selectionFailed = true;
                        break;
                    }
                }
                if (selectionFailed) continue;
                if (adapter == null) adapter = NATIVE;
                if (failed.contains(adapter)) continue;
                try {
                    if (adapter == NATIVE && !adapter.supports(state)) continue;
                    boolean heavy = adapter.heavy(state);
                    if (!adapter.ready(level, pos, state) || heavy && trees <= 0) continue;
                    Position key = new Position(level, pos.asLong());
                    Long until = s.recent.get(key);
                    if (until != null && until > now) continue;
                    if (until == null && s.recent.size() >= MAX_RECENT) continue;
                    s.recent.remove(key);
                    s.recent.put(key, now + p.interval);
                    // 调用前消耗预算和记录间隔；失败可能已有副作用，不能重放。
                    calls--;
                    if (heavy) trees--;
                    adapter.attempt(level, pos, state);
                } catch (RuntimeException ex) {
                    failure(s, failed, adapter, level, pos, ex);
                }
            }
        }
        int cleanup = 256;
        var it = s.recent.entrySet().iterator();
        var retained = new ArrayList<Map.Entry<Position, Long>>();
        while (cleanup-- > 0 && it.hasNext()) {
            var entry = it.next();
            if (entry.getValue() > now) retained.add(Map.entry(entry.getKey(), entry.getValue()));
            it.remove();
        }
        for (var entry : retained) s.recent.put(entry.getKey(), entry.getValue());
    }

    private static void failure(
            State state,
            Set<GrowthAdapter> failed,
            GrowthAdapter adapter,
            ServerLevel level,
            BlockPos pos,
            RuntimeException failure) {
        failed.add(adapter);
        long now = System.nanoTime();
        Long next = state.warnings.get(adapter);
        if (next == null || now - next >= 0) {
            state.warnings.put(adapter, now + 60_000_000_000L);
            TheLongTravail.LOGGER.warn(
                    "Growth adapter {} failed in {} at {} ({}); skipped for this tick",
                    adapter.getClass().getName(),
                    level.dimension().location(),
                    pos,
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()),
                    failure);
        }
    }

    private GrowthScheduler() {}
}

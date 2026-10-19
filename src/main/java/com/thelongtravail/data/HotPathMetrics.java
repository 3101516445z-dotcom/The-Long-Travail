package com.thelongtravail.data;

import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.LongAccumulator;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "the_long_travail")
public final class HotPathMetrics {
    private static final boolean ENABLED = Boolean.getBoolean("travail.hotpath.metrics");

    public enum Counter {
        SYNC_ENTITY_VISITS,
        OWNER_LOOKUPS,
        SYNC_PACKETS,
        TASK_CONTAINER_SCANS,
        TASK_VISITS,
        NBT_STEPS,
        NBT_SYNC_NODES,
        LIGHT_CANDIDATES,
        EXECUTION_SECTION_BATCHES,
        EXECUTION_SECTION_VISITS,
        EXECUTION_INFINITE,
        EXECUTION_GENERIC,
        EXECUTION_DIRECT,
        EXECUTION_NONFINITE,
        PROJECTILE_INDEX_CHECKS,
        RELEASE_CANDIDATES,
        RELEASE_TASKS,
        RELEASE_SHAPES,
        RELEASE_NEIGHBORS,
        RELEASE_EDGES,
        RELEASE_NANOS,
        STORAGE_PLAYER_NANOS,
        STORAGE_ITEM_NANOS,
        STORAGE_GRAVE_NANOS,
        STORAGE_ENTITY_VISITS,
        STORAGE_SWEEPS,
        STORAGE_SPENT,
        STORAGE_WITNESSES,
        STORAGE_GRAVES,
        JOURNEY_REQUESTS,
        JOURNEY_CONFIRMED,
        JOURNEY_EXPIRED,
        LANTERN_DIRTY_SECTIONS,
        STORAGE_INDEX_SEED_VISITS,
        RELEASE_DENSE_BATCHES,
        RELEASE_SPARSE_BATCHES;
        private final LongAdder count = new LongAdder();

        public void add(long value) {
            if (ENABLED) count.add(value);
        }

        public long value() {
            return count.sum();
        }

        public void set(long value) {
            if (ENABLED) {
                count.reset();
                count.add(value);
            }
        }
    }

    public enum Peak {
        FROZEN_QUEUE_SIZE,
        RELEASE_CANDIDATES_PER_BATCH,
        RELEASED_PER_BATCH,
        RELEASE_NANOS,
        STORAGE_PLAYER_NANOS,
        STORAGE_ITEM_NANOS,
        STORAGE_GRAVE_NANOS;
        private final LongAccumulator maximum = new LongAccumulator(Long::max, 0);

        public void record(long value) {
            if (ENABLED) maximum.accumulate(value);
        }

        public long value() {
            return maximum.get();
        }
    }

    public static long start() {
        return ENABLED ? System.nanoTime() : 0;
    }

    public static void elapsed(Counter counter, long start) {
        if (!ENABLED) return;
        long elapsed = System.nanoTime() - start;
        counter.add(elapsed);
        Peak peak =
                switch (counter) {
                    case RELEASE_NANOS -> Peak.RELEASE_NANOS;
                    case STORAGE_PLAYER_NANOS -> Peak.STORAGE_PLAYER_NANOS;
                    case STORAGE_ITEM_NANOS -> Peak.STORAGE_ITEM_NANOS;
                    case STORAGE_GRAVE_NANOS -> Peak.STORAGE_GRAVE_NANOS;
                    default -> null;
                };
        if (peak != null) peak.record(elapsed);
    }

    public static void reset() {
        for (var counter : Counter.values()) counter.count.reset();
        for (var peak : Peak.values()) peak.maximum.reset();
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void starting(net.minecraftforge.event.server.ServerAboutToStartEvent event) {
        reset();
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        if (!ENABLED) return;
        var values = new java.util.LinkedHashMap<String, Long>();
        for (var counter : Counter.values()) values.put(counter.name(), counter.value());
        com.thelongtravail.TheLongTravail.LOGGER.info(
                "Travail hot path metrics (counts / cumulative nanos / ledger sizes): {}", values);
        var peaks = new java.util.LinkedHashMap<String, Long>();
        for (var peak : Peak.values()) peaks.put(peak.name(), peak.value());
        com.thelongtravail.TheLongTravail.LOGGER.info(
                "Travail hot path peaks (per queue / per batch / nanos): {}", peaks);
    }

    private HotPathMetrics() {}
}

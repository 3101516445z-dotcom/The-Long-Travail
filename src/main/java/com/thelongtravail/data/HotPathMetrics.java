package com.thelongtravail.data;

import java.util.concurrent.atomic.LongAdder;

// 仅通过 JVM 测试参数启用；默认不计数、不输出日志。
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "the_long_travail")
public final class HotPathMetrics {
    private static final boolean ENABLED=Boolean.getBoolean("travail.hotpath.metrics");
    public enum Counter {
        SYNC_ENTITY_VISITS, OWNER_LOOKUPS, SYNC_PACKETS, TASK_CONTAINER_SCANS,
        TASK_VISITS, NBT_STEPS, LIGHT_CANDIDATES, EXECUTION_INFINITE, EXECUTION_GENERIC, EXECUTION_DIRECT, EXECUTION_NONFINITE,
        PROJECTILE_INDEX_CHECKS, RELEASE_CANDIDATES, RELEASE_TASKS, RELEASE_SHAPES, RELEASE_NEIGHBORS, RELEASE_EDGES,
        RELEASE_NANOS, STORAGE_PLAYER_NANOS, STORAGE_ITEM_NANOS, STORAGE_GRAVE_NANOS, STORAGE_ENTITY_VISITS,
        STORAGE_SWEEPS, STORAGE_SPENT, STORAGE_WITNESSES, STORAGE_GRAVES, JOURNEY_REQUESTS, JOURNEY_CONFIRMED,
        JOURNEY_EXPIRED, LANTERN_DIRTY_SECTIONS;
        private final LongAdder count=new LongAdder();
        public void add(long value){if(ENABLED)count.add(value);}
        public long value(){return count.sum();}
        public void set(long value){if(ENABLED){count.reset();count.add(value);}}
    }
    public static long start(){return ENABLED?System.nanoTime():0;}
    public static void elapsed(Counter counter,long start){if(ENABLED)counter.add(System.nanoTime()-start);}
    public static void reset(){for(var counter:Counter.values())counter.count.reset();}
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void starting(net.minecraftforge.event.server.ServerAboutToStartEvent event) { reset(); }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        if (!ENABLED) return;
        var values = new java.util.LinkedHashMap<String,Long>();
        for (var counter : Counter.values()) values.put(counter.name(), counter.value());
        com.thelongtravail.TheLongTravail.LOGGER.info("Travail hot path metrics (counts / cumulative nanos / ledger sizes): {}", values);
    }
    private HotPathMetrics(){}
}

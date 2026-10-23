package com.thelongtravail.diagnostics;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public final class RuntimeDiagnostics {
    public static final boolean ENABLED = Boolean.getBoolean("thelongtravail.diagnostics");

    public enum Metric {
        FOOD_POOL_NS,
        FLUID_IDENTIFY_NS,
        FLUID_PREVIEW_NS,
        FLUID_PREPARE_NS,
        FLUID_COMMIT_NS,
        FLUID_ROLLBACK_NS,
        FLUID_NODES,
        FLUID_STORES,
        REPLAY_DEPTH,
        REPLAY_WAIT_TICKS,
        REPLAY_WAIT_NS,
        REPLAY_CAPACITY_DROP
    }

    public record Sample(long count, long total, long max, long last) {}

    private static final class Counter {
        final AtomicLong count = new AtomicLong(),
                total = new AtomicLong(),
                max = new AtomicLong(),
                last = new AtomicLong();

        void add(long value) {
            count.incrementAndGet();
            total.addAndGet(value);
            max.accumulateAndGet(value, Math::max);
            last.set(value);
        }

        Sample read() {
            return new Sample(count.get(), total.get(), max.get(), last.get());
        }
    }

    private static final Counter[] COUNTERS = counters();

    private static Counter[] counters() {
        if (!ENABLED) return new Counter[0];
        Counter[] result = new Counter[Metric.values().length];
        Arrays.setAll(result, i -> new Counter());
        return result;
    }

    public static long start() {
        return ENABLED ? System.nanoTime() : 0;
    }

    public static void end(Metric metric, long start) {
        if (ENABLED) observe(metric, System.nanoTime() - start);
    }

    public static void observe(Metric metric, long value) {
        if (ENABLED) COUNTERS[metric.ordinal()].add(value);
    }

    // 应在被测操作完成后读取；并发读取的统计值仅供参考。
    public static Map<Metric, Sample> snapshot() {
        if (!ENABLED) return Map.of();
        var result = new EnumMap<Metric, Sample>(Metric.class);
        for (Metric metric : Metric.values()) result.put(metric, COUNTERS[metric.ordinal()].read());
        return Collections.unmodifiableMap(result);
    }

    private RuntimeDiagnostics() {}
}

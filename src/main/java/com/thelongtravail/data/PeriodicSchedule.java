package com.thelongtravail.data;

/** Stable task phases: spread periodic work without changing its frequency or using gameplay RNG. */
public final class PeriodicSchedule {
    public static final int FLUID = 0x3F97A251;
    public static final int FAR_REACH = 0x6B124EC7;

    public static boolean due(long gameTime, int playerHash, int task, int interval) {
        if (interval <= 1) return true;
        int hash = playerHash ^ task;
        hash ^= hash >>> 16;
        hash *= 0x7FEB352D;
        hash ^= hash >>> 15;
        long phase = Math.floorMod(hash, interval);
        // Reduce time first to avoid long overflow near the clock's limits.
        return (Math.floorMod(gameTime, interval) + phase) % interval == 0;
    }

    private PeriodicSchedule() {}
}

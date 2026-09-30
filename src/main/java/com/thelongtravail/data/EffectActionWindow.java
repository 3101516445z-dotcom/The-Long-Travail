package com.thelongtravail.data;

/** Rolling 20-game-tick window. Failed attempts count; unused allowance never accumulates. */
public final class EffectActionWindow {
    private final int[] counts = new int[20];
    private long previous;
    private boolean initialized;
    private int total;
    public boolean acquire(long tick, int limit) {
        if (limit <= 0) return true;
        if (!initialized || tick < previous || tick - previous < 0 || tick - previous >= 20) {
            java.util.Arrays.fill(counts, 0); total = 0; initialized = true;
        } else {
            for (long step = 1; step <= tick - previous; step++) {
                int index = Math.floorMod(previous + step, 20);
                total -= counts[index]; counts[index] = 0;
            }
        }
        previous = tick;
        if (total >= limit) return false;
        counts[Math.floorMod(tick, 20)]++; total++;
        return true;
    }
}

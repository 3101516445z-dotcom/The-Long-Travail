package com.thelongtravail.common;

/** Time conversion policies shared by mechanics; callers choose their existing rounding policy. */
public final class TickDurations {
    /** Round upward, allowing zero but clamping negative results to zero. */
    public static int ceilNonNegative(double seconds) {
        return Math.max(0, (int) Math.ceil(seconds * 20));
    }

    /** Round upward while retaining signed values and Java's narrowing behavior. */
    public static int ceilSigned(double seconds) {
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(seconds * 20));
    }

    /** Round to nearest tick, with a minimum duration of one tick. */
    public static int roundAtLeastOne(double seconds) {
        if (seconds <= 0.0D) return 1;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, Math.round(seconds * 20.0D)));
    }

    private TickDurations() {}
}

package com.thelongtravail.data;

/** 区分本模组上次写入的速度和其他模组新提供的速度。 */
public final class FlightSpeedAdjustment {
    public record Result(float baseline, float applied) {}
    public static boolean same(float first, float second) { return Math.abs(first - second) <= 1.0E-6F; }
    public static Result apply(float current, float baseline, float lastApplied, double reduction) {
        float base = same(current, lastApplied) ? baseline : current;
        return new Result(base, base * (1F - (float) Math.max(0D, Math.min(1D, reduction))));
    }
    public static float restore(float current, float baseline, float lastApplied) {
        return same(current, lastApplied) ? baseline : current;
    }
    private FlightSpeedAdjustment() {}
}

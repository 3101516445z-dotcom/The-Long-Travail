package com.thelongtravail.client;

/** Frame-rate independent charge and release animation, in seconds. */
public final class DiaryHoldProgress {
    public static final double DURATION = 1.5;
    private double seconds;

    public boolean advance(double elapsedSeconds, boolean holding) {
        seconds = Math.max(0, Math.min(DURATION, seconds + Math.max(0, elapsedSeconds) * (holding ? 1 : -1)));
        return seconds >= DURATION;
    }

    public double fraction() { return seconds / DURATION; }
    public void reset() { seconds = 0; }
}

package com.thelongtravail.client.diary;

public final class BookmarkAnimation {
    private static final double RESPONSE = 18.0;

    public static float approach(float current, float target, double elapsedSeconds) {
        double blend = -Math.expm1(-RESPONSE * Math.max(0, elapsedSeconds));
        return (float) (current + (target - current) * blend);
    }

    private BookmarkAnimation() {}
}

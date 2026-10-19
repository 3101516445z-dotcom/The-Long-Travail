package com.thelongtravail.client;

public final class LanternLighting {
    public static final double RADIUS = 12.0;
    public static final double FULL_BRIGHT_RADIUS = 3.0;

    public record Source(double x, double y, double z) {}

    public static double brightness(Source[] sources, double x, double y, double z) {
        double nearestSquared = RADIUS * RADIUS;
        for (Source s : sources) {
            double dx = x - s.x();
            double dy = y - s.y();
            double dz = z - s.z();
            nearestSquared = Math.min(nearestSquared, dx * dx + dy * dy + dz * dz);
        }
        if (nearestSquared <= FULL_BRIGHT_RADIUS * FULL_BRIGHT_RADIUS) return 15.0;
        double remaining = RADIUS - Math.sqrt(nearestSquared);
        return Math.min(15.0, remaining * (15.0 / (RADIUS - FULL_BRIGHT_RADIUS)));
    }

    public static int merge(Source[] sources, double x, double y, double z, int packed) {
        int existing = packed & 255;
        if (existing >= 240 || sources.length == 0) return packed;
        int sampled = Math.min(240, (int) Math.round(brightness(sources, x, y, z) * 16.0));
        // Minecraft 光照坐标的低字节表示方块光；增加差值不触及其余位。
        return packed + Math.max(0, sampled - existing);
    }

    private LanternLighting() {}
}

package com.thelongtravail.client;

// 纯计算部分；不读取世界光照、不写入世界状态。光照采样线程使用不可变快照。
public final class LanternLighting {
    public static final double RADIUS = 12.0;
    public static final double FULL_BRIGHT_RADIUS = 3.0;

    public record Source(double x, double y, double z) {}

    public static double brightness(Source[] sources, double x, double y, double z) {
        // 所有腰带亮度相同且不叠加，因此只需最近光源的距离；整次采样只开方一次。
        double nearestSquared = RADIUS * RADIUS;
        for (Source s : sources) {
            double dx = x - s.x();
            double dy = y - s.y();
            double dz = z - s.z();
            nearestSquared = Math.min(nearestSquared, dx * dx + dy * dy + dz * dz);
        }
        // 核心区域保持最高亮度；仍须检查其他候选光源，确保异常坐标的处理方式不变。
        if (nearestSquared <= FULL_BRIGHT_RADIUS * FULL_BRIGHT_RADIUS) return 15.0;
        double remaining = RADIUS - Math.sqrt(nearestSquared);
        // 近处保持满亮度，外围线性衰减至零；多个光源仍不叠加。
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

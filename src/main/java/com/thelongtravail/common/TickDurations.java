package com.thelongtravail.common;

// 提供不同取整规则的时间换算；调用方按机制选择，不能互换。
public final class TickDurations {
    // 向上取整，负数归零，允许零刻。
    public static int ceilNonNegative(double seconds) {
        return Math.max(0, (int) Math.ceil(seconds * 20));
    }

    // 向上取整并保留负数；超范围值按 Java 的整数转换规则处理。
    public static int ceilSigned(double seconds) {
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(seconds * 20));
    }

    // 四舍五入，最少一刻，最大值限制为 Integer.MAX_VALUE。
    public static int roundAtLeastOne(double seconds) {
        if (seconds <= 0.0D) return 1;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, Math.round(seconds * 20.0D)));
    }

    private TickDurations() {}
}

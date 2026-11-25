package com.thelongtravail.runtime.scheduling;

import net.minecraft.nbt.CompoundTag;

/** 使用调用方传入的游戏时间判断间隔和截止刻。 */
public final class TickDeadline {
    /** Long.MIN_VALUE 表示尚未触发过。 */
    public static boolean hasElapsed(long lastTick, long now, int intervalTicks) {
        return lastTick == Long.MIN_VALUE || now - lastTick >= intervalTicks;
    }

    public static boolean isActive(CompoundTag data, String key, long now) {
        return now < data.getLong(key);
    }

    public static long remaining(CompoundTag data, String key, long now) {
        return Math.max(0L, data.getLong(key) - now);
    }

    public static void start(CompoundTag data, String key, long now, int duration) {
        data.putLong(key, now + duration);
    }

    private TickDeadline() {}
}

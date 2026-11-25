package com.thelongtravail.runtime.scheduling;

import net.minecraft.nbt.CompoundTag;

/** 按键保存剩余刻数，由调用方决定何时推进。 */
public final class RemainingTicks {
    public static int get(CompoundTag data, String key) {
        return Math.max(0, data.getInt(key));
    }

    public static void start(CompoundTag data, String key, int duration) {
        data.putInt(key, duration);
    }

    public static void extend(CompoundTag data, String key, int duration) {
        start(data, key, Math.max(duration, get(data, key)));
    }

    /** 仅在剩余刻数从 1 减至 0 时返回 true。 */
    public static boolean tick(CompoundTag data, String key) {
        int remaining = get(data, key);
        if (remaining == 0) return false;
        data.putInt(key, remaining - 1);
        return remaining == 1;
    }

    private RemainingTicks() {}
}

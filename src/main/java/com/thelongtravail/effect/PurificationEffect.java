package com.thelongtravail.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class PurificationEffect extends MobEffect {
    public static final int DEFAULT_MAX_LEVEL = 5;

    public PurificationEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    // amplifier 从零开始；高等级直接返回封顶倍率，避免换算等级时溢出。
    public static double remainingMultiplier(int amplifier) {
        if (amplifier >= 9) return 0.01;
        return (9 - Math.max(0, amplifier)) / 10.0;
    }
}

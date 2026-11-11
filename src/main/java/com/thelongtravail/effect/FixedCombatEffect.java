package com.thelongtravail.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

// 效果强度由共用规则提供，药水等级不参与倍率计算。
public final class FixedCombatEffect extends MobEffect {
    public FixedCombatEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}

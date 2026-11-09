package com.thelongtravail.combat;

import com.thelongtravail.farreach.DonQuixoteState;
import com.thelongtravail.flourishing.EverlastingFlower;
import com.thelongtravail.flourishing.FloralCombat;
import com.thelongtravail.flourishing.Mistletoe;
import com.thelongtravail.integration.MaidIntegration;
import com.thelongtravail.underworld.DemonKingCombat;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

// 各模块提供剩余倍率；仅在这里将百分比减伤应用到伤害金额。
public final class DamageReduction {
    static float apply(
            LivingEntity target,
            DamageSource originalSource,
            DamageSource effectiveSource,
            float amount,
            boolean ordinary,
            boolean floral,
            boolean poison) {
        double multiplier = 1;
        if (ordinary) {
            multiplier *= factor(DemonKingCombat.remainingMultiplier(target, effectiveSource));
            multiplier *= factor(DonQuixoteState.remainingMultiplier(target, originalSource));
            if (!originalSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                multiplier *= remaining(Mistletoe.reduction(target));
                multiplier *= remaining(EverlastingFlower.reduction(target));
            }
        }
        if (floral) {
            multiplier *= factor(FloralCombat.remainingMultiplier(target, effectiveSource, poison));
            multiplier *=
                    factor(MaidIntegration.remainingMultiplier(target, effectiveSource, poison));
        }
        // 分类只看转换后的实际伤害；两种肃正互斥，不重复减免。
        multiplier *= factor(Purification.remainingMultiplier(target, effectiveSource));
        return scale(amount, multiplier);
    }

    public static double remaining(double reduction) {
        return Double.isFinite(reduction) ? 1 - Math.max(0, Math.min(1, reduction)) : 1;
    }

    private static double factor(double multiplier) {
        return Double.isFinite(multiplier) ? Math.max(0, Math.min(1, multiplier)) : 1;
    }

    static float scale(float amount, double multiplier) {
        if (!(amount > 0) || !Float.isFinite(amount)) return 0;
        return (float) (amount * factor(multiplier));
    }

    private DamageReduction() {}
}

package com.thelongtravail.runtime.effects;

import com.thelongtravail.combat.EffectCombatRules;

import net.minecraft.world.entity.LivingEntity;

// 独立生命损失直接写入生命值，不经过 hurt、吸收生命、伤害倍率和攻击收益。
public final class HealthLoss {
    public static void nonlethalFraction(LivingEntity entity, double fraction) {
        if (entity.level().isClientSide || !entity.isAlive() || EffectCombatRules.peaceful(entity))
            return;
        float health = entity.getHealth(), maximum = entity.getMaxHealth();
        if (!Float.isFinite(health)
                || !Float.isFinite(maximum)
                || health <= 1
                || maximum <= 0
                || !Double.isFinite(fraction)
                || fraction <= 0) return;
        float loss = (float) Math.min(maximum * fraction, health - 1);
        if (loss > 0) {
            float next = Math.max(1, health - loss);
            if (!com.thelongtravail.compat.maidspell.MaidSpellHealth.applyNonlethalEffectLoss(
                    entity, next)) entity.setHealth(next);
        }
    }

    public static boolean blocks(LivingEntity entity, float requested) {
        if (entity.level().isClientSide
                || com.thelongtravail.combat.CombatContext.allowsKillHealthChange(entity)
                || !EffectCombatRules.peaceful(entity)) return false;
        float current = entity.getHealth(), maximum = entity.getMaxHealth();
        if (!(current > 0)) return false;
        // 最大生命值降低时，允许将当前生命恰好夹取到新上限。
        if (Float.isFinite(maximum) && maximum > 0 && current > maximum && requested == maximum)
            return false;
        return Float.isNaN(requested) || requested < current;
    }

    private HealthLoss() {}
}

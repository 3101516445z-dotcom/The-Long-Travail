package com.thelongtravail.combat;

import com.thelongtravail.effect.PurificationEffect;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class Purification {
    public static double remainingMultiplier(LivingEntity target, DamageSource source) {
        // 数据包若同时加入两类，以魔法类为准，同一段伤害不重复减免。
        MobEffectInstance effect;
        if (source.is(DamageClassTags.MAGIC)) {
            effect = target.getEffect(ModRegistry.MAGIC_PURIFICATION.get());
        } else if (source.is(DamageClassTags.PHYSICAL)) {
            effect = target.getEffect(ModRegistry.PHYSICAL_PURIFICATION.get());
        } else {
            return 1;
        }
        return effect == null ? 1 : PurificationEffect.remainingMultiplier(effect.getAmplifier());
    }

    private Purification() {}
}

package com.thelongtravail.helper;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.ModeEffectRules;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.LongTravailData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.ItemStack;

final class CombatRules {
    static boolean abyssEnvironmentDamage(DamageSource source) {
        return source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.CRAMMING)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.FREEZE);
    }

    static float internalAmount(
            LivingEntity target, ServerPlayer attacker, boolean roseDamage, DamageSource source, float amount) {
        if (!(amount > 0)) return 0;
        if (roseDamage) return amount;
        amount = multiply(amount, 1 + com.thelongtravail.valley.Phosphophyllite.damageBonus(target));
        if (target instanceof ServerPlayer player) {
            ItemStack diary = TravailCurios.stack(player);
            if (ModeEffectRules.diaryApplies(player) && !diary.isEmpty()) {
                if (!LongTravailData.hasWitness(diary, TravailAspect.FLOURISHING)
                        && player.getHealth()
                                > player.getMaxHealth() * TravailConfig.FLOURISHING_HEALTH_THRESHOLD.get()) {
                    amount = multiply(amount, TravailConfig.FLOURISHING_DAMAGE_MULTIPLIER.get());
                }
                if (!LongTravailData.hasWitness(diary, TravailAspect.ABYSS) && abyssEnvironmentDamage(source)) {
                    amount = multiply(amount, TravailConfig.ABYSS_ENVIRONMENT_MULTIPLIER.get());
                }
            }
        }
        if (attacker != null) {
            ItemStack diary = TravailCurios.stack(attacker);
            if (ModeEffectRules.diaryApplies(attacker)
                    && !diary.isEmpty()
                    && !LongTravailData.hasWitness(diary, TravailAspect.UNDERWORLD)) {
                amount = multiply(amount, 1.0 - TravailConfig.UNDERWORLD_DAMAGE_REDUCTION.get());
            }
        }
        return amount;
    }

    static float multiply(float amount, double multiplier) {
        if (multiplier <= 0) return 0;
        double result = amount * multiplier;
        return (float) Math.max(0, Math.min(Float.MAX_VALUE, result));
    }

    private CombatRules() {}
}

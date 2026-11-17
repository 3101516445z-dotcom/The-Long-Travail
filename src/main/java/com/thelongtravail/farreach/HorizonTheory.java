package com.thelongtravail.farreach;

import com.thelongtravail.api.CarryingApi;
import com.thelongtravail.api.EntityHeightApi;
import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.combat.EffectCombatRules;
import com.thelongtravail.config.ThemeItemConfig;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

public final class HorizonTheory {
    public static float capDamage(LivingEntity target, DamageSource source, float amount) {
        if (!target.level().isClientSide
                && ordinary(source)
                && carrying(target)
                && phase(target, false)) {
            return Math.min(
                    amount,
                    (float)
                            (target.getMaxHealth()
                                    * ThemeItemConfig.get("horizon_theory.damageCap")));
        }
        return amount;
    }

    private static final Map<LivingEntity, Long> UNTIL = new WeakHashMap<>();

    private static boolean carrying(LivingEntity entity) {
        return entity instanceof ServerPlayer p
                ? CarryingApi.isCarrying(p, ModRegistry.HORIZON_THEORY)
                : com.thelongtravail.integration.MaidIntegration.equipped(
                        entity, ModRegistry.HORIZON_THEORY);
    }

    private static boolean phase(LivingEntity entity, boolean witness) {
        var state =
                entity instanceof ServerPlayer p
                        ? TravailStateApi.state(p, com.thelongtravail.TravailAspect.FAR_REACH)
                        : com.thelongtravail.integration.MaidIntegration.phase(
                                entity, com.thelongtravail.TravailAspect.FAR_REACH);
        return state
                == (witness
                        ? TravailStateApi.AspectState.WITNESS
                        : TravailStateApi.AspectState.MALICE);
    }

    public static void clear(LivingEntity entity) {
        UNTIL.remove(entity);
    }

    private static boolean ordinary(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    public static boolean blocked(LivingEntity target, DamageSource source) {
        if (target.level().isClientSide || !ordinary(source)) return false;
        LivingEntity actor = EffectCombatRules.actor(source);
        if (actor != null
                && actor != target
                && EntityHeightApi.heightDifference(actor, target)
                        > ThemeItemConfig.get("horizon_theory.maliceHeightDifference")
                && carrying(target)
                && phase(target, false)) return true;
        return target.getServer().overworld().getGameTime()
                        < UNTIL.getOrDefault(target, Long.MIN_VALUE)
                && carrying(target)
                && phase(target, true);
    }

    public static double bonus(LivingEntity target, DamageSource source) {
        if (target.level().isClientSide) return 0;
        LivingEntity actor = EffectCombatRules.actor(source);
        return actor != null
                        && actor != target
                        && EntityHeightApi.heightDifference(actor, target)
                                > ThemeItemConfig.get("horizon_theory.witnessHeightDifference")
                        && carrying(actor)
                        && phase(actor, true)
                ? ThemeItemConfig.get("horizon_theory.heightDamageBonus")
                : 0;
    }

    public static void confirmed(LivingEntity target, DamageSource source) {
        if (!target.level().isClientSide
                && ordinary(source)
                && carrying(target)
                && phase(target, true))
            UNTIL.put(
                    target,
                    target.getServer().overworld().getGameTime()
                            + ThemeItemConfig.ticks("horizon_theory.invulnerabilitySeconds"));
    }

    public static float capHealth(LivingEntity target, DamageSource source, float requested) {
        if (!target.level().isClientSide
                && ordinary(source)
                && carrying(target)
                && phase(target, false))
            return Math.max(
                    requested,
                    target.getHealth()
                            - (float)
                                    (target.getMaxHealth()
                                            * ThemeItemConfig.get("horizon_theory.damageCap")));
        return requested;
    }

    private HorizonTheory() {}
}

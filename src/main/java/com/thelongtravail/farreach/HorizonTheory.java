package com.thelongtravail.farreach;

import com.thelongtravail.api.CarryingApi;
import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.config.ThemeItemConfig;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

public final class HorizonTheory {
    private static final Map<ServerPlayer, Long> UNTIL = new WeakHashMap<>();

    private static boolean carrying(ServerPlayer p) {
        return CarryingApi.isCarrying(p, ModRegistry.HORIZON_THEORY);
    }

    private static boolean ordinary(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    public static boolean blocked(LivingEntity target, DamageSource source) {
        return target instanceof ServerPlayer p
                && ordinary(source)
                && p.server.overworld().getGameTime() < UNTIL.getOrDefault(p, Long.MIN_VALUE)
                && carrying(p)
                && TravailStateApi.hasFarReachWitness(p);
    }

    public static void confirmed(LivingEntity target, DamageSource source) {
        if (target instanceof ServerPlayer p
                && ordinary(source)
                && carrying(p)
                && TravailStateApi.hasFarReachWitness(p))
            UNTIL.put(
                    p,
                    p.server.overworld().getGameTime()
                            + ThemeItemConfig.ticks("horizon_theory.invulnerabilitySeconds"));
    }

    public static float capHealth(LivingEntity target, DamageSource source, float requested) {
        if (target instanceof ServerPlayer p
                && ordinary(source)
                && carrying(p)
                && TravailStateApi.hasFarReachMalice(p))
            return Math.max(
                    requested,
                    p.getHealth()
                            - (float)
                                    (p.getMaxHealth()
                                            * ThemeItemConfig.get("horizon_theory.damageCap")));
        return requested;
    }

    private HorizonTheory() {}
}

package com.thelongtravail.combat;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class InsightRetaliation {
    public static final net.minecraft.tags.TagKey<net.minecraft.world.damagesource.DamageType> RETALIATION =
            net.minecraft.tags.TagKey.create(
                    Registries.DAMAGE_TYPE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("the_long_travail", "retaliation"));
    private static final ThreadLocal<Boolean> CHAIN = ThreadLocal.withInitial(() -> false);

    private static final class Source extends DamageSource {
        Source(LivingEntity defender) {
            super(
                    defender.level()
                            .registryAccess()
                            .registryOrThrow(Registries.DAMAGE_TYPE)
                            .getHolderOrThrow(DamageTypes.FALLING_ANVIL),
                    defender);
        }
    }

    public static boolean isInsight(DamageSource source) {
        return source instanceof Source;
    }

    public static boolean isRetaliation(DamageSource source) {
        return CHAIN.get()
                || source.is(RETALIATION)
                || isInsight(source)
                || source.is(DamageTypes.THORNS)
                || com.thelongtravail.flourishing.FloralCombat.rose(source);
    }

    public static void hit(LivingEntity defender, Entity attacker, double amount) {
        if (!(amount > 0)
                || !Double.isFinite(amount)
                || attacker == null
                || attacker == defender
                || !defender.isAlive()
                || CHAIN.get()
                || defender.isRemoved()
                || attacker.isRemoved()
                || !attacker.isAlive()
                || attacker.level() != defender.level()
                || defender.level().isClientSide) return;
        if (attacker instanceof net.minecraft.world.entity.player.Player player
                && (defender instanceof net.minecraft.server.level.ServerPlayer
                        || com.thelongtravail.compat.maid.MaidAccessories.isMaid(defender))
                && !com.thelongtravail.compat.maid.MaidAccessories.canHarm(defender, player)) return;
        CHAIN.set(true);
        try {
            attacker.hurt(new Source(defender), (float) Math.min(Float.MAX_VALUE, amount));
        } finally {
            CHAIN.remove();
        }
    }

    private InsightRetaliation() {}
}

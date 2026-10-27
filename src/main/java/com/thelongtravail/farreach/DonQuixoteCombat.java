package com.thelongtravail.farreach;

import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.compat.maid.MaidAccessories;
import com.thelongtravail.config.FarReachRelicsConfig;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.helper.CurioLookup;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;

public final class DonQuixoteCombat {
    public static final ResourceKey<DamageType> STRIKE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation("the_long_travail", "don_quixote_strike"));

    public record Snapshot(double bonus, double maxHealth, boolean strike, boolean witness) {}

    public static boolean equipped(ServerPlayer p) {
        return p.isAlive()
                && !p.isSpectator()
                && CurioLookup.hasFunctional(p, ModRegistry.DON_QUIXOTE.get());
    }

    public static boolean equipped(LivingEntity e) {
        return e instanceof ServerPlayer p ? equipped(p) : MaidAccessories.quixote(e);
    }

    public static int phase(LivingEntity e) {
        return e instanceof ServerPlayer p
                ? (TravailStateApi.hasFarReachWitness(p)
                        ? 2
                        : TravailStateApi.hasFarReachMalice(p) ? 1 : 0)
                : MaidAccessories.quixotePhase(e);
    }

    private static LivingEntity attacker(DamageSource source) {
        return source.getEntity() instanceof ServerPlayer p
                ? p
                : MaidAccessories.quixoteAttacker(source);
    }

    public static boolean secondary(DamageSource source) {
        return source.is(STRIKE);
    }

    public static double sizeBonus(double volume) {
        if (!Double.isFinite(volume) || volume <= 0) return 0;
        return Math.min(
                FarReachRelicsConfig.value("don_quixote.maxBonus"),
                Math.max(0, Math.cbrt(volume / .648) - 1)
                        * FarReachRelicsConfig.value("don_quixote.sizeFactor"));
    }

    public static Snapshot snapshot(LivingEntity target, DamageSource source) {
        LivingEntity p = attacker(source);
        if (p == null
                || p == target
                || !equipped(p)
                || secondary(source)
                || source.is(DamageTypes.THORNS)
                || com.thelongtravail.flourishing.FloralCombat.rose(source)) return null;
        double volume = (double) target.getBbWidth() * target.getBbWidth() * target.getBbHeight();
        String id =
                net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                        .getKey(target.getType())
                        .toString();
        for (String entry : FarReachRelicsConfig.SIZES.get()) {
            String[] parts = entry.split("=", 2);
            if (parts[0].equals(id)) {
                volume = Double.parseDouble(parts[1]);
                break;
            }
        }
        double hp = target.getHealth(), ph = p.getHealth(), max = target.getMaxHealth();
        boolean witness = phase(p) == 2;
        boolean strike =
                (phase(p) == 1)
                        && Double.isFinite(hp)
                        && Double.isFinite(ph)
                        && ph > 0
                        && hp >= ph * FarReachRelicsConfig.value("don_quixote.healthRatio");
        return new Snapshot(
                sizeBonus(volume), Double.isFinite(max) && max > 0 ? max : 0, strike, witness);
    }

    public static void finish(
            LivingEntity target, DamageSource source, Snapshot snapshot, double damage) {
        LivingEntity p = attacker(source);
        if (snapshot == null
                || snapshot.maxHealth <= 0
                || !(damage > 0)
                || p == null
                || !equipped(p)) return;
        if (snapshot.witness
                && damage
                        > snapshot.maxHealth
                                * FarReachRelicsConfig.value("don_quixote.witnessThreshold"))
            DonQuixoteState.trigger(p, target);
        if (snapshot.strike
                && target.isAlive()
                && !target.isRemoved()
                && target.level() == p.level())
            strike(
                    p,
                    target,
                    snapshot.maxHealth * FarReachRelicsConfig.value("don_quixote.trueFraction"));
    }

    private static void strike(LivingEntity p, LivingEntity target, double amount) {
        if (!(amount > 0) || !Double.isFinite(amount)) return;
        if (target instanceof net.minecraft.world.entity.player.Player player
                && !(p instanceof ServerPlayer sp
                        ? sp.canHarmPlayer(player)
                        : MaidAccessories.canHarm(p, player))) return;
        DamageSource source =
                new DamageSource(
                        p.level()
                                .registryAccess()
                                .registryOrThrow(Registries.DAMAGE_TYPE)
                                .getHolderOrThrow(STRIKE),
                        p);
        int invulnerable = target.invulnerableTime;
        var state = (com.thelongtravail.mixin.DamageReplayStateAccessor) target;
        float last = state.travail$lastHurt();
        try {
            target.hurt(source, (float) Math.min(Float.MAX_VALUE, amount));
        } finally {
            target.invulnerableTime = invulnerable;
            state.travail$lastHurt(last);
        }
    }

    private DonQuixoteCombat() {}
}

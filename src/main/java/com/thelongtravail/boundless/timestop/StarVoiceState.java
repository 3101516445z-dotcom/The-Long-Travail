package com.thelongtravail.boundless.timestop;

import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.boundless.BoundlessEquipment;
import com.thelongtravail.config.BoundlessItemsConfig;
import com.thelongtravail.runtime.player.PlayerCooldowns;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class StarVoiceState {
    public static final String RECEIVED_INTERVAL = "LongTravailStarReceivedInterval";
    public static final String COOLDOWN = "LongTravailStarCooldown",
            INTERVAL = "LongTravailStarInterval";

    public static void received(LivingEntity victim, DamageSource source) {
        if (victim instanceof ServerPlayer p) {
            var cause = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
            boolean self =
                    cause == p
                            || source.getEntity() == null
                                    && cause != null
                                    && p.getUUID().equals(TimeStopExemptions.owner(cause));
            if (cause != null && !self) roll(p, false);
        }
    }

    public static void confirmed(LivingEntity victim, DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer p && p != victim && !victim.isSpectator())
            roll(p, true);
    }

    private static void roll(ServerPlayer p, boolean attacking) {
        if (!p.isAlive()
                || p.isSpectator()
                || TimeStopManager.frozen(p)
                || !BoundlessEquipment.equipped(p, false)
                || TimeStopManager.locked(p.getUUID())) return;
        boolean witness = TravailStateApi.hasBoundlessWitness(p);
        if (attacking != witness) return;
        if (witness
                && (PlayerCooldowns.remainingTicks(p, COOLDOWN) > 0
                        || PlayerCooldowns.remainingTicks(p, INTERVAL) > 0)) return;
        if (!witness && PlayerCooldowns.remainingTicks(p, RECEIVED_INTERVAL) > 0) return;
        PlayerCooldowns.startTicks(
                p,
                witness ? INTERVAL : RECEIVED_INTERVAL,
                BoundlessItemsConfig.ticks(
                        witness ? "star.witnessInterval" : "star.maliceInterval"));
        if (p.getRandom().nextDouble()
                < BoundlessItemsConfig.get(witness ? "star.witnessChance" : "star.maliceChance"))
            TimeStopManager.get(p.serverLevel()).start(p, witness);
    }

    public static void tick(ServerPlayer p) {
        if (!TimeStopManager.locked(p.getUUID())) PlayerCooldowns.tick(p, COOLDOWN);
        PlayerCooldowns.tick(p, INTERVAL);
        PlayerCooldowns.tick(p, RECEIVED_INTERVAL);
    }

    public static void decrement(ServerPlayer p, String key) {
        PlayerCooldowns.tick(p, key);
    }

    private StarVoiceState() {}
}

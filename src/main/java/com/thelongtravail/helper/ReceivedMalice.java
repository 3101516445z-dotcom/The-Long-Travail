package com.thelongtravail.helper;

import com.thelongtravail.common.ReceivedHitGate;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.event.TravailEvents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class ReceivedMalice {
    private static final ReceivedHitGate GATE = new ReceivedHitGate();

    public static void forget(ServerPlayer player) {
        GATE.forget(player);
    }

    public static void clear() {
        GATE.clear();
    }

    public static boolean trigger(LivingEntity target, DamageSource source) {
        if (target instanceof ServerPlayer player) {
            if (!com.thelongtravail.config.ModeEffectRules.diaryApplies(player)
                    || TravailCurios.stack(player).isEmpty()) return false;
        } else if (com.thelongtravail.valley.Phosphophyllite.phase(target) != 2) return false;
        if (!GATE.acquire(
                target, source, TravailConfig.RECEIVED_EXCLUSIONS.get(), TravailConfig.RECEIVED_COOLDOWN.get()))
            return false;
        if (target instanceof ServerPlayer player) TravailEvents.processBeingAttacked(player);
        return true;
    }

    private ReceivedMalice() {}
}

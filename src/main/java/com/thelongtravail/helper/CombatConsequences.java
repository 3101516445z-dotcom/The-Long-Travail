package com.thelongtravail.helper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;

final class CombatConsequences {
    static void confirmed(LivingEntity target, DamageSource source, ServerPlayer attacker, boolean roseDamage) {
        com.thelongtravail.farreach.FarReachCombat.ignite(target, source);
        if (!roseDamage) com.thelongtravail.valley.SwordLanternState.confirmed(target, source);
        com.thelongtravail.boundless.StarVoiceState.confirmed(target, source);
        if (attacker != null) com.thelongtravail.valley.AzraelState.damageDealt(attacker, target);
    }

    static void received(LivingEntity target, DamageSource source) {
        com.thelongtravail.boundless.StarVoiceState.received(target, source);
        if (target instanceof ServerPlayer player) com.thelongtravail.valley.AzraelState.received(player, source);
        com.thelongtravail.valley.SwordLanternState.received(target, source);
        com.thelongtravail.compat.maid.MaidAccessories.received(target, source);
    }

    private CombatConsequences() {}
}

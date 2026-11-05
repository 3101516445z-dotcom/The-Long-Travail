package com.thelongtravail.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

final class CombatConsequences {
    static void confirmedLoss(
            LivingEntity target, DamageSource source, ServerPlayer attacker, boolean roseDamage) {
        com.thelongtravail.farreach.FarReachCombat.ignite(target, source);
        if (!roseDamage)
            com.thelongtravail.valley.lantern.SwordLanternState.confirmed(target, source);
        com.thelongtravail.boundless.timestop.StarVoiceState.confirmed(target, source);
        if (attacker != null)
            com.thelongtravail.valley.execution.AzraelState.damageDealt(attacker, target);
    }

    static void attackAttempt(LivingEntity target, DamageSource source) {
        com.thelongtravail.boundless.timestop.StarVoiceState.received(target, source);
        if (target instanceof ServerPlayer player)
            com.thelongtravail.valley.execution.AzraelState.received(player, source);
        com.thelongtravail.valley.lantern.SwordLanternState.received(target, source);
        com.thelongtravail.integration.MaidIntegration.received(target, source);
    }

    private CombatConsequences() {}
}

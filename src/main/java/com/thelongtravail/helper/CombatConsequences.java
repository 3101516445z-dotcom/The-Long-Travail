package com.thelongtravail.helper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;

// 仅在首次实际扣除生命值或伤害吸收量时调用，调用顺序会影响效果判定。
final class CombatConsequences {
    static void confirmed(LivingEntity target,DamageSource source,ServerPlayer attacker,boolean floralTransfer,boolean roseDamage){
        com.thelongtravail.farreach.FarReachCombat.ignite(target, source);
        if (!floralTransfer && !roseDamage) com.thelongtravail.valley.SwordLanternState.confirmed(target, source);
        com.thelongtravail.boundless.StarVoiceState.confirmed(target, source);
        if (target instanceof ServerPlayer player) com.thelongtravail.valley.AzraelState.received(player, source);
        if (attacker != null) com.thelongtravail.valley.AzraelState.damageDealt(attacker, target);
    }
    private CombatConsequences(){}
}

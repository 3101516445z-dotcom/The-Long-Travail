package com.thelongtravail.combat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

// 在单次结算首次读取属性前刷新；被排除的伤害不进入此阶段。
final class CombatAttributePreparation {
    static void prepare(LivingEntity target, DamageSource source) {
        com.thelongtravail.underworld.Kagotsurube.refresh(target);
        com.thelongtravail.underworld.UnderworldRelics.refresh(target);
        if (source.getEntity() instanceof LivingEntity actor)
            com.thelongtravail.boundless.LorentzFantasy.refresh(actor);
        if (source.getEntity() instanceof LivingEntity actor && actor != target)
            com.thelongtravail.underworld.UnderworldRelics.refresh(actor);
    }

    private CombatAttributePreparation() {}
}

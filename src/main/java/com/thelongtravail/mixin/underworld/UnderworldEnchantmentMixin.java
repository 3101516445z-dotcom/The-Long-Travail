package com.thelongtravail.mixin.underworld;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.underworld.AttackMobTypeContext;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EnchantmentHelper.class)
public abstract class UnderworldEnchantmentMixin {
    @WrapMethod(method = "doPostDamageEffects")
    private static void travail$attackView(
            LivingEntity attacker, Entity target, Operation<Void> original) {
        try (var ignored = AttackMobTypeContext.open(attacker, target)) {
            original.call(attacker, target);
        }
    }

    @WrapMethod(method = "doPostHurtEffects")
    private static void travail$defenseView(
            LivingEntity victim, Entity attacker, Operation<Void> original) {
        try (var ignored = AttackMobTypeContext.suspend()) {
            original.call(victim, attacker);
        }
    }
}

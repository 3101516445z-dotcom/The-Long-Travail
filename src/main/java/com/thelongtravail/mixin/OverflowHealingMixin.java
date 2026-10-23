package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.thelongtravail.common.OverflowHealing;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class OverflowHealingMixin {
    @Unique private OverflowHealing.Request travail$healing;
    @Unique private float travail$effectiveHealing;

    @WrapMethod(method = "heal")
    private void travail$healScope(float amount, Operation<Void> original) {
        var previous = travail$healing;
        float previousAmount = travail$effectiveHealing;
        travail$healing = OverflowHealing.claim((LivingEntity) (Object) this);
        travail$effectiveHealing = 0;
        try {
            original.call(amount);
        } finally {
            travail$healing = previous;
            travail$effectiveHealing = previousAmount;
        }
    }

    @WrapOperation(
            method = "heal",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraftforge/event/ForgeEventFactory;onLivingHeal(Lnet/minecraft/world/entity/LivingEntity;F)F",
                            remap = false))
    private float travail$effective(LivingEntity target, float amount, Operation<Float> original) {
        float effective = original.call(target, amount);
        travail$effectiveHealing = effective;
        return effective;
    }

    @WrapOperation(
            method = "heal",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    private void travail$overflow(LivingEntity target, float health, Operation<Void> original) {
        float missing = Math.max(0, target.getMaxHealth() - target.getHealth());
        original.call(target, health);
        if (travail$healing != null
                && target.isAlive()
                && Float.isFinite(travail$effectiveHealing)
                && target.getHealth() >= target.getMaxHealth())
            travail$healing.overflow = Math.max(0, travail$effectiveHealing - missing);
    }
}

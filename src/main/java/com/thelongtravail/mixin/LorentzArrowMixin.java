package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.thelongtravail.boundless.LorentzFantasy;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractArrow.class)
public abstract class LorentzArrowMixin {
    @WrapOperation(
            method = "onHitEntity",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean travail$impact(Entity target, DamageSource source, float amount, Operation<Boolean> original) {
        return LorentzFantasy.impact(
                (AbstractArrow) (Object) this, target, source, amount, () -> original.call(target, source, amount));
    }
}

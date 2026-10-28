package com.thelongtravail.mixin;

import com.thelongtravail.farreach.SunChaser;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class SunFireMixin {
    @Inject(method = "setRemainingFireTicks", at = @At("HEAD"), cancellable = true)
    private void travail$preventFire(int ticks, CallbackInfo ci) {
        if (ticks > 0
                && (Object) this instanceof LivingEntity entity
                && !entity.level().isClientSide
                && SunChaser.carrying(entity)) ci.cancel();
    }
}

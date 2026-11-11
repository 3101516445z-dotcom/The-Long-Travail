package com.thelongtravail.mixin.movement;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.runtime.effects.RevivalMovement;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class RevivalImpulseMixin implements RevivalMovement.Impulses {
    @Unique private Vec3 travail$external = Vec3.ZERO;
    @Unique private Vec3 travail$flightStart = Vec3.ZERO;

    @Override
    public void travail$externalImpulse(Vec3 change) {
        if (((LivingEntity) (Object) this).isFallFlying())
            travail$external = travail$external.add(change);
    }

    @Override
    public Vec3 travail$glidingMovement(Vec3 movement) {
        float factor = RevivalMovement.factor((LivingEntity) (Object) this);
        Vec3 external =
                RevivalMovement.glideImpulse(
                        (LivingEntity) (Object) this, travail$flightStart, travail$external);
        travail$external = external;
        return factor == 1
                ? movement
                : new Vec3(
                        (movement.x - external.x) * factor + external.x,
                        movement.y,
                        (movement.z - external.z) * factor + external.z);
    }

    @WrapMethod(method = "knockback")
    private void travail$knockback(double strength, double x, double z, Operation<Void> original) {
        LivingEntity self = (LivingEntity) (Object) this;
        Vec3 before = self.getDeltaMovement();
        original.call(strength, x, z);
        travail$externalImpulse(self.getDeltaMovement().subtract(before));
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void travail$clearGroundImpulse(Vec3 input, CallbackInfo ci) {
        if (!((LivingEntity) (Object) this).isFallFlying()) travail$external = Vec3.ZERO;
    }

    @Inject(
            method = "travel",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;checkSlowFallDistance()V"),
            require = 1,
            allow = 1)
    private void travail$captureGlide(Vec3 input, CallbackInfo ci) {
        travail$flightStart = ((LivingEntity) (Object) this).getDeltaMovement();
    }

    @Inject(method = "travel", at = @At("TAIL"))
    private void travail$clearCollisionImpulse(Vec3 input, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.horizontalCollision || self.onGround()) travail$external = Vec3.ZERO;
    }
}

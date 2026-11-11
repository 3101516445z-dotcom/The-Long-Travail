package com.thelongtravail.mixin.movement;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thelongtravail.runtime.effects.RevivalMovement;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Explosion.class)
public abstract class RevivalExplosionImpulseMixin {
    @WrapOperation(
            method = "explode",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"),
            require = 1,
            allow = 1)
    private void travail$explosionImpulse(Entity entity, Vec3 velocity, Operation<Void> original) {
        Vec3 before = entity.getDeltaMovement();
        original.call(entity, velocity);
        if (entity instanceof RevivalMovement.Impulses impulses)
            impulses.travail$externalImpulse(entity.getDeltaMovement().subtract(before));
    }
}

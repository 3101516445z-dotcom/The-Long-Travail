package com.thelongtravail.mixin.movement;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.runtime.effects.RevivalMovement;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class RevivalPropulsionMixin {
    @ModifyVariable(method = "moveRelative", at = @At("HEAD"), argsOnly = true)
    private float travail$propulsion(float speed) {
        return speed * RevivalMovement.factor((Entity) (Object) this);
    }

    @WrapMethod(method = "push(DDD)V")
    private void travail$push(double x, double y, double z, Operation<Void> original) {
        Entity self = (Entity) (Object) this;
        Vec3 before = self.getDeltaMovement();
        original.call(x, y, z);
        if (self instanceof RevivalMovement.Impulses impulses)
            impulses.travail$externalImpulse(self.getDeltaMovement().subtract(before));
    }
}

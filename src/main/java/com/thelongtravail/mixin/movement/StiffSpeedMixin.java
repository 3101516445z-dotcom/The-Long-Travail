package com.thelongtravail.mixin.movement;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thelongtravail.runtime.attributes.GroundSpeed;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class StiffSpeedMixin implements GroundSpeed.PropulsionRead {
    @Unique private boolean travail$propulsionRead;

    @Override
    public boolean travail$deferLimit() {
        return travail$propulsionRead;
    }

    @WrapOperation(
            method = {"travel", "getFrictionInfluencedSpeed", "getFlyingSpeed", "getRiddenSpeed"},
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/LivingEntity;getSpeed()F"),
            require = 4,
            allow = 4)
    private float travail$beforePropulsion(LivingEntity entity, Operation<Float> original) {
        boolean previous = travail$propulsionRead;
        travail$propulsionRead = true;
        try {
            return original.call(entity);
        } finally {
            travail$propulsionRead = previous;
        }
    }

    @Inject(method = "getSpeed", at = @At("RETURN"), cancellable = true)
    private void travail$stiffSpeed(CallbackInfoReturnable<Float> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof net.minecraft.world.entity.player.Player))
            cir.setReturnValue(GroundSpeed.apply(entity, cir.getReturnValueF()));
    }
}

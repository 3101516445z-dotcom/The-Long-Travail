package com.thelongtravail.mixin.flourishing;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.flourishing.Mistletoe;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.effect.MobEffectInstance;

@Mixin(LivingEntity.class)
public abstract class MistletoeExpiryMixin {
    @Inject(method = "onEffectRemoved", at = @At("TAIL"))
    private void travail$removed(
            MobEffectInstance effect, CallbackInfo ci) {
        Mistletoe.removed((LivingEntity) (Object) this, effect);
    }

    @WrapMethod(method = "tickEffects")
    private void travail$expiry(Operation<Void> original) {
        LivingEntity entity = (LivingEntity) (Object) this;
        try {
            original.call();
        } catch (RuntimeException | Error failure) {
            Mistletoe.abort(entity);
            throw failure;
        }
        Mistletoe.afterEffects(entity);
    }
}

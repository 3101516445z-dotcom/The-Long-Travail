package com.thelongtravail.mixin.effects;

import com.thelongtravail.runtime.effects.InsertedEffectSync;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class InsertedEffectSyncMixin {
    @Inject(method = "onEffectUpdated", at = @At("TAIL"))
    private void travail$updated(
            MobEffectInstance effect, boolean reapply, Entity source, CallbackInfo ci) {
        InsertedEffectSync.updated((LivingEntity) (Object) this, effect);
    }

    @Inject(method = "onEffectRemoved", at = @At("TAIL"))
    private void travail$removed(MobEffectInstance effect, CallbackInfo ci) {
        InsertedEffectSync.removed((LivingEntity) (Object) this, effect);
    }
}

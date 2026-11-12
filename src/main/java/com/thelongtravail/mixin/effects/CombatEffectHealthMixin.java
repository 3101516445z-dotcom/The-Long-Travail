package com.thelongtravail.mixin.effects;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.combat.EffectCombatRules;
import com.thelongtravail.runtime.effects.HealthLoss;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class CombatEffectHealthMixin {
    @Unique private boolean travail$effectsReady;
    @Unique private int travail$readingHealth;
    @Unique private int travail$maraTicks;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void travail$ready(CallbackInfo ci) {
        travail$effectsReady = true;
    }

    @WrapMethod(method = "readAdditionalSaveData")
    private void travail$read(CompoundTag tag, Operation<Void> original) {
        // 存档读取使用保存的生命值，暂不应用运行期间的生命下降保护。
        travail$readingHealth++;
        try {
            original.call(tag);
            travail$maraTicks = Math.floorMod(tag.getInt("TravailMaraTicks"), 20);
        } finally {
            travail$readingHealth--;
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void travail$save(CompoundTag tag, CallbackInfo ci) {
        if (travail$maraTicks > 0) tag.putInt("TravailMaraTicks", travail$maraTicks);
    }

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void travail$health(float health, CallbackInfo ci) {
        if (travail$effectsReady
                && travail$readingHealth == 0
                && HealthLoss.blocks((LivingEntity) (Object) this, health)) ci.cancel();
    }

    @Inject(method = "tickEffects", at = @At("HEAD"))
    private void travail$periodicLoss(CallbackInfo ci) {
        // 计时跟随实际效果 tick，刷新持续时间不重置周期。
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) return;
        if (!EffectCombatRules.mara(self)) {
            travail$maraTicks = 0;
            return;
        }
        if (++travail$maraTicks >= 20) {
            travail$maraTicks = 0;
            HealthLoss.nonlethalFraction(self, .05);
        }
    }

    @Inject(method = "onEffectRemoved", at = @At("HEAD"))
    private void travail$resetLossClock(
            net.minecraft.world.effect.MobEffectInstance effect, CallbackInfo ci) {
        if (effect.getEffect()
                == com.thelongtravail.registry.ModRegistry.MARA_TRANSFORMATION.get()) {
            travail$maraTicks = 0;
        }
    }
}

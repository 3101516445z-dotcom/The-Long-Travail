package com.thelongtravail.mixin;

import com.thelongtravail.farreach.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntity.class)
public abstract class BabelLivingMixin {
    @org.spongepowered.asm.mixin.Shadow protected Brain<?> brain;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void travail$created(CallbackInfo ci) {
        if (brain != null) ((BabelBrainOwner) brain).travail$babelOwner((LivingEntity) (Object) this);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void travail$loaded(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        if (brain != null) ((BabelBrainOwner) brain).travail$babelOwner((LivingEntity) (Object) this);
    }

    @Inject(method = "setLastHurtByMob", at = @At("HEAD"), cancellable = true)
    private void travail$hurtBy(LivingEntity attacker, CallbackInfo ci) {
        if (BabelProtection.protects((LivingEntity) (Object) this, attacker)) ci.cancel();
    }

    @Inject(method = "setLastHurtMob", at = @At("HEAD"), cancellable = true)
    private void travail$hurtOther(Entity target, CallbackInfo ci) {
        if (BabelProtection.protects((LivingEntity) (Object) this, target)) ci.cancel();
    }

    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("RETURN"), cancellable = true)
    private void travail$canAttack(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (BabelProtection.protects((LivingEntity) (Object) this, target)) cir.setReturnValue(false);
    }

    @Inject(method = "getBrain", at = @At("RETURN"))
    private void travail$brain(CallbackInfoReturnable<Brain<?>> cir) {
        Brain<?> currentBrain = cir.getReturnValue();
        // 部分 AI 优化会在 LivingEntity 初始化 brain 之前调用 getBrain。
        if (currentBrain != null) ((BabelBrainOwner) currentBrain).travail$babelOwner((LivingEntity) (Object) this);
    }
}

package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.helper.CombatContext;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingCombatMixin {
    @WrapMethod(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z")
    private boolean travail$extendEffect(net.minecraft.world.effect.MobEffectInstance effect, net.minecraft.world.entity.Entity source, Operation<Boolean> original) {
        return original.call(com.thelongtravail.flourishing.FloralEvents.extendedEffect((LivingEntity)(Object)this,effect,source),source);
    }

    @WrapMethod(method = "hurt")
    private boolean travail$damageScope(DamageSource source, float amount, Operation<Boolean> original) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (target.level().isClientSide || target instanceof net.minecraft.world.entity.player.Player) return original.call(source, amount);
        CombatContext context = CombatContext.open(target, source);
        boolean result;
        try (context) {
            result = original.call(source, amount);
        }
        context.finish();
        return result;
    }

    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getAbsorptionAmount()F", ordinal = 0))
    private float travail$roseAbsorption(LivingEntity entity, Operation<Float> original, DamageSource source, float amount) {
        return com.thelongtravail.flourishing.FloralCombat.rose(source) ? 0F : original.call(entity);
    }

    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setAbsorptionAmount(F)V", ordinal = 1))
    private void travail$preserveRoseAbsorption(LivingEntity entity, float absorption, Operation<Void> original, DamageSource source, float amount) {
        float before = entity.getAbsorptionAmount();
        if (!com.thelongtravail.flourishing.FloralCombat.rose(source))
            original.call(entity, com.thelongtravail.valley.AzraelExecution.matches(entity, source) && !Float.isFinite(absorption) ? 0F : absorption);
        CombatContext.recordAbsorptionLoss(entity, before, entity.getAbsorptionAmount());
    }

    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setAbsorptionAmount(F)V", ordinal = 0))
    private void travail$finiteExecutionAbsorption(LivingEntity entity, float absorption, Operation<Void> original, DamageSource source, float amount) {
        float before = entity.getAbsorptionAmount();
        original.call(entity, com.thelongtravail.valley.AzraelExecution.matches(entity, source) && !Float.isFinite(absorption) ? 0F : absorption);
        CombatContext.recordAbsorptionLoss(entity, before, entity.getAbsorptionAmount());
    }

    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    private void travail$recordCommittedDamage(LivingEntity target, float health, Operation<Void> original) {
        float before = target.getHealth();
        original.call(target, health);
        CombatContext.recordHealthLoss(target, before, target.getHealth());
    }
}

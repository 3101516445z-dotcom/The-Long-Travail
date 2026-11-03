package com.thelongtravail.mixin.combat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thelongtravail.combat.CombatContext;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingCombatMixin {
    @WrapMethod(
            method =
                    "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z")
    private boolean travail$extendEffect(
            net.minecraft.world.effect.MobEffectInstance effect,
            net.minecraft.world.entity.Entity source,
            Operation<Boolean> original) {
        var target = (LivingEntity) (Object) this;
        try {
            if (com.thelongtravail.abyss.MidnightLafite.levels(target, effect.getEffect()) > 0)
                effect = com.thelongtravail.abyss.MidnightLafite.copyEffect(effect);
            effect =
                    com.thelongtravail.flourishing.FloralPassives.shorten(
                            target,
                            com.thelongtravail.abyss.MidnightLafite.duration(
                                    target,
                                    com.thelongtravail.integration.MaidIntegration.extend(
                                            target,
                                            com.thelongtravail.flourishing.FloralEvents
                                                    .extendedEffect(target, effect, source),
                                            source)));
        } catch (com.thelongtravail.runtime.effects.EffectCopies.InvalidChain failure) {
            com.thelongtravail.common.RuntimeFailures.report(
                    "hidden effect chain "
                            + net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(
                                    effect.getEffect()),
                    failure);
            return false;
        }
        return original.call(effect, source);
    }

    @WrapMethod(method = "hurt")
    private boolean travail$damageScope(
            DamageSource source, float amount, Operation<Boolean> original) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (target.level().isClientSide
                || target instanceof net.minecraft.world.entity.player.Player)
            return original.call(source, amount);
        return com.thelongtravail.combat.DamageScope.hurt(
                target, source, amount, (effective, value) -> original.call(effective, value));
    }

    @WrapOperation(
            method = "actuallyHurt",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;getAbsorptionAmount()F",
                            ordinal = 0))
    private float travail$directStrikeAbsorption(
            LivingEntity entity, Operation<Float> original, DamageSource source, float amount) {
        return source.is(com.thelongtravail.combat.DirectStrikeDamage.TYPE)
                ? 0F
                : original.call(entity);
    }

    @WrapOperation(
            method = "actuallyHurt",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;setAbsorptionAmount(F)V",
                            ordinal = 1))
    private void travail$preserveDirectStrikeAbsorption(
            LivingEntity entity,
            float absorption,
            Operation<Void> original,
            DamageSource source,
            float amount) {
        float before = entity.getAbsorptionAmount();
        if (!source.is(com.thelongtravail.combat.DirectStrikeDamage.TYPE))
            original.call(
                    entity,
                    com.thelongtravail.valley.execution.AzraelExecution.matches(entity, source)
                                    && !Float.isFinite(absorption)
                            ? 0F
                            : absorption);
        CombatContext.recordAbsorptionLoss(entity, before, entity.getAbsorptionAmount());
    }

    @WrapOperation(
            method = "actuallyHurt",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;setAbsorptionAmount(F)V",
                            ordinal = 0))
    private void travail$finiteExecutionAbsorption(
            LivingEntity entity,
            float absorption,
            Operation<Void> original,
            DamageSource source,
            float amount) {
        float before = entity.getAbsorptionAmount();
        if (!source.is(com.thelongtravail.combat.DirectStrikeDamage.TYPE))
            original.call(
                    entity,
                    com.thelongtravail.valley.execution.AzraelExecution.matches(entity, source)
                                    && !Float.isFinite(absorption)
                            ? 0F
                            : absorption);
        CombatContext.recordAbsorptionLoss(entity, before, entity.getAbsorptionAmount());
    }

    @WrapOperation(
            method = "actuallyHurt",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    private void travail$recordCommittedDamage(
            LivingEntity target, float health, Operation<Void> original) {
        float before = target.getHealth();
        original.call(target, health);
        CombatContext.recordHealthLoss(target, before, target.getHealth());
    }
}

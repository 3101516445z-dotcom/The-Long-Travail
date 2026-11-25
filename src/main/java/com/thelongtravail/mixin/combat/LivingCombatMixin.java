package com.thelongtravail.mixin.combat;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thelongtravail.combat.CombatContext;
import com.thelongtravail.runtime.effects.EffectInsertion;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

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
            effect = EffectInsertion.prepare(target, effect, source);
        } catch (com.thelongtravail.runtime.effects.EffectCopies.InvalidChain failure) {
            com.thelongtravail.common.RuntimeFailures.report(
                    "hidden effect chain "
                            + net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(
                                    effect.getEffect()),
                    failure);
            return false;
        }
        boolean listen = com.thelongtravail.abyss.SeaBottle.listens(target, effect);
        var before = listen ? target.getEffect(effect.getEffect()) : null;
        int oldLevel = before == null ? -1 : before.getAmplifier();
        var incoming =
                listen
                        ? new net.minecraft.world.effect.MobEffectInstance(
                                effect.getEffect(),
                                effect.getDuration(),
                                effect.getAmplifier(),
                                effect.isAmbient(),
                                effect.isVisible(),
                                effect.showIcon())
                        : null;
        boolean accepted = original.call(effect, source);
        if (incoming != null)
            com.thelongtravail.abyss.SeaBottle.reapply(
                    target, incoming, source, oldLevel, accepted);
        return accepted;
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

    @WrapWithCondition(
            method = "hurt",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/Level;broadcastDamageEvent(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private boolean travail$broadcastHit(Level level, Entity target, DamageSource source) {
        return !CombatContext.blockedByOverlord((LivingEntity) target, source);
    }

    @WrapWithCondition(
            method = "hurt",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;playHurtSound(Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private boolean travail$playHitSound(LivingEntity target, DamageSource source) {
        return !CombatContext.blockedByOverlord(target, source);
    }

    @WrapWithCondition(
            method = "hurt",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;indicateDamage(DD)V"))
    private boolean travail$showHitDirection(
            LivingEntity target, double x, double z, DamageSource source, float amount) {
        return !CombatContext.blockedByOverlord(target, source);
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

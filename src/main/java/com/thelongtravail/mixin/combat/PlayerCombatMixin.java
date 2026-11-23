package com.thelongtravail.mixin.combat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thelongtravail.combat.CombatContext;
import com.thelongtravail.farreach.WitnessAbsorption;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerCombatMixin {
    @WrapOperation(
            method = "actuallyHurt",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/player/Player;setAbsorptionAmount(F)V"))
    private void travail$finiteExecutionAbsorption(
            Player player,
            float absorption,
            Operation<Void> original,
            DamageSource source,
            float amount) {
        float before = player.getAbsorptionAmount();
        float owned = player instanceof ServerPlayer sp ? WitnessAbsorption.balance(sp) : 0;
        if (!source.is(com.thelongtravail.combat.DirectStrikeDamage.TYPE))
            original.call(
                    player,
                    com.thelongtravail.valley.execution.AzraelExecution.matches(player, source)
                                    && !Float.isFinite(absorption)
                            ? 0F
                            : absorption);
        if (player instanceof ServerPlayer sp)
            WitnessAbsorption.spent(sp, owned, before - player.getAbsorptionAmount());
        CombatContext.recordAbsorptionLoss(player, before, player.getAbsorptionAmount());
    }

    @org.spongepowered.asm.mixin.injection.Inject(
            method = "setAbsorptionAmount",
            at = @At("RETURN"))
    private void travail$clampWitnessBalance(
            float amount, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player)
            WitnessAbsorption.clamp(player, player.getAbsorptionAmount());
    }

    @WrapMethod(method = "hurt")
    private boolean travail$damageScope(
            DamageSource source, float amount, Operation<Boolean> original) {
        Player target = (Player) (Object) this;
        if (target.level().isClientSide) return original.call(source, amount);
        return com.thelongtravail.combat.DamageScope.hurt(
                target, source, amount, (effective, value) -> original.call(effective, value));
    }

    @WrapOperation(
            method = "actuallyHurt",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/player/Player;setHealth(F)V"))
    private void travail$recordCommittedDamage(
            Player target,
            float health,
            Operation<Void> original,
            DamageSource source,
            float amount) {
        float before = target.getHealth();
        health = com.thelongtravail.farreach.HorizonTheory.capHealth(target, source, health);
        if (target instanceof net.minecraft.server.level.ServerPlayer sp)
            com.thelongtravail.underworld.BookRevival.damage(sp, before - health);
        original.call(target, health);
        CombatContext.recordHealthLoss(target, before, target.getHealth());
    }
}

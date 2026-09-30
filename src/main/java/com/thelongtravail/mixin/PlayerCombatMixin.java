package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.helper.CombatContext;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerCombatMixin {
    @WrapMethod(method = "hurt")
    private boolean travail$damageScope(DamageSource source, float amount, Operation<Boolean> original) {
        Player target = (Player) (Object) this;
        if (target.level().isClientSide) return original.call(source, amount);
        CombatContext context = CombatContext.open(target, source);
        boolean result;
        try (context) {
            result = original.call(source, amount);
        }
        context.finish();
        return result;
    }

    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;setHealth(F)V"))
    private void travail$recordCommittedDamage(Player target, float health, Operation<Void> original) {
        float before = target.getHealth();
        original.call(target, health);
        CombatContext.recordHealthLoss(target, before, target.getHealth());
    }
}

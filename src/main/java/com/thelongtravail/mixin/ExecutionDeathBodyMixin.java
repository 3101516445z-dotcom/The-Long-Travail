package com.thelongtravail.mixin;

import com.thelongtravail.valley.FinalExecution;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;

@Mixin({LivingEntity.class, ServerPlayer.class})
public abstract class ExecutionDeathBodyMixin {
    @WrapMethod(method = "die")
    private void travail$once(DamageSource source, Operation<Void> original) {
        if (FinalExecution.enterDeath((LivingEntity) (Object) this, source)) original.call(source);
    }

    @WrapOperation(
            method = "die",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraftforge/common/ForgeHooks;onLivingDeath(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)Z",
                            remap = false))
    private boolean travail$finalDeath(
            LivingEntity target, DamageSource source, Operation<Boolean> original) {
        return FinalExecution.bypassDeathCheck(target, source)
                ? false
                : original.call(target, source);
    }
}

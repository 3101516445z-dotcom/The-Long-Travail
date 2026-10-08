package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.valley.AzraelExecution;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ForgeHooks.class, remap = false)
public abstract class AzraelDeathMixin {
    @WrapMethod(method = "onLivingDeath", remap = false)
    private static boolean travail$deathOutcome(LivingEntity target, DamageSource source, Operation<Boolean> original) {
        boolean cancelled = original.call(target, source);
        AzraelExecution.deathResult(target, source, cancelled);
        if (target instanceof net.minecraft.server.level.ServerPlayer p) com.thelongtravail.underworld.BookRevival.accepted(p, source, cancelled);
        return cancelled;
    }
}

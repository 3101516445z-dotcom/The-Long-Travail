package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.thelongtravail.abyss.LiquefiedAbundance;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ThrownPotion.class)
public abstract class AbundanceLingeringPotionMixin {
    @WrapOperation(
            method = "makeAreaOfEffectCloud",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/AreaEffectCloud;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)V"),
            require = 1)
    private void travail$abundanceCloud(
            AreaEffectCloud cloud,
            MobEffectInstance effect,
            Operation<Void> original,
            @Local(argsOnly = true) ItemStack stack) {
        original.call(cloud, LiquefiedAbundance.cloudEffect(stack, effect));
    }
}

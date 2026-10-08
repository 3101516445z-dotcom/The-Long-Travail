package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thelongtravail.farreach.IcarusFlight;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.world.entity.player.Player.class)
public abstract class IcarusPlayerFlightMixin {
    @WrapOperation(method = "tryToStartFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z", remap = false))
    private boolean travail$icarusFlight(ItemStack stack, LivingEntity entity, Operation<Boolean> original) {
        return IcarusFlight.equipped(entity) ? IcarusFlight.allowed(entity) : original.call(stack, entity);
    }
}

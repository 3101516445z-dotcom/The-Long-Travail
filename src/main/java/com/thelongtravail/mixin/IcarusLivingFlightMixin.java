package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thelongtravail.farreach.IcarusFlight;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.world.entity.LivingEntity.class)
public abstract class IcarusLivingFlightMixin {
    @WrapOperation(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z", remap = false))
    private boolean travail$icarusFlight(ItemStack stack, LivingEntity entity, Operation<Boolean> original) {
        return IcarusFlight.equipped(entity) ? IcarusFlight.allowed(entity) : original.call(stack, entity);
    }
    // 由伊卡洛斯提供飞行能力时，不消耗胸甲槽中鞘翅的耐久。
    @WrapOperation(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;elytraFlightTick(Lnet/minecraft/world/entity/LivingEntity;I)Z", remap = false))
    private boolean travail$icarusTick(ItemStack stack, LivingEntity entity, int ticks, Operation<Boolean> original) {
        if (IcarusFlight.equipped(entity)) {
            if (!entity.level().isClientSide && (ticks + 1) % 10 == 0)
                entity.gameEvent(net.minecraft.world.level.gameevent.GameEvent.ELYTRA_GLIDE);
            return true;
        }
        return original.call(stack, entity, ticks);
    }
}

package com.thelongtravail.mixin;
import com.thelongtravail.boundless.TimeStopManager;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(HopperBlockEntity.class)
public abstract class TimeStopHopperMixin {
    @Inject(method="addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z",at=@At("HEAD"),cancellable=true)
    private static void travail$itemPickup(Container target,net.minecraft.world.entity.item.ItemEntity item,CallbackInfoReturnable<Boolean> ci){if(TimeStopManager.frozen(item)||com.thelongtravail.boundless.FrozenContainers.frozen(target))ci.setReturnValue(false);}
    private static boolean travail$frozen(Container c){return com.thelongtravail.boundless.FrozenContainers.frozen(c);}
    @Inject(method="addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;",at=@At("HEAD"),cancellable=true)
    private static void travail$transfer(Container from,Container to,net.minecraft.world.item.ItemStack stack,net.minecraft.core.Direction face,CallbackInfoReturnable<net.minecraft.world.item.ItemStack> ci){if(travail$frozen(from)||travail$frozen(to))ci.setReturnValue(stack);}
    @Inject(method="suckInItems",at=@At("HEAD"),cancellable=true)
    private static void travail$suck(net.minecraft.world.level.Level l,net.minecraft.world.level.block.entity.Hopper h,CallbackInfoReturnable<Boolean> ci){if(TimeStopManager.frozen(l,net.minecraft.core.BlockPos.containing(h.getLevelX(),h.getLevelY()+1,h.getLevelZ())))ci.setReturnValue(false);}
}

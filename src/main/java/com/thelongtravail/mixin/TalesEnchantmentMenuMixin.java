package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thelongtravail.boundless.EnchantmentTransfer;

import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantmentMenu.class)
public abstract class TalesEnchantmentMenuMixin {
    // 仅扩展附魔候选规则，保留古今语怪本身的物品身份。
    @WrapOperation(
            method = "getEnchantmentList",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"),
            require = 1)
    private boolean travail$talesSelection(
            ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item)
                || (item == Items.BOOK && EnchantmentTransfer.isTales(stack));
    }
}

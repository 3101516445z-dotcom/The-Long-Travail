package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thelongtravail.boundless.EnchantmentTransfer;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantmentHelper.class)
public abstract class TalesEnchantmentCandidatesMixin {
    @WrapOperation(
            method = "getAvailableEnchantmentResults",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"),
            require = 1)
    private static boolean travail$talesCandidates(
            ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item)
                || (item == Items.BOOK && EnchantmentTransfer.isTales(stack));
    }
}

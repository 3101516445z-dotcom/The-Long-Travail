package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thelongtravail.boundless.EnchantmentTransfer;

import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnvilMenu.class)
public abstract class TalesAnvilMixin {
    // 1.20.1 的此调用仅豁免接收方的装备类型检查。
    // 右侧附魔书材料使用 getItem() 判断，保持原版识别。
    @WrapOperation(
            method = "createResult",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"),
            require = 1)
    private boolean travail$talesReceiver(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item)
                || (item == Items.ENCHANTED_BOOK && EnchantmentTransfer.isTales(stack));
    }
}

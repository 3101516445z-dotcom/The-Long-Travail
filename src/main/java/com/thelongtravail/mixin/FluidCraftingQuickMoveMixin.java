package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.compat.fluid.FluidCrafting;
import com.thelongtravail.boundless.TransferNotices;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({InventoryMenu.class, CraftingMenu.class})
public abstract class FluidCraftingQuickMoveMixin {
    @WrapMethod(method = "quickMoveStack")
    private ItemStack travail$copy(Player player, int slot, Operation<ItemStack> original) {
        if (slot != 0) return original.call(player, slot);
        if (TransferNotices.blocked((AbstractContainerMenu) (Object) this, player))
            return ItemStack.EMPTY;
        return FluidCrafting.take(
                (AbstractContainerMenu) (Object) this,
                player,
                () -> original.call(player, slot),
                ItemStack.EMPTY);
    }
}

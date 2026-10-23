package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.compat.fluid.FluidCrafting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractContainerMenu.class)
public abstract class FluidCraftingClickMixin {
    @WrapMethod(method = "clicked")
    private void travail$copy(
            int slot, int button, ClickType click, Player player, Operation<Void> original) {
        if (slot != 0
                || click == ClickType.QUICK_MOVE
                || click == ClickType.CLONE
                || click == ClickType.QUICK_CRAFT
                || click == ClickType.PICKUP_ALL) {
            original.call(slot, button, click, player);
            return;
        }
        FluidCrafting.take(
                (AbstractContainerMenu) (Object) this,
                player,
                () -> {
                    original.call(slot, button, click, player);
                    return null;
                },
                null);
    }
}

package com.thelongtravail.common;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

public final class ItemDelivery {
    public static void give(Player player, ItemStack input) {
        for (int left = input.getCount(); left > 0; ) {
            ItemStack part = input.copy();
            part.setCount(Math.min(left, Math.max(1, part.getMaxStackSize())));
            left -= part.getCount();
            ItemStack remainder =
                    ItemHandlerHelper.insertItemStacked(new PlayerMainInvWrapper(player.getInventory()), part, false);
            if (!remainder.isEmpty()) player.drop(remainder, false);
        }
    }

    private ItemDelivery() {}
}

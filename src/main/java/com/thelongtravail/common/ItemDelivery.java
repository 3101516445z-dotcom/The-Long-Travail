package com.thelongtravail.common;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

public final class ItemDelivery {
    public static void give(Player player, ItemStack input) {
        give(player, input, false);
    }

    // 仅供需要反馈的调用者确认交付；不能确认时不重发，第三方可能已接管物品。
    public static boolean giveChecked(Player player, ItemStack input) {
        return give(player, input, true);
    }

    private static boolean give(Player player, ItemStack input, boolean checked) {
        boolean delivered = true;
        for (int left = input.getCount(); left > 0; ) {
            ItemStack part = input.copy();
            part.setCount(Math.min(left, Math.max(1, part.getMaxStackSize())));
            left -= part.getCount();
            ItemStack remainder =
                    ItemHandlerHelper.insertItemStacked(
                            new PlayerMainInvWrapper(player.getInventory()), part, false);
            if (!remainder.isEmpty()) {
                var drop = player.drop(remainder, false);
                if (checked) {
                    var captured = player.captureDrops();
                    delivered &=
                            drop != null
                                    && (drop.level().getEntity(drop.getId()) == drop
                                            || captured != null && captured.contains(drop));
                }
            }
        }
        return delivered;
    }

    private ItemDelivery() {}
}

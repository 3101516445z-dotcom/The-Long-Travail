package com.thelongtravail.valley;

import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class LanternSources {
    private static final String DISABLED = "LanternLightDisabled";

    public static boolean enabled(ItemStack stack) {
        return stack.is(ModRegistry.SWORD_AND_LANTERN.get())
                && (!stack.hasTag() || !stack.getTag().getBoolean(DISABLED));
    }

    public static boolean toggle(ItemStack stack) {
        boolean next = !enabled(stack);
        if (next) {
            if (stack.hasTag()) stack.getTag().remove(DISABLED);
        } else stack.getOrCreateTag().putBoolean(DISABLED, true);
        return next;
    }

    public static boolean equipped(Player player) {
        if (!player.isAlive() || player.isSpectator()) return false;
        var inventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve();
        if (inventory.isEmpty()) return false;
        var belt = inventory.get().getStacksHandler("belt");
        if (belt.isEmpty()) return false;

        var stacks = belt.get().getStacks();
        for (int i = 0; i < stacks.getSlots(); i++)
            if (enabled(stacks.getStackInSlot(i))) return true;
        return false;
    }

    public static boolean held(Player player) {
        return player.isAlive()
                && !player.isSpectator()
                && (enabled(player.getMainHandItem()) || enabled(player.getOffhandItem()));
    }

    public static boolean dropped(ItemEntity item) {
        return item.isAlive() && !item.isRemoved() && enabled(item.getItem());
    }

    private LanternSources() {}
}

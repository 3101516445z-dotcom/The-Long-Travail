package com.thelongtravail.item;

import com.thelongtravail.flourishing.ImmortalBud;

import net.minecraft.world.item.ItemStack;

public final class ItemDurabilityProtection {
    public static boolean protects(ItemStack stack) {
        return stack.getItem() instanceof HighFrequencyTremorItem || ImmortalBud.marked(stack);
    }

    private ItemDurabilityProtection() {}
}

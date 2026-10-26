package com.thelongtravail.flourishing;

import com.thelongtravail.registry.ModRegistry;

import net.minecraft.world.item.ItemStack;

public final class ImmortalBud {
    public static final String TAG = "the_long_travail:immortal_bud";

    public static boolean marked(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTag() && stack.getTag().getBoolean(TAG);
    }

    public static boolean protectsDrop(ItemStack stack) {
        return marked(stack) || stack.is(ModRegistry.IMMORTAL_BUD.get());
    }

    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && stack.getMaxDamage() > 0 && !marked(stack);
    }

    public static ItemStack apply(ItemStack target) {
        if (!accepts(target)) return ItemStack.EMPTY;
        ItemStack result = target.copy();
        result.setCount(1);
        // 仅赋予时直接归零，兼容禁止普通修复或只允许损伤增加的物品。
        result.getOrCreateTag().putInt("Damage", 0);
        result.getOrCreateTag().putBoolean(TAG, true);
        return result;
    }

    private ImmortalBud() {}
}

package com.thelongtravail.flourishing;

import com.thelongtravail.registry.RegisteredItems;

import com.thelongtravail.config.FlourishingItemsConfig;
import com.thelongtravail.network.TooltipConfigSync;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.registries.ForgeRegistries;

public final class ImmortalBud {
    public static final String TAG = "the_long_travail:immortal_bud";

    public static boolean marked(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTag() && stack.getTag().getBoolean(TAG);
    }

    public static boolean protectsDrop(ItemStack stack) {
        return marked(stack) || RegisteredItems.is(stack, ModRegistry.IMMORTAL_BUD);
    }

    public static boolean accepts(ItemStack stack) {
        if (stack.isEmpty() || stack.getMaxDamage() <= 0 || marked(stack)) return false;
        var blocked = FlourishingItemsConfig.BUD_BLACKLIST.get();
        if (EffectiveSide.get().isClient())
            blocked = TooltipConfigSync.pool("bud.blacklist", blocked);
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && !blocked.contains(id.toString());
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

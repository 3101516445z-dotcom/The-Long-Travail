package com.thelongtravail.compat.farmersdelight;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.List;

// 在农夫乐事存在时复用其食品效果排版，缺席时由调用方补充。
public final class FarmersDelightTooltips {
    private FarmersDelightTooltips() {}

    private static final Method FARMERS_EFFECTS = farmersEffects();

    private static Method farmersEffects() {
        if (!ModList.get().isLoaded("farmersdelight")) return null;
        try {
            return Class.forName("vectorwing.farmersdelight.common.utility.TextUtils")
                    .getMethod("addFoodEffectTooltip", ItemStack.class, List.class, float.class);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot resolve Farmer's Delight food tooltip", e);
        }
    }

    public static boolean append(ItemStack stack, List<Component> lines) {
        if (FARMERS_EFFECTS == null) return false;
        try {
            FARMERS_EFFECTS.invoke(null, stack, lines, 1f);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot render Farmer's Delight food tooltip", e);
        }
        return true;
    }
}

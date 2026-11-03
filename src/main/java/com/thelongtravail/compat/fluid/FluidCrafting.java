package com.thelongtravail.compat.fluid;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;

import java.util.function.Supplier;

public final class FluidCrafting {
    public static boolean supported(CraftingContainer grid) {
        return com.thelongtravail.abyss.copy.FluidCrafting.supported(grid);
    }

    public static <T> T take(
            AbstractContainerMenu menu, Player player, Supplier<T> action, T rejected) {
        return com.thelongtravail.abyss.copy.FluidCrafting.take(menu, player, action, rejected);
    }

    private FluidCrafting() {}
}

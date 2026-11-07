package com.thelongtravail.flourishing;

import com.thelongtravail.registry.RegisteredItems;

import com.thelongtravail.registry.ModRegistry;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class ImmortalBudRecipe extends CustomRecipe {
    public ImmortalBudRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    private ItemStack target(CraftingContainer grid) {
        boolean bud = false;
        ItemStack target = ItemStack.EMPTY;
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack stack = grid.getItem(i);
            if (stack.isEmpty()) continue;
            if (RegisteredItems.is(stack, ModRegistry.IMMORTAL_BUD)) {
                if (bud) return ItemStack.EMPTY;
                bud = true;
            } else {
                if (!target.isEmpty() || !ImmortalBud.accepts(stack)) return ItemStack.EMPTY;
                target = stack;
            }
        }
        return bud ? target : ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingContainer grid, Level level) {
        try {
            return !target(grid).isEmpty();
        } catch (RuntimeException failure) {
            report(grid, failure);
            return false;
        }
    }

    @Override
    public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
        try {
            return ImmortalBud.apply(target(grid));
        } catch (RuntimeException failure) {
            report(grid, failure);
            return ItemStack.EMPTY;
        }
    }

    private static void report(CraftingContainer grid, RuntimeException failure) {
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            ItemStack stack = grid.getItem(slot);
            if (stack.isEmpty() || RegisteredItems.is(stack, ModRegistry.IMMORTAL_BUD)) continue;
            com.thelongtravail.common.RuntimeFailures.report(
                    "immortal bud target "
                            + net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(
                                    stack.getItem()),
                    failure);
            return;
        }
        com.thelongtravail.common.RuntimeFailures.report("immortal bud recipe", failure);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid) {
        // 输出已包含目标，不能再通过合成剩余物重复返还。
        return NonNullList.withSize(grid.getContainerSize(), ItemStack.EMPTY);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.IMMORTAL_BUD_APPLY.get();
    }
}

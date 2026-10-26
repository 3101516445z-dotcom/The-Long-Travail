package com.thelongtravail.flourishing;

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
            if (stack.is(ModRegistry.IMMORTAL_BUD.get())) {
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
        return !target(grid).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
        return ImmortalBud.apply(target(grid));
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

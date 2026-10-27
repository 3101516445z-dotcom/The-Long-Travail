package com.thelongtravail.abyss;

import com.google.gson.JsonObject;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

// 蜂蜜瓶作为第四个产物的容器，合成后不再返还玻璃瓶。
public final class LiquefiedAbundanceRecipe extends ShapelessRecipe {
    public LiquefiedAbundanceRecipe(ResourceLocation id, ShapelessRecipe base) {
        super(
                id,
                base.getGroup(),
                base.category(),
                base.getResultItem(RegistryAccess.EMPTY),
                base.getIngredients());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid) {
        NonNullList<ItemStack> remaining = super.getRemainingItems(grid);
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            if (grid.getItem(slot).is(Items.HONEY_BOTTLE)) remaining.set(slot, ItemStack.EMPTY);
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.LIQUEFIED_ABUNDANCE_RECIPE.get();
    }

    public static final class Serializer implements RecipeSerializer<LiquefiedAbundanceRecipe> {
        private final ShapelessRecipe.Serializer delegate = new ShapelessRecipe.Serializer();

        @Override
        public LiquefiedAbundanceRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new LiquefiedAbundanceRecipe(id, delegate.fromJson(id, json));
        }

        @Override
        public LiquefiedAbundanceRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new LiquefiedAbundanceRecipe(id, delegate.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, LiquefiedAbundanceRecipe recipe) {
            delegate.toNetwork(buffer, recipe);
        }
    }
}

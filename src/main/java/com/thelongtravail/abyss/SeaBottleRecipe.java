package com.thelongtravail.abyss;

import com.google.gson.JsonObject;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public final class SeaBottleRecipe implements SmithingRecipe {
    private final ResourceLocation id;

    public SeaBottleRecipe(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public boolean matches(Container grid, Level level) {
        return ModRegistry.SEA_IN_A_BOTTLE.isPresent()
                && grid.getItem(0).isEmpty()
                && isBaseIngredient(grid.getItem(1))
                && isAdditionIngredient(grid.getItem(2));
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return stack.isEmpty();
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getKey(stack.getItem())
                .equals(new ResourceLocation("the_long_travail", "boundless_essence"));
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return stack.is(Items.POTION) && PotionUtils.getPotion(stack) == Potions.WATER;
    }

    @Override
    public ItemStack assemble(Container grid, RegistryAccess access) {
        return getResultItem(access).copy();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return ModRegistry.SEA_IN_A_BOTTLE.isPresent()
                ? new ItemStack(ModRegistry.SEA_IN_A_BOTTLE.get())
                : ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.SEA_BOTTLE_RECIPE.get();
    }

    @Override
    public boolean isIncomplete() {
        return !ModRegistry.SEA_IN_A_BOTTLE.isPresent();
    }

    public static final class Serializer implements RecipeSerializer<SeaBottleRecipe> {
        @Override
        public SeaBottleRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new SeaBottleRecipe(id);
        }

        @Override
        public SeaBottleRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new SeaBottleRecipe(id);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, SeaBottleRecipe recipe) {}
    }
}

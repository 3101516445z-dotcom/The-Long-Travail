package com.thelongtravail.valley;

import com.google.gson.JsonObject;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

public final class NbtInheritingShapedRecipe extends ShapedRecipe {
    private final Ingredient nbtSource;
    private final boolean repair;

    public NbtInheritingShapedRecipe(
            ResourceLocation id, ShapedRecipe base, Ingredient nbtSource, boolean repair) {
        super(
                id,
                base.getGroup(),
                base.category(),
                base.getWidth(),
                base.getHeight(),
                base.getIngredients(),
                base.getResultItem(RegistryAccess.EMPTY));
        this.nbtSource = nbtSource;
        this.repair = repair;
    }

    @Override
    public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
        ItemStack result = super.assemble(grid, access);
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack source = grid.getItem(i);
            if (!source.isEmpty() && nbtSource.test(source)) {
                if (source.hasTag()) result.setTag(source.getTag().copy());
                if (repair) result.setDamageValue(0);
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.TREMOR_RECIPE.get();
    }

    public static final class Serializer implements RecipeSerializer<NbtInheritingShapedRecipe> {
        private final ShapedRecipe.Serializer delegate = new ShapedRecipe.Serializer();

        @Override
        public NbtInheritingShapedRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new NbtInheritingShapedRecipe(
                    id,
                    delegate.fromJson(id, json),
                    Ingredient.fromJson(json.get("nbt_source")),
                    json.has("repair") && json.get("repair").getAsBoolean());
        }

        @Override
        public NbtInheritingShapedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new NbtInheritingShapedRecipe(
                    id,
                    delegate.fromNetwork(id, buf),
                    Ingredient.fromNetwork(buf),
                    buf.readBoolean());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, NbtInheritingShapedRecipe recipe) {
            delegate.toNetwork(buf, recipe);
            recipe.nbtSource.toNetwork(buf);
            buf.writeBoolean(recipe.repair);
        }
    }
}

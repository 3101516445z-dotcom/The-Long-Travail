package com.thelongtravail.underworld;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public final class KagotsurubeRecipe extends CustomRecipe {
    public KagotsurubeRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    public static boolean cursedBook(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK)
                && EnchantmentHelper.getEnchantments(stack).keySet().stream()
                        .anyMatch(e -> e.isCurse());
    }

    @Override
    public boolean matches(CraftingContainer c, Level level) {
        if (c.getWidth() != 3 || c.getHeight() != 3) return false;
        return c.getItem(0).isEmpty()
                && cursedBook(c.getItem(1))
                && c.getItem(2).isEmpty()
                && c.getItem(3).isEmpty()
                && c.getItem(4)
                        .is(ModRegistry.ESSENCES.get(TravailAspect.UNDERWORLD.ordinal()).get())
                && c.getItem(5).isEmpty()
                && c.getItem(6).is(Items.WITHER_ROSE)
                && c.getItem(7).is(Items.NETHERITE_SWORD)
                && c.getItem(8).is(Items.WITHER_ROSE);
    }

    @Override
    public ItemStack assemble(CraftingContainer c, RegistryAccess access) {
        return new ItemStack(ModRegistry.KAGOTSURUBE.get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width == 3 && height == 3;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return new ItemStack(ModRegistry.KAGOTSURUBE.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.KAGOTSURUBE_RECIPE.get();
    }
}

package com.thelongtravail.compat.jei;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** 仅用于 JEI 展示，实际匹配由 KagotsurubeRecipe 检查诅咒附魔。 */
public final class KagotsurubeDisplay {
    public static ShapedRecipe recipe() {
        List<ItemStack> books = new ArrayList<>();
        for (var enchantment : ForgeRegistries.ENCHANTMENTS) {
            if (!enchantment.isCurse()) continue;
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(enchantment, 1));
            books.add(book);
        }
        var ingredients = NonNullList.withSize(9, Ingredient.EMPTY);
        ingredients.set(1, Ingredient.of(books.stream()));
        ingredients.set(
                4,
                Ingredient.of(ModRegistry.ESSENCES.get(TravailAspect.UNDERWORLD.ordinal()).get()));
        ingredients.set(6, Ingredient.of(Items.WITHER_ROSE));
        ingredients.set(7, Ingredient.of(Items.NETHERITE_SWORD));
        ingredients.set(8, Ingredient.of(Items.WITHER_ROSE));
        return new ShapedRecipe(
                ResourceLocation.fromNamespaceAndPath("the_long_travail", "jei/kagotsurube"),
                "",
                CraftingBookCategory.EQUIPMENT,
                3,
                3,
                ingredients,
                new ItemStack(ModRegistry.KAGOTSURUBE.get()));
    }

    private KagotsurubeDisplay() {}
}

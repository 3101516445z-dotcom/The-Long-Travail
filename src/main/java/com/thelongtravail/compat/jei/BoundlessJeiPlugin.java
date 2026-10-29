package com.thelongtravail.compat.jei;

import com.thelongtravail.registry.ModRegistry;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.stream.IntStream;

@JeiPlugin
public final class BoundlessJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("the_long_travail", "boundless_items");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addItemStackInfo(
                new ItemStack(ModRegistry.ANCIENT_STRANGE_TALES.get()),
                IntStream.range(0, 8)
                        .mapToObj(
                                i ->
                                        Component.translatable(
                                                "tooltip.the_long_travail.ancient_strange_tales.details."
                                                        + i))
                        .toArray(Component[]::new));
    }
}

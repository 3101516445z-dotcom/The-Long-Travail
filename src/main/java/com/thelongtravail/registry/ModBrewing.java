package com.thelongtravail.registry;

import com.thelongtravail.TheLongTravail;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModBrewing {
    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(
                () ->
                        BrewingRecipeRegistry.addRecipe(
                                Ingredient.of(Items.DRAGON_BREATH),
                                Ingredient.of(Items.NETHER_WART),
                                new ItemStack(ModRegistry.LIQUEFIED_ABUNDANCE.get())));
    }

    private ModBrewing() {}
}

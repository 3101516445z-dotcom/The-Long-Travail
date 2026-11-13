package com.thelongtravail.client;

import com.thelongtravail.compat.farmersdelight.FarmersDelightTooltips;
import com.thelongtravail.item.ThemedFoodItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

// 缺少可选食品显示模组时，补充效果文字与营养图示。
@Mod.EventBusSubscriber(modid = "the_long_travail", value = Dist.CLIENT)
public final class FoodTooltips {
    public static List<Component> summary(ItemStack stack) {
        var lines = new ArrayList<Component>();
        if (com.thelongtravail.registry.ModRegistry.MOON_TOAD_PASTRY.isPresent()
                && stack.is(com.thelongtravail.registry.ModRegistry.MOON_TOAD_PASTRY.get()))
            lines.add(ThemeTooltips.text("moon_toad_pastry.obtain"));
        if (!FarmersDelightTooltips.append(stack, lines)) {
            var potion = new ItemStack(Items.POTION);
            var effects =
                    stack.getFoodProperties(Minecraft.getInstance().player).getEffects().stream()
                            .map(com.mojang.datafixers.util.Pair::getFirst)
                            .toList();
            PotionUtils.setCustomEffects(potion, effects);
            PotionUtils.addPotionTooltip(potion, lines, 1f);
        }
        return lines;
    }

    @SubscribeEvent
    public static void gather(
            net.minecraftforge.client.event.RenderTooltipEvent.GatherComponents event) {
        if (event.getItemStack().getItem() instanceof ThemedFoodItem
                && !ModList.get().isLoaded("appleskin"))
            event.getTooltipElements()
                    .add(
                            com.mojang.datafixers.util.Either.right(
                                    new FoodIcons(event.getItemStack())));
    }

    @Mod.EventBusSubscriber(
            modid = "the_long_travail",
            bus = Mod.EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT)
    public static final class Factories {
        @SubscribeEvent
        public static void register(
                net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent
                        event) {
            event.register(FoodIcons.class, component -> component);
        }
    }

    private static final class FoodIcons
            implements net.minecraft.world.inventory.tooltip.TooltipComponent,
                    net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent {
        private static final ResourceLocation HUNGER =
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/icons.png");
        private static final ResourceLocation SATURATION =
                ResourceLocation.fromNamespaceAndPath(
                        "the_long_travail", "textures/gui/food-saturation.png");
        private final int hunger;
        private final float saturation;
        private final boolean rotten;

        private FoodIcons(ItemStack stack) {
            var food = stack.getFoodProperties(Minecraft.getInstance().player);
            hunger = food.getNutrition();
            saturation = hunger * food.getSaturationModifier() * 2;
            rotten =
                    food.getEffects().stream()
                            .anyMatch(
                                    pair ->
                                            pair.getFirst().getEffect().getCategory()
                                                    == net.minecraft.world.effect.MobEffectCategory
                                                            .HARMFUL);
        }

        @Override
        public int getHeight() {
            return 20;
        }

        @Override
        public int getWidth(net.minecraft.client.gui.Font font) {
            return Math.max(
                            (int) Math.ceil(hunger / 2.0) * 9,
                            (int) Math.ceil(saturation / 2.0) * 7)
                    + 2;
        }

        @Override
        public void renderImage(
                net.minecraft.client.gui.Font font, int x, int y, GuiGraphics graphics) {
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            int hungerIcons = (hunger + 1) / 2;
            for (int i = 0; i < hungerIcons; i++) {
                int px = x + (hungerIcons - 1 - i) * 9;
                graphics.blit(HUNGER, px, y, 0, 16f, 27f, 9, 9, 256, 256);
                int u = (rotten ? 88 : 52) + (hunger - i * 2 == 1 ? 9 : 0);
                graphics.blit(HUNGER, px, y, 0, (float) u, 27f, 9, 9, 256, 256);
            }
            int saturationIcons = (int) Math.ceil(saturation / 2);
            for (int i = 0; i < saturationIcons; i++) {
                float part = (saturation - i * 2) / 2;
                float u = part >= 1 ? 21 : part > .5 ? 14 : part > .25 ? 7 : 0;
                graphics.blit(
                        SATURATION,
                        x + (saturationIcons - 1 - i) * 7,
                        y + 10,
                        0,
                        u,
                        27f,
                        7,
                        7,
                        256,
                        256);
            }
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        }
    }
}

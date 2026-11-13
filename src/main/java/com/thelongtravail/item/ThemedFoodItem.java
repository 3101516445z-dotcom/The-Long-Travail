package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.config.ThemeItemConfig;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import java.util.List;

public final class ThemedFoodItem extends Item implements ItemDescription {
    private final boolean flesh;

    public ThemedFoodItem(boolean flesh) {
        super(
                new Properties()
                        .food(
                                new FoodProperties.Builder()
                                        .nutrition(1)
                                        .saturationMod(0)
                                        .alwaysEat()
                                        .build()));
        this.flesh = flesh;
    }

    @Override
    public FoodProperties getFoodProperties(ItemStack stack, LivingEntity entity) {
        return food(
                entity != null
                        ? entity.level().isClientSide
                        : net.minecraftforge.fml.util.thread.EffectiveSide.get().isClient());
    }

    public FoodProperties food(boolean display) {
        String id = flesh ? "living_flesh" : "moon_toad_pastry";
        int nutrition = (int) value(display, id + ".nutrition");
        float saturation = (float) value(display, id + ".saturation");
        var b =
                new FoodProperties.Builder()
                        .nutrition(nutrition)
                        .saturationMod(nutrition == 0 ? 0 : saturation / (2 * nutrition))
                        .alwaysEat();
        if (flesh) {
            add(b, display, id, "physical", ModRegistry.PHYSICAL_PURIFICATION.get());
            add(b, display, id, "wither", MobEffects.WITHER);
            add(b, display, id, "speed", MobEffects.MOVEMENT_SPEED);
            int ticks = (int) Math.round(value(display, id + ".maraSeconds") * 20);
            if (ticks > 0)
                b.effect(
                        () -> new MobEffectInstance(ModRegistry.MARA_TRANSFORMATION.get(), ticks),
                        1);
        } else {
            add(b, display, id, "magic", ModRegistry.MAGIC_PURIFICATION.get());
            add(b, display, id, "resistance", MobEffects.DAMAGE_RESISTANCE);
            add(b, display, id, "saturation", MobEffects.SATURATION);
            add(b, display, id, "luck", MobEffects.LUCK);
        }
        return b.build();
    }

    private static double value(boolean display, String key) {
        return display ? ThemeItemConfig.display(key) : ThemeItemConfig.get(key);
    }

    private static void add(
            FoodProperties.Builder b, boolean display, String id, String key, MobEffect effect) {
        int ticks = (int) Math.round(value(display, id + "." + key + "Seconds") * 20);
        int amplifier = (int) value(display, id + "." + key + "Level") - 1;
        if (ticks > 0) b.effect(() -> new MobEffectInstance(effect, ticks, amplifier), 1);
    }

    @Override
    public Component getName(ItemStack stack) {
        return (flesh ? AspectTheme.UNDERWORLD : AspectTheme.FLOURISHING)
                .name(super.getName(stack));
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        ItemDescription.summary(stack, lines);
    }

    @Override
    public void appendDetails(
            ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        ThemeItemText.details(stack, lines);
    }
}

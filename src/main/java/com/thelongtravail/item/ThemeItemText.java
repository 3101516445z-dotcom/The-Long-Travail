package com.thelongtravail.item;

import com.thelongtravail.config.ThemeItemConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ThemeItemText {
    public static java.util.function.Supplier<Component> skillKey = Component::empty;

    public static void summary(String id, ItemStack stack, List<Component> lines) {
        Object[] args =
                switch (id) {
                    case "sea_in_a_bottle" -> new Object[] {(int) value(id + ".extraSlots")};
                    case "holy_fool" ->
                            new Object[] {skillKey.get(), number(value(id + ".durationSeconds"))};
                    case "last_affairs" ->
                            new Object[] {
                                LastAffairsItem.message(
                                        LastAffairsItem.spawn(stack) ? "spawn" : "death")
                            };
                    default -> new Object[] {};
                };
        appendLines(
                com.thelongtravail.text.TooltipStyle.coloredLine(
                        Component.translatable(
                                        "tooltip.the_long_travail.summary.theme_items." + id, args)
                                .withStyle(
                                        style ->
                                                style.withColor(
                                                        com.thelongtravail.text.TooltipStyle
                                                                .BODY_COLOR))),
                lines);
    }

    private static double value(String key) {
        return ThemeItemConfig.display(key);
    }

    public static void details(ItemStack stack, List<Component> lines) {
        String id =
                net.minecraftforge.registries.ForgeRegistries.ITEMS
                        .getKey(stack.getItem())
                        .getPath();
        Object[] args =
                switch (id) {
                    case "sea_in_a_bottle" ->
                            new Object[] {
                                (int) value(id + ".extraSlots"),
                                value(id + ".healingBonus") * 100,
                                value(id + ".magicBonus") * 100
                            };
                    case "horizon_theory" ->
                            new Object[] {
                                value(id + ".damageCap") * 100,
                                value(id + ".invulnerabilitySeconds")
                            };
                    case "last_affairs" ->
                            new Object[] {
                                value(id + ".holdSeconds"), value(id + ".revivalSeconds")
                            };
                    case "holy_fool" ->
                            new Object[] {
                                value(id + ".durationSeconds"),
                                value(id + ".cooldownSeconds"),
                                skillKey.get()
                            };
                    default ->
                            new Object[] {
                                (int) value(id + ".nutrition"), value(id + ".saturation")
                            };
                };
        for (int i = 0; i < args.length; i++) if (args[i] instanceof Double n) args[i] = number(n);
        var text =
                com.thelongtravail.text.TooltipStyle.coloredLine(
                        Component.translatable(
                                "tooltip.the_long_travail." + id + ".details", args));
        appendLines(text, lines);
        if (id.equals("moon_toad_pastry") || id.equals("living_flesh")) {
            var food = ((ThemedFoodItem) stack.getItem()).food(true);
            var effects = food.getEffects();
            for (int i = 0; i < effects.size(); i++) {
                var effect = effects.get(i).getFirst();
                String rank =
                        effect.getAmplifier() == 0
                                ? ""
                                : com.thelongtravail.flourishing.FlowerDescriptions.effectLevel(
                                        effect.getAmplifier() + 1);
                lines.add(
                        Component.translatable(
                                "tooltip.the_long_travail.theme_food.effect"
                                        + (i == effects.size() - 1 ? ".last" : ""),
                                effect.getEffect().getDisplayName(),
                                rank,
                                number(effect.getDuration() / 20.0)));
            }
            lines.add(Component.empty());
            lines.add(Component.translatable("tooltip.the_long_travail.theme_food.always_edible"));
            if (id.equals("moon_toad_pastry")) {
                lines.add(Component.empty());
                lines.add(
                        Component.translatable(
                                "tooltip.the_long_travail.moon_toad_pastry.harvest"));
            }
        }
    }

    private static void appendLines(Component text, List<Component> lines) {
        net.minecraft.network.chat.MutableComponent[] current = {Component.empty()};
        text.visit(
                (style, fragment) -> {
                    String[] parts = fragment.split("\\n", -1);
                    for (int i = 0; i < parts.length; i++) {
                        if (i > 0) {
                            lines.add(current[0]);
                            current[0] = Component.empty();
                        }
                        current[0].append(Component.literal(parts[i]).setStyle(style));
                    }
                    return java.util.Optional.empty();
                },
                net.minecraft.network.chat.Style.EMPTY);
        lines.add(current[0]);
    }

    private static String number(double value) {
        return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private ThemeItemText() {}
}

package com.thelongtravail.valley;

import com.thelongtravail.config.DeepValleyItemsConfig;

import net.minecraft.network.chat.Component;

import java.util.*;

public final class DeepValleyItemText {
    private static String percent(String key) {
        return number(100 * DeepValleyItemsConfig.shown(key));
    }

    private static String number(double value) {
        return java.math.BigDecimal.valueOf(value)
                .setScale(6, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static List<Component> effects(String id) {
        List<Component> out = new ArrayList<>();
        String prefix = "tooltip.the_long_travail." + id + ".";
        if (id.equals("phosphophyllite")) {
            out.add(title("malice"));
            out.add(
                    Component.translatable(
                            prefix + "malice",
                            percent(id + ".maliceAttributes"),
                            percent(id + ".maliceDamage")));
            out.add(Component.empty());
            out.add(title("witness"));
            out.add(
                    Component.translatable(
                            prefix + "witness",
                            percent(id + ".witnessAttributes"),
                            percent(id + ".witnessDamage"),
                            percent(id + ".healChance"),
                            percent(id + ".healFraction")));
        } else {
            double ore = DeepValleyItemsConfig.shown(id + ".oreChance"),
                    mine = DeepValleyItemsConfig.shown(id + ".mineshaftChance"),
                    city = DeepValleyItemsConfig.shown(id + ".cityChance"),
                    d = Math.max(1, mine + city);
            out.add(
                    Component.translatable(
                            prefix + "base", number(ore * 100), number((1 - ore) * 100)));
            out.add(Component.empty());
            out.add(title("malice"));
            out.add(
                    Component.translatable(
                            prefix + "malice",
                            number(mine / d * 100),
                            number(city / d * 100),
                            number(Math.max(0, 1 - (mine + city) / d) * 100)));
            out.add(Component.empty());
            out.add(title("witness"));
            out.add(Component.translatable(prefix + "witness"));
        }
        return out;
    }

    private static Component title(String branch) {
        return Component.translatable("tooltip.the_long_travail.deep_valley." + branch + ".title")
                .withStyle(style -> style.withColor(0xC9B8FF));
    }

    public static List<Component> details(String id) {
        return effects(id);
    }

    private DeepValleyItemText() {}
}

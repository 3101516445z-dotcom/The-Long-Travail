package com.thelongtravail.flourishing;

import com.thelongtravail.abyss.RainTooltips;
import com.thelongtravail.config.FlourishingGrowthConfig;
import com.thelongtravail.config.FlourishingItemsConfig;
import com.thelongtravail.network.TooltipConfigSync;
import net.minecraft.network.chat.Component;
import java.math.BigDecimal;
import java.util.*;

public final class FlourishingItemText {
    private static double value(String key) {
        double fallback =
                switch (key) {
                    case "mistletoe.maxLevel" -> FlourishingGrowthConfig.MAX_LEVEL.get();
                    case "growth.radius" -> FlourishingGrowthConfig.RADIUS.get();
                    case "growth.height" -> FlourishingGrowthConfig.HEIGHT.get();
                    case "growth.interval" -> FlourishingGrowthConfig.INTERVAL.get();
                    case "resistanceLevel" -> FlourishingGrowthConfig.RESISTANCE_LEVEL.get();
                    default -> FlourishingItemsConfig.get(key);
                };
        return TooltipConfigSync.decimal("floral." + key, fallback);
    }

    private static String n(double v) {
        return BigDecimal.valueOf(v).stripTrailingZeros().toPlainString();
    }

    private static String percent(String key) {
        return BigDecimal.valueOf(value(key)).movePointRight(2).stripTrailingZeros().toPlainString();
    }

    private static Component line(String key, Object... args) {
        return RainTooltips.coloredLine(
                Component.translatable("tooltip.the_long_travail.growth_items." + key, args)
                        .withStyle(s -> s.withColor(RainTooltips.BODY_COLOR)));
    }

    public static List<Component> details(boolean flower) {
        var out = new ArrayList<Component>();
        String id = flower ? "everlasting_flower." : "mistletoe.";
        if (!flower) {
            out.add(line(id + "0", n(value("mistletoe.durationSeconds"))));
            out.add(
                    line(
                            id + "1",
                            percent("mistletoe.speedBonusPerLevel"),
                            percent("mistletoe.damageBonusPerLevel"),
                            percent("mistletoe.damageReductionPerLevel")));
            out.add(line(id + "2", n(value("mistletoe.durationSeconds")), n(value("mistletoe.maxLevel"))));
            out.add(line(id + "3"));
            out.add(Component.empty());
            out.add(line(id + "4", percent("mistletoe.expiryDamagePerLevel")));
            double chance = value("mistletoe.lootChancePerLevel");
            int guaranteed = chance > 0 ? (int) Math.ceil(1 / chance) : Integer.MAX_VALUE;
            if (guaranteed <= value("mistletoe.maxLevel"))
                out.add(line(id + "5", percent("mistletoe.lootChancePerLevel"), guaranteed));
            else out.add(line(id + "roll_only", percent("mistletoe.lootChancePerLevel")));
            out.add(line(id + "6"));
            out.add(Component.empty());
            out.add(line(id + "7"));
        } else {
            out.add(line(id + "0", n(value("growth.radius")), n(value("growth.height"))));
            out.add(line(id + "1"));
            out.add(Component.empty());
            out.add(line(id + "2"));
            double seconds = value("growth.interval") / 20;
            out.add(seconds == 1 ? line(id + "every_second") : line(id + "3", n(seconds)));
            out.add(Component.empty());
            out.add(line(id + "4"));
            int rank = (int) value("resistanceLevel");
            String roman =
                    rank >= 1 && rank <= 5 ? new String[] {"Ⅰ", "Ⅱ", "Ⅲ", "Ⅳ", "Ⅴ"}[rank - 1] : Integer.toString(rank);
            out.add(
                    line(
                            id + "5",
                            percent("everlasting_flower.malice.resistanceChance"),
                            roman,
                            n(value("everlasting_flower.malice.resistanceSeconds"))));
            out.add(
                    line(
                            id + "6",
                            percent("everlasting_flower.malice.speedBonus"),
                            percent("everlasting_flower.malice.damageReduction")));
            out.add(Component.empty());
            out.add(line(id + "7"));
            out.add(line(id + "8", percent("everlasting_flower.witness.repeatChance")));
            out.add(line(id + "9", percent("everlasting_flower.witness.damageBonus")));
            out.add(Component.empty());
            for (int i = 10; i <= 14; i++) out.add(line(id + i));
        }
        return out;
    }

    private FlourishingItemText() {}
}

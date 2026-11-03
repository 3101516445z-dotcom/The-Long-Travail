package com.thelongtravail.client.catalog;

import com.thelongtravail.config.*;
import com.thelongtravail.network.TooltipConfigSync;

import net.minecraft.network.chat.Component;

import java.math.BigDecimal;
import java.util.*;

// 穷遐拾遗正文复用服务端同步参数。
public final class CatalogFarReachDetails {
    private static Component line(String key, Object... args) {
        return com.thelongtravail.client.TooltipTranslations.text(
                "gui.the_long_travail.catalog.far." + key, args);
    }

    private static void section(List<Component> out, String key) {
        out.add(Component.empty());
        out.add(line(key).copy().withStyle(s -> s.withColor(CatalogTextStyle.CONDITION)));
    }

    private static String n(String key, int scale) {
        double fallback =
                key.startsWith("sun_chaser.")
                        ? SunBladeConfig.value(key)
                        : key.startsWith("don_quixote.") || key.startsWith("babel.")
                                ? FarReachRelicsConfig.value(key)
                                : key.equals("icarus.igniteSeconds")
                                        ? FarReachItemsConfig.IGNITE_SECONDS.get()
                                        : FarReachItemsConfig.get(key);
        double value =
                TooltipConfigSync.decimal(
                        (key.startsWith("sun_chaser.") ? "sunBlade." : "farItem.") + key, fallback);
        return BigDecimal.valueOf(value)
                .multiply(BigDecimal.valueOf(scale))
                .stripTrailingZeros()
                .toPlainString();
    }

    private static void bullets(List<Component> out, Component... rows) {
        for (int i = 0; i < rows.length; i++)
            out.add(line(i == rows.length - 1 ? "last" : "row", rows[i]));
    }

    public static List<Component> details(String id) {
        var out = new ArrayList<Component>();
        switch (id) {
            case "golden_age" -> {
                out.add(line("gold.0"));
                out.add(line("carrying"));
                out.add(Component.empty());
                out.add(line("gold.1"));
                out.add(line("gold.2", n("golden_age.waterTime", 1), n("golden_age.range", 1)));
                section(out, "gold.malice");
                out.add(line("gold.3", n("golden_age.enchantmentChance", 100)));
                section(out, "gold.witness");
                out.add(line("gold.4", n("golden_age.foodChance", 100)));
            }
            case "icarus" -> {
                out.add(line("icarus.0"));
                bullets(
                        out,
                        line("icarus.1"),
                        line("icarus.2"),
                        line("icarus.3", n("icarus.igniteSeconds", 1)));
                out.add(line("icarus.4"));
                section(out, "malice");
                out.add(line("icarus.5", n("icarus.malicePeak", 100)));
                String peak =
                        new BigDecimal(n("icarus.malicePeak", 100))
                                .add(new BigDecimal(n("icarus.sunnyExtraPeak", 100)))
                                .stripTrailingZeros()
                                .toPlainString();
                out.add(line("icarus.6", peak));
                section(out, "witness");
                bullets(out, line("icarus.7"), line("icarus.8", n("icarus.witnessBonus", 100)));
            }
            case "don_quixote" -> {
                out.add(line("quixote.0"));
                out.add(
                        line(
                                "quixote.1",
                                n("don_quixote.sizeFactor", 100),
                                n("don_quixote.maxBonus", 100)));
                section(out, "malice");
                out.add(
                        line(
                                "quixote.2",
                                n("don_quixote.healthRatio", 1),
                                n("don_quixote.trueFraction", 100)));
                section(out, "witness");
                out.add(
                        line(
                                "quixote.3",
                                n("don_quixote.witnessThreshold", 100),
                                n("don_quixote.reduction", 100),
                                n("don_quixote.duration", 1)));
                out.add(line("quixote.4", n("don_quixote.cooldown", 1)));
            }
            case "tower_of_babel" -> {
                out.add(line("babel.0"));
                out.add(line("babel.1"));
                out.add(line("carrying"));
                section(out, "malice");
                bullets(
                        out,
                        line("babel.2", n("babel.priceFactor", 100)),
                        line(
                                "babel.3",
                                n("babel.restockRange", 1),
                                n("babel.restockSeconds", 1),
                                n("babel.restocksPerDay", 1)));
                out.add(line("babel.4"));
                out.add(line("babel.5"));
            }
            case "sun_chaser" -> {
                out.add(line("sun.0"));
                bullets(out, line("sun.1"), line("sun.2", n("sun_chaser.healing", 1)));
                out.add(line("carrying"));
                section(out, "malice");
                bullets(
                        out,
                        line("sun.3"),
                        line("sun.4", n("sun_chaser.projectileBonus", 100)),
                        line("sun.5", n("sun_chaser.burningBonus", 100)));
                section(out, "witness");
                bullets(
                        out,
                        line("sun.6", n("sun_chaser.suppressionSeconds", 1)),
                        line("sun.7", n("sun_chaser.strikeFraction", 100)));
            }
            default -> throw new IllegalArgumentException(id);
        }
        return out;
    }

    private CatalogFarReachDetails() {}
}

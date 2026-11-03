package com.thelongtravail.client.catalog;

import com.thelongtravail.boundless.BoundlessTooltips;
import com.thelongtravail.config.BoundlessItemsConfig;
import com.thelongtravail.network.TooltipConfigSync;

import net.minecraft.network.chat.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// 数值从服务端同步的配置快照读取。
public final class CatalogBoundlessDetails {
    private static Component line(String key, Object... args) {
        return com.thelongtravail.client.TooltipTranslations.text(
                "gui.the_long_travail.catalog.boundless." + key, args);
    }

    private static String n(String key, int multiplier) {
        return BigDecimal.valueOf(
                        TooltipConfigSync.decimal(
                                "boundlessItem." + key, BoundlessItemsConfig.get(key)))
                .multiply(BigDecimal.valueOf(multiplier))
                .stripTrailingZeros()
                .toPlainString();
    }

    private static void heading(List<Component> out, String key) {
        out.add(line(key).copy().withStyle(s -> s.withColor(CatalogTextStyle.CONDITION)));
    }

    private static void section(List<Component> out, String key) {
        out.add(Component.empty());
        heading(out, key);
    }

    private static void bullets(List<Component> out, Component... lines) {
        for (int i = 0; i < lines.length; i++)
            out.add(line(i == lines.length - 1 ? "last" : "row", lines[i]));
    }

    public static List<Component> details(String id) {
        var out = new ArrayList<Component>();
        switch (id) {
            case "free_and_easy_wandering" -> {
                out.add(line("dream.0", BoundlessTooltips.dreamKey.get(), n("dream.duration", 1)));
                out.add(line("dream.1"));
                out.add(line("dream.2", n("dream.cooldown", 1)));
                out.add(Component.empty());
                out.add(line("dream.3"));
            }
            case "star_voice" -> {
                heading(out, "malice");
                out.add(line("star.0", n("star.maliceChance", 100), n("star.maliceDuration", 1)));
                out.add(line("star.interval", n("star.maliceInterval", 1)));
                section(out, "witness");
                out.add(line("star.1", n("star.witnessChance", 100), n("star.witnessDuration", 1)));
                out.add(line("star.interval", n("star.witnessInterval", 1)));
                out.add(line("star.2", n("star.witnessCooldown", 1)));
                section(out, "star.heading");
                out.add(line("star.7"));
                out.add(line("star.3", n("star.radius", 1)));
                out.add(line("star.4"));
                section(out, "star.group_heading");
                out.add(line("star.5"));
                out.add(line("star.6", n("star.groupLimit", 1)));
                out.add(line("star.8"));
                out.add(line("star.9"));
            }
            case "little_prince" -> {
                out.add(line("prince.0", n("prince.stepHeight", 1)));
                out.add(line("prince.1"));
            }
            case "lorentz_fantasy" -> {
                out.add(line("lorentz.0", n("lorentz.pierce", 1)));
                out.add(line("lorentz.1"));
                section(out, "malice");
                bullets(
                        out,
                        line("lorentz.2", n("lorentz.critical", 100)),
                        line("lorentz.3", n("lorentz.damage", 100)),
                        line("lorentz.4", n("lorentz.headshot", 100)));
                section(out, "witness");
                out.add(line("lorentz.5", n("lorentz.voidRatio", 100)));
                out.add(line("lorentz.6", n("lorentz.interval", 1)));
            }
            case "ancient_strange_tales" -> {
                out.add(line("tales.0"));
                out.add(line("tales.1"));
                out.add(Component.empty());
                out.add(line("tales.2"));
                out.add(line("tales.3"));
                out.add(
                        line("tales.materials")
                                .copy()
                                .withStyle(
                                        style ->
                                                style.withColor(CatalogTextStyle.GOLD)
                                                        .withInsertion(
                                                                "the_long_travail:emphasis")));
                bullets(out, line("tales.4"), line("tales.5"), line("tales.6"));
                out.add(line("tales.7"));
                out.add(line("tales.8"));
                section(out, "tales.filter");
                bullets(out, line("tales.9"), line("tales.10"));
                section(out, "tales.limits");
                bullets(out, line("tales.11"), line("tales.12"), line("tales.13"));
            }
            default -> throw new IllegalArgumentException(id);
        }
        return out;
    }

    private CatalogBoundlessDetails() {}
}

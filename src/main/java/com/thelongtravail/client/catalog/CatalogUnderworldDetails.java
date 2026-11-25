package com.thelongtravail.client.catalog;

import com.thelongtravail.config.SunBladeConfig;
import com.thelongtravail.config.UnderworldItemsConfig;
import com.thelongtravail.network.TooltipConfigSync;
import com.thelongtravail.underworld.UnderworldRelicText;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

// 冥府拾遗正文，数值沿用服务端同步配置。
public final class CatalogUnderworldDetails {
    private static Component line(String key, Object... args) {
        return com.thelongtravail.client.TooltipTranslations.text(
                "gui.the_long_travail.catalog.underworld." + key, args);
    }

    private static void heading(List<Component> out, String key) {
        out.add(CatalogTextStyle.heading(line(key)));
    }

    private static void section(List<Component> out, String key) {
        out.add(Component.empty());
        heading(out, key);
    }

    private static void bullets(List<Component> out, Component... rows) {
        CatalogTextStyle.bullets(
                out, java.util.Arrays.asList(rows), CatalogUnderworldDetails::line);
    }

    private static String relic(String key, int scale) {
        return UnderworldRelicText.number(UnderworldRelicText.shown(key) * scale);
    }

    private static String blade(String key) {
        return blade(key, 1);
    }

    private static String blade(String key, int scale) {
        String full = "kagotsurube." + key;
        return UnderworldRelicText.number(
                TooltipConfigSync.decimal("sunBlade." + full, SunBladeConfig.value(full)) * scale);
    }

    public static List<Component> details(String id) {
        var out = new ArrayList<Component>();
        switch (id) {
            case "a_thousand_years_later" -> {
                heading(out, "malice");
                out.add(
                        line(
                                "thousand.malice",
                                UnderworldRelicText.number(
                                        100
                                                * TooltipConfigSync.decimal(
                                                        "underworldItem.wither",
                                                        UnderworldItemsConfig.WITHER_BONUS
                                                                .get()))));
                section(out, "witness");
                out.add(line("thousand.witness"));
            }
            case "book_of_the_dead" -> {
                out.add(line("book.base"));
                out.add(line("book.hardcore"));
                section(out, "malice");
                out.add(line("book.conditions"));
                bullets(out, line("book.effects"), line("book.damage"));
                out.add(Component.empty());
                out.add(line("book.exclusions"));
            }
            case "demon_king_of_the_sixth_heaven" -> {
                heading(out, "malice");
                bullets(
                        out,
                        line("king.burning", relic("king.burningBonus", 100)),
                        line("king.heal", relic("king.lifesteal", 100)),
                        line("king.undead", relic("king.undeadReduction", 100)));
                section(out, "witness");
                bullets(
                        out,
                        line("king.overlord", relic("king.overlord", 1)),
                        line("king.environment", relic("king.environmentBonus", 100)));
                out.add(line("king.overlap"));
            }
            case "myriad_souls_banner" -> {
                out.add(line("banner.base", relic("banner.capacity", 1)));
                out.add(line("banner.persistence"));
                section(out, "malice");
                out.add(line("banner.malice", relic("banner.maliceCriticalPerSoul", 100)));
                section(out, "witness");
                bullets(
                        out,
                        line(
                                "banner.insight",
                                relic("banner.insightPerSoul", 1),
                                relic("banner.insightCap", 1)),
                        line("banner.critical", relic("banner.witnessCriticalPerSoul", 100)));
            }
            case "kagotsurube" -> {
                out.add(line("blade.base", blade("witherSeconds")));
                out.add(line("blade.extend", blade("witherSeconds")));
                out.add(line("blade.infinite"));
                section(out, "malice");
                out.add(line("blade.direct_strike"));
                section(out, "witness");
                bullets(
                        out,
                        line("blade.physical_bonus", blade("physicalBonus", 100)),
                        line("blade.insight", blade("insight")),
                        line("blade.overlord", blade("overlord")));
            }
            default -> throw new IllegalArgumentException(id);
        }
        return out;
    }

    private CatalogUnderworldDetails() {}
}

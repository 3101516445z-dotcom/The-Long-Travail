package com.thelongtravail.client.catalog;

import com.thelongtravail.config.*;
import com.thelongtravail.network.TooltipConfigSync;
import com.thelongtravail.config.SwordLanternConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

// 幽谷拾遗正文及补充卡片，复用服务端同步配置。
public final class CatalogValleyDetails {
    private static Component line(String key, Object... args) {
        return com.thelongtravail.client.TooltipTranslations.text(
                "gui.the_long_travail.catalog.valley." + key, args);
    }

    private static String number(double value) {
        return BigDecimal.valueOf(value)
                .setScale(6, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static String percent(double value) {
        return number(value * 100);
    }

    private static double value(String key) {
        return DeepValleyItemsConfig.shown(key);
    }

    private static String stat(String key) {
        return percent(value("phosphophyllite." + key));
    }

    private static double lantern(String key) {
        double fallback =
                switch (key) {
                    case "memory" -> SwordLanternConfig.MEMORY.get();
                    case "timeout" -> SwordLanternConfig.TIMEOUT.get();
                    case "duration" -> SwordLanternConfig.DURATION.get();
                    case "radius" -> SwordLanternConfig.RADIUS.get();
                    case "crowdMinimum" -> SwordLanternConfig.CROWD_MINIMUM.get();
                    case "aloneBonus" -> SwordLanternConfig.ALONE_BONUS.get();
                    case "crowdBonus" -> SwordLanternConfig.CROWD_BONUS.get();
                    case "undeadBonus" -> SwordLanternConfig.UNDEAD_BONUS.get();
                    case "prolongedBonus" -> SwordLanternConfig.PROLONGED_BONUS.get();
                    case "lowHealthBonus" -> SwordLanternConfig.LOW_HEALTH_BONUS.get();
                    case "health" -> SwordLanternConfig.HEALTH.get();
                    default -> throw new IllegalArgumentException(key);
                };
        return TooltipConfigSync.decimal("swordLantern." + key, fallback);
    }

    public static Component card(String id) {
        String key = id.equals("lantern_enemies") ? "memory" : "timeout";
        return com.thelongtravail.client.TooltipTranslations.text(
                "gui.the_long_travail.catalog.glossary." + id + ".body", number(lantern(key)));
    }

    private static void heading(List<Component> out, String key) {
        out.add(line(key).copy().withStyle(s -> s.withColor(CatalogTextStyle.CONDITION)));
    }

    private static void section(List<Component> out, String key) {
        out.add(Component.empty());
        heading(out, key);
    }

    private static void bullets(List<Component> out, Component... rows) {
        for (int i = 0; i < rows.length; i++)
            out.add(line(i == rows.length - 1 ? "last" : "row", rows[i]));
    }

    public static List<Component> details(String id) {
        var out = new ArrayList<Component>();
        switch (id) {
            case "azrael" -> {
                heading(out, "malice");
                out.add(
                        line(
                                "azrael.0",
                                percent(
                                        TooltipConfigSync.decimal(
                                                "azrael.malice", AzraelConfig.MALICE.get()))));
                out.add(line("azrael.1"));
                section(out, "witness");
                out.add(
                        line(
                                "azrael.2",
                                percent(
                                        TooltipConfigSync.decimal(
                                                "azrael.target", AzraelConfig.targetChance())),
                                percent(
                                        TooltipConfigSync.decimal(
                                                "azrael.self", AzraelConfig.selfChance()))));
                out.add(line("azrael.3"));
            }
            case "sword_and_lantern" -> {
                out.add(line("lantern.0"));
                out.add(line("lantern.1"));
                out.add(line("lantern.2"));
                section(out, "lantern.heading");
                bullets(
                        out,
                        line("lantern.3", percent(lantern("aloneBonus"))),
                        line(
                                "lantern.4",
                                number(lantern("crowdMinimum")),
                                percent(lantern("crowdBonus"))),
                        line("lantern.5", percent(lantern("undeadBonus"))),
                        line(
                                "lantern.6",
                                number(lantern("duration") / 60),
                                percent(lantern("prolongedBonus"))),
                        line(
                                "lantern.7",
                                percent(lantern("health")),
                                percent(lantern("lowHealthBonus"))));
                out.add(line("lantern.8", number(lantern("radius"))));
            }
            case "phosphophyllite" -> {
                heading(out, "malice");
                out.add(line("phos.malice", stat("maliceAttributes"), stat("maliceDamage")));
                section(out, "witness");
                bullets(
                        out,
                        line("phos.stats", stat("witnessAttributes"), stat("witnessDamage")),
                        line("phos.heal", stat("healChance"), stat("healFraction")));
                out.add(line("phos.end"));
            }
            case "underground_velvet" -> {
                double ore = value("underground_velvet.oreChance"),
                        mine = value("underground_velvet.mineshaftChance"),
                        city = value("underground_velvet.cityChance"),
                        divisor = Math.max(1, mine + city);
                heading(out, "velvet.0");
                bullets(
                        out,
                        line("velvet.ore", percent(ore)),
                        line("velvet.ingot", percent(1 - ore)));
                section(out, "with_malice");
                out.add(
                        line("velvet.roll")
                                .copy()
                                .withStyle(
                                        s ->
                                                s.withColor(CatalogTextStyle.GOLD)
                                                        .withInsertion(
                                                                "the_long_travail:emphasis")));
                bullets(
                        out,
                        line("velvet.mine", percent(mine / divisor)),
                        line("velvet.city", percent(city / divisor)),
                        line("velvet.none", percent(Math.max(0, 1 - (mine + city) / divisor))));
                section(out, "with_witness");
                out.add(line("velvet.exp"));
            }
            case "high_frequency_tremor" -> {
                out.add(line("tremor.0"));
                out.add(line("tremor.1"));
                section(out, "malice");
                out.add(line("tremor.2"));
                out.add(line("tremor.3"));
                section(out, "witness");
                out.add(line("tremor.4"));
                heading(out, "tremor.blacklist");
                var ids =
                        TooltipConfigSync.pool(
                                "deepValley.tremorBlacklist",
                                DeepValleyItemsConfig.TREMOR_BLACKLIST.get());
                var names = Component.empty();
                for (String raw : ids) {
                    if (!names.getSiblings().isEmpty()) names.append(line("separator"));
                    var key = ResourceLocation.tryParse(raw);
                    Component name =
                            key != null && ForgeRegistries.BLOCKS.containsKey(key)
                                    ? ForgeRegistries.BLOCKS.getValue(key).getName()
                                    : Component.literal(raw);
                    names.append(
                            name.copy()
                                    .withStyle(
                                            s ->
                                                    s.withColor(CatalogTextStyle.WARNING)
                                                            .withInsertion(
                                                                    "the_long_travail:blacklist_name")));
                }
                if (ids.isEmpty()) names.append(line("none"));
                out.add(names);
            }
            default -> throw new IllegalArgumentException(id);
        }
        return out;
    }

    private CatalogValleyDetails() {}
}

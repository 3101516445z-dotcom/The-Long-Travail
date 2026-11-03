package com.thelongtravail.client.catalog;

import com.thelongtravail.config.FlourishingGrowthConfig;
import com.thelongtravail.config.FlourishingItemsConfig;
import com.thelongtravail.flourishing.Flower;
import com.thelongtravail.flourishing.FlowerDescriptions;
import com.thelongtravail.network.TooltipConfigSync;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

// 拾遗专用分区，数值与花朵效果继续使用原有配置读取路径。
public final class CatalogFlourishingDetails {
    private static final String PREFIX = "gui.the_long_travail.catalog.floral.";

    private static Component line(String key, Object... args) {
        return com.thelongtravail.client.TooltipTranslations.text(PREFIX + key, args);
    }

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

    private static String n(String key) {
        return key.startsWith("affection.")
                ? FlowerDescriptions.number(value(key))
                : java.math.BigDecimal.valueOf(value(key)).stripTrailingZeros().toPlainString();
    }

    private static String p(String key) {
        return key.startsWith("affection.")
                ? FlowerDescriptions.percent(value(key))
                : java.math.BigDecimal.valueOf(value(key))
                        .movePointRight(2)
                        .stripTrailingZeros()
                        .toPlainString();
    }

    public static Component growthCard() {
        double seconds = value("growth.interval") / 20;
        return com.thelongtravail.client.TooltipTranslations.text(
                "gui.the_long_travail.catalog.glossary.growth."
                        + (seconds == 1 ? "every_second" : "body"),
                FlowerDescriptions.number(seconds));
    }

    public static List<Component> details(String id) {
        var out = new ArrayList<Component>();
        switch (id) {
            case "spring_corner" -> {
                out.add(line("spring.0"));
                out.add(line("spring.1"));
                out.add(Component.empty());
                out.add(condition("spring.heading"));
                Flower[] flowers = Flower.values();
                for (int i = 0; i < flowers.length; i++) {
                    Flower flower = flowers[i];
                    var name =
                            flower.item()
                                    .getDescription()
                                    .copy()
                                    .withStyle(
                                            s ->
                                                    s.withColor(CatalogTextStyle.GOLD)
                                                            .withInsertion(
                                                                    "the_long_travail:flower_name"));
                    var disabled =
                            FlourishingItemsConfig.shownEnabled(flower)
                                    ? Component.empty()
                                    : com.thelongtravail.client.TooltipTranslations.text(
                                            "tooltip.the_long_travail.flower.catalog_disabled");
                    out.add(
                            line(
                                    i == flowers.length - 1 ? "flower.last" : "flower.row",
                                    name,
                                    FlowerDescriptions.describe(
                                            flower, PREFIX + "flower." + flower.id),
                                    disabled));
                }
            }
            case "affection" -> {
                out.add(line("affection.0"));
                out.add(line("affection.1"));
                out.add(line("affection.range", n("affection.range")));
                out.add(Component.empty());
                out.add(condition("affection.companion"));
                out.add(line("affection.2", p("affection.servantDamageBonus")));
                out.add(line("affection.3", p("affection.servantDamageReduction")));
                out.add(Component.empty());
                out.add(condition("affection.owner"));
                out.add(line("affection.4", p("affection.playerDamageBonus")));
                out.add(line("affection.5", p("affection.playerDamageReduction")));
                out.add(line("affection.6", p("affection.playerSpeedBonus")));
            }
            case "mistletoe" -> {
                out.add(line("mistletoe.0", n("mistletoe.durationSeconds")));
                out.add(
                        line(
                                "mistletoe.1",
                                n("mistletoe.durationSeconds"),
                                n("mistletoe.maxLevel")));
                out.add(Component.empty());
                out.add(condition("mistletoe.heading"));
                out.add(line("mistletoe.2", p("mistletoe.speedBonusPerLevel")));
                out.add(line("mistletoe.3", p("mistletoe.damageBonusPerLevel")));
                out.add(line("mistletoe.4", p("mistletoe.damageReductionPerLevel")));
                out.add(Component.empty());
                out.add(condition("mistletoe.expiry"));
                out.add(line("mistletoe.5", p("mistletoe.expiryDamagePerLevel")));
                out.add(line("mistletoe.6", p("mistletoe.lootChancePerLevel")));
                out.add(Component.empty());
                out.add(line("mistletoe.7"));
                out.add(line("mistletoe.8"));
            }
            case "everlasting_flower" -> {
                out.add(line("everlasting.0", n("growth.radius"), n("growth.height")));
                out.add(line("everlasting.1"));
                out.add(Component.empty());
                out.add(condition("everlasting.malice"));
                int rank = (int) value("resistanceLevel");
                String level = FlowerDescriptions.effectLevel(rank);
                out.add(
                        line(
                                "everlasting.2",
                                p("everlasting_flower.malice.resistanceChance"),
                                level,
                                n("everlasting_flower.malice.resistanceSeconds")));
                out.add(
                        line(
                                "everlasting.3",
                                p("everlasting_flower.malice.speedBonus"),
                                p("everlasting_flower.malice.damageReduction")));
                out.add(Component.empty());
                out.add(condition("everlasting.witness"));
                out.add(line("everlasting.4", p("everlasting_flower.witness.repeatChance")));
                out.add(line("everlasting.5", p("everlasting_flower.witness.damageBonus")));
            }
            case "immortal_bud" -> {
                out.add(line("bud.0"));
                out.add(line("bud.1"));
                out.add(line("bud.2"));
                out.add(Component.empty());
                out.add(line("bud.3"));
                var blocked =
                        TooltipConfigSync.pool(
                                "bud.blacklist", FlourishingItemsConfig.BUD_BLACKLIST.get());
                if (!blocked.isEmpty()) {
                    out.add(Component.empty());
                    out.add(line("bud.blacklist"));
                    var names = Component.empty();
                    for (int i = 0; i < blocked.size(); i++) {
                        String raw = blocked.get(i);
                        var key = ResourceLocation.tryParse(raw);
                        Component name =
                                key != null && ForgeRegistries.ITEMS.containsKey(key)
                                        ? ForgeRegistries.ITEMS.getValue(key).getDescription()
                                        : Component.literal(raw);
                        if (i > 0) names.append(line("bud.separator"));
                        names.append(
                                name.copy()
                                        .withStyle(
                                                s ->
                                                        s.withColor(CatalogTextStyle.WARNING)
                                                                .withInsertion(
                                                                        "the_long_travail:blacklist_name")));
                    }
                    out.add(names);
                }
            }
            default -> throw new IllegalArgumentException(id);
        }
        return out;
    }

    private static Component condition(String key) {
        return line(key).copy().withStyle(s -> s.withColor(CatalogTextStyle.CONDITION));
    }

    private CatalogFlourishingDetails() {}
}

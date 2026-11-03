package com.thelongtravail.client.catalog;

import com.thelongtravail.abyss.AbyssItemText;
import com.thelongtravail.client.RainClient;
import com.thelongtravail.client.ThemeTooltips;
import com.thelongtravail.config.AbyssItemsConfig;
import com.thelongtravail.config.RainConfig;
import com.thelongtravail.network.TooltipConfigSync;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.math.BigDecimal;
import java.util.*;

// 拾遗专用文稿。配置经现有 TooltipConfigSync 同步，不改变物品行为。
public final class CatalogAbyssDetails {
    private static final String PREFIX = "gui.the_long_travail.catalog.abyss.";

    private static Component line(String key, Object... args) {
        return com.thelongtravail.client.TooltipTranslations.text(PREFIX + key, args);
    }

    private static Component heading(String key) {
        return line(key).copy().withStyle(s -> s.withColor(CatalogTextStyle.CONDITION));
    }

    private static String number(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static String percent(double value) {
        return BigDecimal.valueOf(value).movePointRight(2).stripTrailingZeros().toPlainString();
    }

    private static double rain(String key, Number fallback) {
        return TooltipConfigSync.decimal("catalogRain." + key, fallback.doubleValue());
    }

    private static boolean flag(String key, boolean fallback) {
        return rain(key, fallback ? 1 : 0) != 0;
    }

    private static void weather(List<Component> out, boolean eki) {
        if (flag(
                eki ? "ekiLocal" : "tokaidoLocal",
                eki ? RainConfig.EKI_LOCAL.get() : RainConfig.TOKAIDO_LOCAL.get()))
            out.add(line("local"));
        if (flag(
                eki ? "ekiSky" : "tokaidoSky",
                eki ? RainConfig.EKI_SKY.get() : RainConfig.TOKAIDO_SKY.get()))
            out.add(line("sky"));
    }

    private static void bullets(List<Component> out, List<Component> rows) {
        for (int i = 0; i < rows.size(); i++)
            out.add(line(i == rows.size() - 1 ? "last" : "row", rows.get(i)));
    }

    public static List<Component> details(String id) {
        var out = new ArrayList<Component>();
        switch (id) {
            case "eki" -> {
                out.add(line("eki.intro"));
                weather(out, true);
                out.add(line("carrying"));
                out.add(Component.empty());
                out.add(heading("eki.malice"));
                bullets(
                        out,
                        List.of(
                                line(
                                        "eki.damage",
                                        percent(rain("damage", RainConfig.DAMAGE.get()))),
                                line("eki.speed", percent(rain("speed", RainConfig.SPEED.get())))));
                out.add(Component.empty());
                out.add(heading("eki.witness"));
                out.add(
                        line(
                                flag("immuneSkip", RainConfig.IMMUNE_SKIP.get())
                                        ? "eki.immune"
                                        : "eki.damage_only",
                                percent(rain("immunity", RainConfig.IMMUNITY.get()))));
            }
            case "tokaido" -> {
                out.add(line("tokaido.intro"));
                weather(out, false);
                var rows = new ArrayList<Component>();
                rows.add(
                        line(
                                "tokaido.heal",
                                number(rain("healSeconds", RainConfig.HEAL_SECONDS.get())),
                                number(rain("heal", RainConfig.HEAL.get()))));
                rows.add(
                        line(
                                "tokaido.food",
                                number(rain("foodSeconds", RainConfig.FOOD_SECONDS.get())),
                                number(rain("food", RainConfig.FOOD.get())),
                                number(rain("saturation", RainConfig.SATURATION.get()))));
                if (flag("fire", RainConfig.FIRE.get())) rows.add(line("tokaido.fire"));
                bullets(out, rows);
                out.add(Component.empty());
                out.add(heading("tokaido.skill"));
                out.add(
                        line(
                                "tokaido.cast",
                                ThemeTooltips.keyName(RainClient.CAST),
                                number(rain("duration", RainConfig.DURATION.get()))));
                out.add(
                        line(
                                "tokaido.cooldown",
                                number(rain("cooldown", RainConfig.COOLDOWN.get()))));
            }
            case "perfect_fluid" -> {
                out.add(line("fluid.0"));
                out.add(line("fluid.1"));
                out.add(Component.empty());
                out.add(heading("fluid.quantity"));
                bullets(out, List.of(line("fluid.2"), line("fluid.3")));
                out.add(Component.empty());
                out.add(heading("fluid.restrictions"));
                out.add(line("fluid.4"));
                out.add(line("fluid.5"));
                var blocked =
                        TooltipConfigSync.pool(
                                "abyss.fluidBlacklist", AbyssItemsConfig.BLACKLIST.get());
                if (!blocked.isEmpty()) {
                    out.add(Component.empty());
                    out.add(heading("fluid.blacklist"));
                    var names = Component.empty();
                    for (String raw : blocked) {
                        if (!names.getSiblings().isEmpty()) names.append(line("separator"));
                        var key = ResourceLocation.tryParse(raw);
                        Component name =
                                key != null && ForgeRegistries.ITEMS.containsKey(key)
                                        ? ForgeRegistries.ITEMS.getValue(key).getDescription()
                                        : Component.literal(raw);
                        names.append(
                                name.copy()
                                        .withStyle(
                                                s ->
                                                        s.withColor(CatalogTextStyle.WARNING)
                                                                .withInsertion(
                                                                        "the_long_travail:blacklist_name")));
                    }
                    names.append(line("period"));
                    out.add(names);
                }
            }
            case "midnight_lafite" -> {
                var rows = new ArrayList<Component>();
                for (String key : List.of("regenerationLevelBonus", "saturationLevelBonus")) {
                    double value = AbyssItemText.value(key);
                    if (value > 0) rows.add(line("lafite." + key, number(value)));
                }
                boolean durations = false;
                for (String key : List.of("poisonDurationMultiplier", "nauseaDurationMultiplier")) {
                    double value = AbyssItemText.value(key);
                    if (value != 1) {
                        rows.add(line("lafite." + key, percent(value)));
                        durations = true;
                    }
                }
                if (rows.isEmpty()) out.add(line("lafite.disabled"));
                else {
                    out.add(line("lafite.intro"));
                    bullets(out, rows);
                }
                if (durations) out.add(line("lafite.infinite"));
                out.add(Component.empty());
                out.add(heading("lafite.malice"));
                double peak = AbyssItemText.value("malicePeak");
                out.add(line("lafite.peak", percent(peak)));
                out.add(
                        line(
                                "lafite.sheltered",
                                percent(peak + AbyssItemText.value("shelteredNonClearExtraPeak"))));
                out.add(Component.empty());
                out.add(heading("lafite.witness"));
                bullets(
                        out,
                        List.of(
                                line("lafite.immunity"),
                                line(
                                        "lafite.bonus",
                                        percent(AbyssItemText.value("witnessBonus")))));
            }
            case "liquefied_abundance" -> {
                out.add(line("abundance.switch"));
                out.add(
                        line(
                                "abundance.use",
                                number(
                                        TooltipConfigSync.decimal(
                                                "abundance.hold",
                                                AbyssItemsConfig.ABUNDANCE_HOLD.get()))));
                out.add(Component.empty());
                out.add(heading("abundance.duration"));
                bullets(
                        out,
                        List.of(
                                line("abundance.sample"),
                                line(
                                        "abundance.fallback",
                                        number(
                                                TooltipConfigSync.decimal(
                                                        "abundance.fallback",
                                                        AbyssItemsConfig.ABUNDANCE_DURATION
                                                                .get())))));
                out.add(line("abundance.lingering"));
                out.add(Component.empty());
                out.add(heading("abundance.result"));
                bullets(out, List.of(line("abundance.last"), line("abundance.more")));
                out.add(line("abundance.empty"));
            }
            default -> throw new IllegalArgumentException(id);
        }
        return out;
    }

    private CatalogAbyssDetails() {}
}

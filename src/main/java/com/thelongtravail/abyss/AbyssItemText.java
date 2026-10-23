package com.thelongtravail.abyss;

import com.thelongtravail.config.AbyssItemsConfig;
import com.thelongtravail.network.TooltipConfigSync;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import java.math.BigDecimal;
import java.util.*;

public final class AbyssItemText {
    private static final String PREFIX = "tooltip.the_long_travail.abyss_items.";

    public static double value(String key) {
        double fallback =
                switch (key) {
                    case "regenerationLevelBonus" -> AbyssItemsConfig.REGENERATION.get();
                    case "saturationLevelBonus" -> AbyssItemsConfig.SATURATION.get();
                    default -> AbyssItemsConfig.get(key);
                };
        return TooltipConfigSync.decimal("abyssItem." + key, fallback);
    }

    private static String n(double number) {
        return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
    }

    private static String percent(double number) {
        return BigDecimal.valueOf(number).movePointRight(2).stripTrailingZeros().toPlainString();
    }

    private static Component raw(String key, Object... args) {
        return Component.translatable(PREFIX + key, args)
                .withStyle(s -> s.withColor(RainTooltips.BODY_COLOR));
    }

    private static Component line(String key, Object... args) {
        return RainTooltips.coloredLine(raw(key, args));
    }

    private static Component sentence(List<Component> parts) {
        var out = Component.empty().withStyle(s -> s.withColor(RainTooltips.BODY_COLOR));
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) out.append(raw("separator"));
            out.append(parts.get(i));
        }
        return RainTooltips.coloredLine(out.append(raw("period")));
    }

    private static List<Component> basics() {
        var out = new ArrayList<Component>();
        var levels = new ArrayList<Component>();
        double regen = value("regenerationLevelBonus"), saturation = value("saturationLevelBonus");
        if (regen > 0) levels.add(raw("regeneration", n(regen)));
        if (saturation > 0)
            levels.add(raw(levels.isEmpty() ? "saturation_first" : "saturation", n(saturation)));
        if (!levels.isEmpty()) out.add(sentence(levels));
        var durations = new ArrayList<Component>();
        double poison = value("poisonDurationMultiplier"),
                nausea = value("nauseaDurationMultiplier");
        if (poison != 1)
            durations.add(raw("poison_" + (poison < 1 ? "shorter" : "longer"), percent(poison)));
        if (nausea != 1)
            durations.add(
                    raw(
                            (durations.isEmpty() ? "nausea_first_" : "nausea_")
                                    + (nausea < 1 ? "shorter" : "longer"),
                            percent(nausea)));
        if (!durations.isEmpty()) {
            out.add(sentence(durations));
            out.add(line("infinite"));
        }
        if (out.isEmpty()) out.add(line("base_disabled"));
        return out;
    }

    public static List<Component> details(boolean lafite) {
        var out = new ArrayList<Component>();
        if (!lafite) {
            out.add(line("fluid.0"));
            out.add(line("fluid.1"));
            out.add(line("fluid.restriction"));
            out.add(Component.empty());
            out.add(line("fluid.2"));
            out.add(line("fluid.3"));
            out.add(Component.empty());
            out.add(line("fluid.4"));
            var blacklist =
                    TooltipConfigSync.pool(
                            "abyss.fluidBlacklist", AbyssItemsConfig.BLACKLIST.get());
            var names = Component.empty();
            for (int i = 0; i < blacklist.size(); i++) {
                if (i > 0) names.append(raw("separator"));
                var id = ResourceLocation.tryParse(blacklist.get(i));
                names.append(
                        (id != null && ForgeRegistries.ITEMS.containsKey(id)
                                        ? ForgeRegistries.ITEMS.getValue(id).getDescription().copy()
                                        : Component.literal(blacklist.get(i)))
                                .withStyle(
                                        style ->
                                                style.withColor(RainTooltips.ACCENT_COLOR)
                                                        .withInsertion(
                                                                "the_long_travail:configured_item")));
            }
            if (blacklist.isEmpty())
                names.append(
                        raw("fluid.blacklist_unrestricted")
                                .copy()
                                .withStyle(
                                        style ->
                                                style.withColor(RainTooltips.ACCENT_COLOR)
                                                        .withInsertion(
                                                                "the_long_travail:configured_item")));
            out.add(raw("fluid.blacklist_current", names));
            return out;
        }
        out.addAll(basics());
        out.add(Component.empty());
        out.add(line("malice_heading"));
        double ordinary = value("malicePeak"), sheltered = value("shelteredNonClearPeak");
        if (ordinary > 0) out.add(line("malice", percent(ordinary)));
        else out.add(line("malice_zero"));
        out.add(line("sheltered", percent(sheltered)));
        out.add(line("dimensions"));
        out.add(Component.empty());
        out.add(line("witness_heading"));
        out.add(line("immunity"));
        if (value("witnessBonus") > 0) out.add(line("witness", percent(value("witnessBonus"))));
        return out;
    }

    public static List<Component> summary() {
        var out = new ArrayList<Component>();
        boolean defaults =
                value("regenerationLevelBonus") > 0
                        && value("saturationLevelBonus") > 0
                        && value("poisonDurationMultiplier") < 1
                        && value("nauseaDurationMultiplier") > 1;
        if (defaults) out.add(line("summary.base"));
        else out.addAll(basics());
        if (value("malicePeak") > 0 && value("shelteredNonClearPeak") >= value("malicePeak"))
            out.add(line("summary.malice"));
        else if (value("malicePeak") > 0 || value("shelteredNonClearPeak") > 0)
            out.add(line("summary.malice_custom"));
        if (value("witnessBonus") > 0) out.add(line("summary.witness"));
        else out.add(line("summary.witness_zero"));
        return out;
    }

    private AbyssItemText() {}
}

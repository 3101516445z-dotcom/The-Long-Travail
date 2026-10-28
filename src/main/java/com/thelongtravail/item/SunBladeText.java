package com.thelongtravail.item;

import com.thelongtravail.config.SunBladeConfig;
import com.thelongtravail.network.TooltipConfigSync;
import com.thelongtravail.underworld.UnderworldRelicText;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class SunBladeText {
    private static String number(String key, double scale) {
        return UnderworldRelicText.number(
                TooltipConfigSync.decimal("sunBlade." + key, SunBladeConfig.value(key)) * scale);
    }

    public static List<Component> details(boolean sun) {
        String id = sun ? "sun_chaser" : "kagotsurube";
        String prefix = "tooltip.the_long_travail." + id + ".";
        var out = new ArrayList<Component>();
        if (sun) {
            out.add(Component.translatable(prefix + "base"));
            out.add(Component.translatable(prefix + "healing", number(id + ".healing", 1)));
            out.add(Component.translatable(prefix + "lava"));
        } else {
            out.add(Component.translatable(prefix + "base", number(id + ".witherSeconds", 1)));
            out.add(Component.translatable(prefix + "extend", number(id + ".witherSeconds", 1)));
        }
        out.add(Component.empty());
        out.add(
                Component.translatable(prefix + "malice_heading")
                        .withStyle(s -> s.withColor(0xC9B8FF)));
        if (sun) {
            out.add(Component.translatable(prefix + "reflect"));
            out.add(
                    Component.translatable(
                            prefix + "projectile", number(id + ".projectileBonus", 100)));
            out.add(Component.translatable(prefix + "burning", number(id + ".burningBonus", 100)));
        } else {
            out.add(
                    Component.translatable(
                            prefix + "malice",
                            number(id + ".insight", 1),
                            number(id + ".overlord", 1)));
        }
        out.add(Component.empty());
        out.add(
                Component.translatable(prefix + "witness_heading")
                        .withStyle(s -> s.withColor(0xC9B8FF)));
        if (sun) {
            out.add(
                    Component.translatable(
                            prefix + "suppression", number(id + ".suppressionSeconds", 1)));
            out.add(Component.translatable(prefix + "strike", number(id + ".strikeFraction", 100)));
        } else {
            out.add(Component.translatable(prefix + "witness"));
        }
        return out;
    }

    private SunBladeText() {}
}

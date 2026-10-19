package com.thelongtravail.underworld;

import com.thelongtravail.config.UnderworldItemsConfig;
import com.thelongtravail.network.TooltipConfigSync;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class UnderworldRelicText {
    public static double shown(String key) {
        return TooltipConfigSync.decimal(
                "underworldRelic." + key,
                key.equals("banner.capacity")
                        ? UnderworldItemsConfig.SOUL_CAPACITY.get()
                        : UnderworldItemsConfig.RELICS.get(key).get());
    }

    public static String number(double x) {
        return java.math.BigDecimal.valueOf(x)
                .setScale(6, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static String pct(String key) {
        return number(shown(key) * 100);
    }

    public static List<Component> details(boolean banner) {
        String p = "tooltip.the_long_travail.relic.";
        var out = new ArrayList<Component>();
        if (banner) {
            out.add(Component.translatable(p + "banner.base", number(shown("banner.capacity"))));
            out.add(Component.translatable(p + "banner.persistence"));
            out.add(Component.empty());
        }
        out.add(
                Component.translatable("tooltip.the_long_travail.thousand_years.malice_title")
                        .withStyle(s -> s.withColor(0xC9B8FF)));
        if (banner) out.add(Component.translatable(p + "banner.malice", pct("banner.maliceCriticalPerSoul")));
        else {
            out.add(Component.translatable(p + "king.burning", pct("king.burningBonus")));
            out.add(Component.translatable(p + "king.heal", pct("king.lifesteal")));
            out.add(Component.translatable(p + "king.undead", pct("king.undeadReduction")));
        }
        out.add(Component.empty());
        out.add(
                Component.translatable("tooltip.the_long_travail.thousand_years.witness_title")
                        .withStyle(s -> s.withColor(0xC9B8FF)));
        if (banner) {
            out.add(
                    Component.translatable(
                            p + "banner.insight",
                            number(shown("banner.insightPerSoul")),
                            number(shown("banner.insightCap"))));
            out.add(Component.translatable(p + "banner.witness", pct("banner.witnessCriticalPerSoul")));
        } else {
            out.add(Component.translatable(p + "king.overlord", number(shown("king.overlord"))));
            out.add(Component.translatable(p + "king.environment", pct("king.environmentBonus")));
        }
        if (banner) {
            out.add(Component.empty());
            out.add(Component.translatable(p + "banner.credit"));
            out.add(Component.translatable(p + "banner.boundaries"));
        }
        return out;
    }

    private UnderworldRelicText() {}
}

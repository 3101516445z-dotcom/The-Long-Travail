package com.thelongtravail.farreach;

import com.thelongtravail.abyss.RainTooltips;
import com.thelongtravail.config.FarReachRelicsConfig;
import com.thelongtravail.network.TooltipConfigSync;
import net.minecraft.network.chat.Component;
import java.math.BigDecimal;
import java.util.List;

public final class FarRelicText {
    private static String n(String key, double scale) {
        return BigDecimal.valueOf(TooltipConfigSync.decimal("farItem." + key, FarReachRelicsConfig.value(key)) * scale)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static void line(List<Component> out, String key, Object... args) {
        out.add(
                RainTooltips.coloredLine(
                        Component.translatable("tooltip.the_long_travail." + key, args)
                                .withStyle(
                                        s ->
                                                s.withColor(
                                                        key.endsWith("heading")
                                                                ? RainTooltips.TITLE_COLOR
                                                                : RainTooltips.BODY_COLOR))));
    }

    public static void details(List<Component> out, boolean quixote) {
        if (quixote) {
            line(out, "don_quixote.base", n("don_quixote.maxBonus", 100));
            line(out, "don_quixote.size", n("don_quixote.sizeFactor", 100));
            out.add(Component.empty());
            line(out, "golden_age.malice_heading");
            line(out, "don_quixote.malice", n("don_quixote.healthRatio", 1), n("don_quixote.trueFraction", 100));
            out.add(Component.empty());
            line(out, "golden_age.witness_heading");
            line(
                    out,
                    "don_quixote.witness",
                    n("don_quixote.witnessThreshold", 100),
                    n("don_quixote.reduction", 100),
                    n("don_quixote.duration", 1));
            line(out, "don_quixote.cooldown", n("don_quixote.cooldown", 1));
        } else {
            line(out, "tower_of_babel.base");
            line(out, "tower_of_babel.provocation");
            out.add(Component.empty());
            line(out, "golden_age.malice_heading");
            line(out, "tower_of_babel.malice", n("babel.priceFactor", 100));
            line(
                    out,
                    "tower_of_babel.restock",
                    n("babel.restockRange", 1),
                    n("babel.restockSeconds", 1),
                    n("babel.restocksPerDay", 1));
            line(out, "tower_of_babel.injured");
            out.add(Component.empty());
            line(out, "tower_of_babel.limit");
        }
    }

    private FarRelicText() {}
}

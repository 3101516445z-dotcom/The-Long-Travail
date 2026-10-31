package com.thelongtravail.boundless;

import com.thelongtravail.abyss.RainTooltips;
import com.thelongtravail.config.BoundlessItemsConfig;
import com.thelongtravail.network.TooltipConfigSync;
import net.minecraft.network.chat.Component;
import java.math.BigDecimal;
import java.util.*;

public final class BoundlessRelicText {
    private static String number(String key, double scale) {
        return BigDecimal.valueOf(
                        TooltipConfigSync.decimal(
                                        "boundlessItem." + key, BoundlessItemsConfig.get(key))
                                * scale)
                .stripTrailingZeros()
                .toPlainString();
    }

    public static String dreamDuration() {
        return number("dream.duration", 1);
    }

    public static Component princeHeight() {
        String height = number("prince.stepHeight", 1);
        return height.equals("1")
                ? Component.translatable("tooltip.the_long_travail.little_prince.one")
                : Component.translatable("tooltip.the_long_travail.little_prince.height", height);
    }

    public static Component line(String key, Object... args) {
        return RainTooltips.coloredLine(
                Component.translatable("tooltip.the_long_travail." + key, args)
                        .withStyle(style -> style.withColor(RainTooltips.BODY_COLOR)));
    }

    public static Component toggleMessage(boolean enabled) {
        return RainTooltips.coloredLine(
                Component.translatable(
                                "message.the_long_travail.lorentz." + (enabled ? "on" : "off"))
                        .withStyle(style -> style.withColor(RainTooltips.BODY_COLOR)));
    }

    public static List<Component> details(boolean prince) {
        List<Component> out = new ArrayList<>();
        if (prince) {
            out.add(line("little_prince.base", princeHeight()));
            out.add(Component.empty());
            out.add(line("little_prince.limit"));
        } else {
            out.add(line("lorentz.base", number("lorentz.pierce", 1)));
            out.add(Component.empty());
            out.add(line("lorentz.toggle"));
            out.add(Component.empty());
            out.add(line("lorentz.malice_heading"));
            out.add(Component.empty());
            out.add(line("lorentz.critical", number("lorentz.critical", 100)));
            out.add(Component.empty());
            out.add(
                    line(
                            "lorentz.damage",
                            number("lorentz.damage", 100),
                            number("lorentz.headshot", 100)));
            out.add(Component.empty());
            out.add(line("lorentz.witness_heading"));
            out.add(Component.empty());
            out.add(line("lorentz.witness", number("lorentz.voidRatio", 100)));
            out.add(Component.empty());
            out.add(line("lorentz.interval", number("lorentz.interval", 1)));
        }
        return List.copyOf(out);
    }

    private BoundlessRelicText() {}
}

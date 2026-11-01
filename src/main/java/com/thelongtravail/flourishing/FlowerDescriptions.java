package com.thelongtravail.flourishing;

import com.thelongtravail.config.FlourishingItemsConfig;
import net.minecraft.network.chat.Component;
import java.math.BigDecimal;

public final class FlowerDescriptions {
    public static String effectLevel(int level) {
        if (level < 1) return Integer.toString(level);
        if (level <= 12)
            return new String[] {"Ⅰ", "Ⅱ", "Ⅲ", "Ⅳ", "Ⅴ", "Ⅵ", "Ⅶ", "Ⅷ", "Ⅸ", "Ⅹ", "Ⅺ", "Ⅻ"}
                    [level - 1];
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"Ⅿ", "ⅭⅯ", "Ⅾ", "ⅭⅮ", "Ⅽ", "ⅩⅭ", "Ⅼ", "ⅩⅬ", "Ⅹ", "ⅠⅩ", "Ⅴ", "ⅠⅤ", "Ⅰ"};
        var result = new StringBuilder();
        for (int i = 0; i < values.length; i++)
            while (level >= values[i]) {
                result.append(symbols[i]);
                level -= values[i];
            }
        return result.toString();
    }

    public static String number(double n) {
        return BigDecimal.valueOf(n)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    public static String percent(double n) {
        return number(n * 100);
    }

    public static Component describe(Flower f) {
        return describe(f, "tooltip.the_long_travail.flower." + f.id);
    }

    public static Component describe(Flower f, String key) {
        String root = "flowers." + f.id + ".";
        double v =
                FlourishingItemsConfig.shown(
                        root
                                + (f == Flower.OXEYE_DAISY
                                        ? "healing"
                                        : f == Flower.LILY_OF_THE_VALLEY || f == Flower.WITHER_ROSE
                                                ? "level"
                                                : "bonus"));
        return switch (f) {
            case CHORUS_FLOWER, FLOWERING_AZALEA, FLOWERING_AZALEA_LEAVES ->
                    FloralText.text(key, number(v));
            case DANDELION ->
                    FloralText.text(
                            key,
                            percent(v),
                            percent(FlourishingItemsConfig.shown(root + "saturationBonus")));
            case RED_TULIP, WHITE_TULIP ->
                    FloralText.text(
                            key,
                            percent(FlourishingItemsConfig.shown(root + "healthThreshold")),
                            percent(v));
            case OXEYE_DAISY ->
                    FloralText.text(
                            key,
                            number(FlourishingItemsConfig.shown(root + "intervalSeconds")),
                            number(v));
            case LILY_OF_THE_VALLEY, WITHER_ROSE ->
                    FloralText.text(
                            key,
                            key.startsWith("gui.the_long_travail.catalog.")
                                    ? effectLevel((int) Math.floor(v))
                                    : number(Math.floor(v)),
                            number(FlourishingItemsConfig.shown(root + "durationSeconds")),
                            number(FlourishingItemsConfig.shown(root + "cooldownSeconds")));
            case SUNFLOWER ->
                    FloralText.text(
                            key,
                            percent(FlourishingItemsConfig.shown(root + "speedBonus")),
                            percent(v));
            case ROSE_BUSH, PITCHER_PLANT ->
                    FloralText.text(
                            key,
                            percent(v),
                            number(FlourishingItemsConfig.shown(root + "cooldownSeconds")));
            default -> FloralText.text(key, percent(v));
        };
    }
}

package com.thelongtravail.underworld;

import com.thelongtravail.config.UnderworldItemsConfig;
import com.thelongtravail.network.TooltipConfigSync;

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

    private UnderworldRelicText() {}
}

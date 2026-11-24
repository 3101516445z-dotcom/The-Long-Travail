package com.thelongtravail.text;

import java.math.BigDecimal;

public final class DisplayNumbers {
    private DisplayNumbers() {}

    public static String number(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    public static String percent(double value) {
        return BigDecimal.valueOf(value).movePointRight(2).stripTrailingZeros().toPlainString();
    }
}

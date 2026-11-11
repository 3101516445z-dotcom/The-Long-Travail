package com.thelongtravail.flourishing;

import com.thelongtravail.text.TooltipStyle;

import net.minecraft.network.chat.Component;

public final class FloralText {
    public static final int BODY = TooltipStyle.BODY_COLOR,
            TITLE = TooltipStyle.TITLE_COLOR,
            VALUE = TooltipStyle.ACCENT_COLOR;

    public static Component text(String key, Object... args) {
        return TooltipStyle.coloredLine(
                Component.translatable(key, args).withStyle(s -> s.withColor(BODY)));
    }

    public static Component gold(Component text) {
        return text.copy().withStyle(s -> s.withColor(TITLE));
    }

    private FloralText() {}
}

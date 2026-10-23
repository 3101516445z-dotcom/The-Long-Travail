package com.thelongtravail.flourishing;

import com.thelongtravail.abyss.RainTooltips;
import net.minecraft.network.chat.Component;

public final class FloralText {
    public static final int BODY = RainTooltips.BODY_COLOR,
            TITLE = RainTooltips.TITLE_COLOR,
            VALUE = RainTooltips.ACCENT_COLOR;

    public static Component text(String key, Object... args) {
        return RainTooltips.coloredLine(
                Component.translatable(key, args).withStyle(s -> s.withColor(BODY)));
    }

    public static Component gold(Component text) {
        return text.copy().withStyle(s -> s.withColor(TITLE));
    }

    private FloralText() {}
}

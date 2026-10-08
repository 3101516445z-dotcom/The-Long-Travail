package com.thelongtravail.flourishing;

import com.thelongtravail.abyss.RainTooltips;
import net.minecraft.network.chat.Component;

// 复用约定色板；翻译参数先完成拼接，使数值、百分号和单位保持连续颜色。
public final class FloralText {
    public static final int BODY=RainTooltips.BODY_COLOR, TITLE=RainTooltips.TITLE_COLOR, VALUE=RainTooltips.ACCENT_COLOR;
    public static Component text(String key,Object...args) {
        return RainTooltips.coloredLine(Component.translatable(key,args).withStyle(s->s.withColor(BODY)));
    }
    public static Component gold(Component text){return text.copy().withStyle(s->s.withColor(TITLE));}
    private FloralText() {}
}

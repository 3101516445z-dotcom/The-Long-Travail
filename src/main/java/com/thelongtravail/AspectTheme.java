package com.thelongtravail;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

// 日记配色与物品名称材质共用主题定义，可在独立服务端安全引用。
public enum AspectTheme {
    FLOURISHING(0x487846, 0x355B32),
    ABYSS(0x397F8A, 0x285C65),
    FAR_REACH(0xBDB46A, 0x706A3D, 0x77715A, 0xBDB46A, 0xEAE8CF),
    DEEP_VALLEY(0x7A8085, 0x50565C),
    UNDERWORLD(0xA14A3E, 0x81352D),
    BOUNDLESS(0x805091, 0x654073);

    public final int bookAccent, bookInk, dark, main, highlight;
    private final String marker;
    private static final AspectTheme[] THEMES = values();
    AspectTheme(int accent, int ink) {
        this(accent, ink, mix(accent, 0x000000, .25F), mix(accent, 0xFFFFFF, .28F), mix(accent, 0xFFFFFF, .72F));
    }
    AspectTheme(int accent, int ink, int dark, int main, int highlight) {
        this.bookAccent = accent; this.bookInk = ink;
        this.dark = dark; this.main = main; this.highlight = highlight;
        this.marker = "the_long_travail:aspect_name:" + name().toLowerCase(java.util.Locale.ROOT);
    }
    public int material() { return 3 + ordinal(); }
    public MutableComponent name(Component base) {
        return base.copy().withStyle(style -> style.withColor(main).withInsertion(marker));
    }
    public static AspectTheme forAspect(TravailAspect aspect) { return valueOf(aspect.name()); }
    public static AspectTheme fromMarker(String marker) {
        if (marker == null) return null;
        for (var theme : THEMES) if (theme.marker.equals(marker)) return theme;
        return null;
    }
    private static int mix(int from, int to, float amount) {
        int result = 0;
        for (int shift : new int[]{16, 8, 0}) {
            int a = (from >> shift) & 255, b = (to >> shift) & 255;
            result |= Math.round(a + (b-a)*amount) << shift;
        }
        return result;
    }
}

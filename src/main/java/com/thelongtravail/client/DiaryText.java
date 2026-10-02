package com.thelongtravail.client;

import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import java.util.ArrayList;

/** 仅移除材质标记，保留文本视觉顺序、索引和样式。 */
public final class DiaryText {
    public interface Unmarked extends FormattedCharSequence {}

    public static FormattedCharSequence lazyUnmarked(FormattedCharSequence text) {
        return (Unmarked) sink -> text.accept((index, style, codePoint) ->
                sink.accept(index, style.withInsertion(null), codePoint));
    }

    /** 名称需要绘制 17 遍，源文本遍历和样式转换只执行一次。 */
    public static FormattedCharSequence prepare(FormattedCharSequence text) {
        var glyphs = new ArrayList<Glyph>();
        text.accept(new FormattedCharSink() {
            private Style previous, clean;
            public boolean accept(int index, Style style, int codePoint) {
                if (style != previous) { previous = style; clean = style.withInsertion(null); }
                glyphs.add(new Glyph(index, clean, codePoint));
                return true;
            }
        });
        return new Prepared(glyphs.toArray(Glyph[]::new));
    }

    private record Glyph(int index, Style style, int codePoint) {}
    private record Prepared(Glyph[] glyphs) implements Unmarked {
        public boolean accept(FormattedCharSink sink) {
            for (Glyph glyph : glyphs) if (!sink.accept(glyph.index(), glyph.style(), glyph.codePoint())) return false;
            return true;
        }
    }
    private DiaryText() {}
}

package com.thelongtravail.client;

import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import java.util.ArrayList;

/** Draw-scoped text: retain visual order, indices and styles, removing only the material marker. */
public final class DiaryText {
    public interface Unmarked extends FormattedCharSequence {}

    public static FormattedCharSequence lazyUnmarked(FormattedCharSequence text) {
        return (Unmarked) sink -> text.accept((index, style, codePoint) ->
                sink.accept(index, style.withInsertion(null), codePoint));
    }

    /** Used for the 17 name passes, so source traversal and style conversion happen only once. */
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

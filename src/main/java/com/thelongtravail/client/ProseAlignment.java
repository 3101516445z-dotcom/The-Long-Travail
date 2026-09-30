package com.thelongtravail.client;

import java.util.function.ToIntFunction;

/** Optical centering: lightly hang edge punctuation while retaining the original text. */
public final class ProseAlignment {
    private ProseAlignment() {}

    public static float offset(String text, ToIntFunction<String> width) {
        int start = 0, end = text.length();
        while (start < end && punctuation(text.codePointAt(start)))
            start += Character.charCount(text.codePointAt(start));
        if (start == end) return 0;
        while (end > start && punctuation(text.codePointBefore(end)))
            end -= Character.charCount(text.codePointBefore(end));
        float cap = width.applyAsInt("。");
        float leading = Math.min(cap, width.applyAsInt(text.substring(0, start)));
        float trailing = Math.min(cap, width.applyAsInt(text.substring(end)));
        return (trailing - leading) * 0.65F / 2F;
    }

    private static boolean punctuation(int cp) {
        return "，。！？；：、…“”‘’（）《》〈〉【】「」『』,.!?;:\"'()[]".indexOf(cp) >= 0;
    }
}

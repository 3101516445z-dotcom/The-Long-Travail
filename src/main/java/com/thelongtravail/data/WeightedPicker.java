package com.thelongtravail.data;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

public final class WeightedPicker {
    public static <T> T one(List<T> entries, ToIntFunction<T> weight, RandomSource random) {
        long total = 0L;
        for (T entry : entries) total += Math.max(0, weight.applyAsInt(entry));
        if (total <= 0L) return null;
        long roll = Math.floorMod(random.nextLong(), total);
        for (T entry : entries) {
            roll -= Math.max(0, weight.applyAsInt(entry));
            if (roll < 0L) return entry;
        }
        return entries.get(entries.size() - 1);
    }

    public static <T> List<T> withoutReplacement(List<T> source, int count, ToIntFunction<T> weight, RandomSource random) {
        List<T> candidates = new ArrayList<>(source);
        List<T> result = new ArrayList<>();
        while (!candidates.isEmpty() && result.size() < count) {
            T selected = one(candidates, weight, random);
            if (selected == null) break;
            result.add(selected);
            candidates.remove(selected);
        }
        return result;
    }

    private WeightedPicker() {}
}

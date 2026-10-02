package com.thelongtravail.data;
import java.util.List;
import java.util.function.ToIntFunction;
import net.minecraft.util.RandomSource;
public final class WeightedTable<T> {
    private final List<T> entries;
    private final long[] cumulative;
    private final long total;
    public WeightedTable(List<T> source, ToIntFunction<T> weight) {
        entries = List.copyOf(source); cumulative = new long[entries.size()];
        long sum = 0;
        for (int i = 0; i < entries.size(); i++) cumulative[i] = sum += Math.max(0, weight.applyAsInt(entries.get(i)));
        total = sum;
    }
    public List<T> entries() { return entries; }
    public T choose(RandomSource random) {
        if (total == 0) return null;
        long roll = Math.floorMod(random.nextLong(), total);
        int low = 0, high = cumulative.length - 1;
        while (low < high) { int mid = (low + high) >>> 1;
            if (cumulative[mid] > roll) high = mid; else low = mid + 1;
        }
        return entries.get(low);
    }
}

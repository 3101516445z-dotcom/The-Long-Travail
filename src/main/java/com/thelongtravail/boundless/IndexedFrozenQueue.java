package com.thelongtravail.boundless;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import com.thelongtravail.data.HotPathMetrics.Counter;

// 区块仅用于筛选；恢复仍按原 LinkedHashMap 的首次插入顺序执行。
final class IndexedFrozenQueue<K, V> {
    private record Entry<V>(long order, V value, Set<Long> chunks) {}
    private final Map<K, Entry<V>> entries = new LinkedHashMap<>();
    private final Map<Long, Set<K>> chunks = new HashMap<>();
    private final Set<K> pending = new HashSet<>();
    private final Function<V, Set<Long>> coverage;
    private long nextOrder;

    IndexedFrozenQueue(Function<V, Set<Long>> coverage) { this.coverage = coverage; }
    boolean containsKey(K key) { return entries.containsKey(key); }
    boolean isEmpty() { return entries.isEmpty(); }
    Collection<V> values() { return entries.values().stream().map(Entry::value).toList(); }
    void putIfAbsent(K key, V value) { if (!entries.containsKey(key)) put(key, value); }
    void put(K key, V value) {
        Entry<V> old = entries.get(key);
        if (old != null) unindex(key, old);
        Entry<V> entry = new Entry<>(old == null ? nextOrder++ : old.order(), value, Set.copyOf(coverage.apply(value)));
        entries.put(key, entry);
        for (long chunk : entry.chunks()) chunks.computeIfAbsent(chunk, ignored -> new HashSet<>()).add(key);
        pending.add(key);
    }
    void changed(Collection<Long> changed) {
        for (long chunk : changed) pending.addAll(chunks.getOrDefault(chunk, Set.of()));
    }
    private void unindex(K key, Entry<V> entry) {
        for (long chunk : entry.chunks()) {
            Set<K> keys = chunks.get(chunk);
            keys.remove(key);
            if (keys.isEmpty()) chunks.remove(chunk);
        }
    }
    List<V> release(Predicate<V> ready) {
        if (pending.isEmpty()) return List.of();
        List<K> candidates = new ArrayList<>(pending);
        pending.clear();
        candidates.sort(Comparator.comparingLong(key -> entries.get(key).order()));
        List<V> result = new ArrayList<>();
        for (K key : candidates) {
            Entry<V> entry = entries.get(key);
            Counter.RELEASE_CANDIDATES.add(1);
            if (!ready.test(entry.value())) continue;
            entries.remove(key);
            unindex(key, entry);
            result.add(entry.value());
        }
        return result;
    }
}

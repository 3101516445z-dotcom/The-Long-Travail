package com.thelongtravail.boundless;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import com.thelongtravail.data.HotPathMetrics.Counter;
import com.thelongtravail.data.HotPathMetrics.Peak;

// 区块仅用于筛选；恢复仍按原 LinkedHashMap 的首次插入顺序执行。
final class IndexedFrozenQueue<K, V> {
    private record Entry<V>(long order, V value, Set<Long> chunks) {}

    private final Map<K, Entry<V>> entries = new LinkedHashMap<>();
    private final Map<Long, Set<K>> chunks = new HashMap<>();
    private final Set<K> pending = new HashSet<>();
    private final Function<V, Set<Long>> coverage;
    private long nextOrder;

    IndexedFrozenQueue(Function<V, Set<Long>> coverage) {
        this.coverage = coverage;
    }

    boolean containsKey(K key) {
        return entries.containsKey(key);
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }

    Collection<V> values() {
        return entries.values().stream().map(Entry::value).toList();
    }

    void putIfAbsent(K key, V value) {
        if (!entries.containsKey(key)) put(key, value);
    }

    void put(K key, V value) {
        Entry<V> old = entries.get(key);
        Set<Long> covered = Set.copyOf(coverage.apply(value));
        boolean sameCoverage = old != null && old.chunks().equals(covered);
        if (old != null && !sameCoverage) unindex(key, old);
        Entry<V> entry =
                new Entry<>(
                        old == null ? nextOrder++ : old.order(),
                        value,
                        sameCoverage ? old.chunks() : covered);
        entries.put(key, entry);
        // 覆盖未变时保留区块索引；值、首次插入顺序和待检查状态仍按原规则更新。
        if (!sameCoverage)
            for (long chunk : entry.chunks())
                chunks.computeIfAbsent(chunk, ignored -> new HashSet<>()).add(key);
        pending.add(key);
        Peak.FROZEN_QUEUE_SIZE.record(entries.size());
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
        List<K> candidates;
        // 大量记录发生变化时，直接按首次插入顺序遍历，避免维护庞大的候选集合；恢复仍在本次调用内完成。
        if ((long) pending.size() * 2 >= entries.size()) {
            candidates = new ArrayList<>(pending.size());
            for (K key : entries.keySet()) if (pending.contains(key)) candidates.add(key);
            Counter.RELEASE_DENSE_BATCHES.add(1);
        } else {
            candidates = new ArrayList<>(pending);
            candidates.sort(Comparator.comparingLong(key -> entries.get(key).order()));
            Counter.RELEASE_SPARSE_BATCHES.add(1);
        }
        Peak.RELEASE_CANDIDATES_PER_BATCH.record(candidates.size());
        pending.clear();
        List<V> result = new ArrayList<>();
        for (K key : candidates) {
            Entry<V> entry = entries.get(key);
            Counter.RELEASE_CANDIDATES.add(1);
            if (!ready.test(entry.value())) continue;
            entries.remove(key);
            unindex(key, entry);
            result.add(entry.value());
        }
        Peak.RELEASED_PER_BATCH.record(result.size());
        return result;
    }
}

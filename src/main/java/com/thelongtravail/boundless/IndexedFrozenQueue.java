package com.thelongtravail.boundless;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import com.thelongtravail.data.HotPathMetrics.Counter;
import com.thelongtravail.data.HotPathMetrics.Peak;

final class IndexedFrozenQueue<K, V> {
    private record Entry<V>(long order, V value, Set<Long> chunks) {}

    private final Map<K, Entry<V>> entries = new LinkedHashMap<>();
    private final Map<Long, Set<K>> chunks = new HashMap<>();
    private final Set<K> pending = new HashSet<>();
    private final Function<V, Set<Long>> coverage;
    private long nextOrder;
    private final java.util.function.BinaryOperator<V> recoveryMerge;

    IndexedFrozenQueue(
            Function<V, Set<Long>> coverage, java.util.function.BinaryOperator<V> recoveryMerge) {
        this.coverage = coverage;
        this.recoveryMerge = recoveryMerge;
    }

    final class Batch extends AbstractList<V> {
        private final List<Map.Entry<K, Entry<V>>> rows = new ArrayList<>();
        private int next;

        @Override
        public int size() {
            return rows.size();
        }

        @Override
        public V get(int index) {
            return rows.get(index).getValue().value();
        }

        // 先将游标移过当前任务；任务失败前可能已经产生副作用，因此不能自动重试。
        void execute(java.util.function.Consumer<V> action) {
            try {
                while (next < rows.size()) action.accept(get(next++));
            } catch (RuntimeException | Error failure) {
                int failed = next - 1, remaining = rows.size() - next;
                try {
                    restore(next);
                } catch (RuntimeException | Error recovery) {
                    failure.addSuppressed(recovery);
                }
                com.thelongtravail.TheLongTravail.LOGGER.warn(
                        "Frozen update failed at batch index {}; retaining {} unstarted updates",
                        failed,
                        remaining);
                throw failure;
            }
        }

        private void restore(int from) {
            for (int i = from; i < rows.size(); i++) {
                var row = rows.get(i);
                K key = row.getKey();
                Entry<V> old = row.getValue(), newer = entries.get(key);
                V value =
                        newer == null
                                ? old.value()
                                : recoveryMerge.apply(old.value(), newer.value());
                if (newer != null) unindex(key, newer);
                var restored = new Entry<>(old.order(), value, Set.copyOf(coverage.apply(value)));
                entries.put(key, restored);
                for (long chunk : restored.chunks())
                    chunks.computeIfAbsent(chunk, ignored -> new HashSet<>()).add(key);
                pending.add(key);
            }
            var ordered = new ArrayList<>(entries.entrySet());
            ordered.sort(Comparator.comparingLong(row -> row.getValue().order()));
            entries.clear();
            ordered.forEach(row -> entries.put(row.getKey(), row.getValue()));
            next = rows.size();
        }
    }

    IndexedFrozenQueue(Function<V, Set<Long>> coverage) {
        this(coverage, (old, newer) -> newer);
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

    Batch release(Predicate<V> ready) {
        Batch result = new Batch();
        if (pending.isEmpty()) return result;
        List<K> candidates;
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
        try {
            for (K key : candidates) {
                Entry<V> entry = entries.get(key);
                Counter.RELEASE_CANDIDATES.add(1);
                if (!ready.test(entry.value())) continue;
                entries.remove(key);
                unindex(key, entry);
                result.rows.add(Map.entry(key, entry));
            }
        } catch (RuntimeException | Error failure) {
            // 筛选失败时尚未执行外部更新，因此可以恢复所有已取出的记录。
            result.restore(0);
            for (K key : candidates) if (entries.containsKey(key)) pending.add(key);
            throw failure;
        }
        Peak.RELEASED_PER_BATCH.record(result.size());
        return result;
    }
}

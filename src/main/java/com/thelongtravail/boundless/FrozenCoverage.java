package com.thelongtravail.boundless;

import java.util.*;

// 比较覆盖该区块的区域身份，而非只比较是否覆盖，确保重叠区域结束时重新判断。
final class FrozenCoverage {
    private TimeStopManager manager;
    private long revision = Long.MIN_VALUE;
    private Map<Long, Set<Long>> previous = Map.of();
    Set<Long> changed(TimeStopManager current) {
        if (manager == current && revision == current.revision()) return Set.of();
        Map<Long, Set<Long>> next = new HashMap<>();
        for (var field : TimeStopManager.fields(current.level))
            for (long chunk : field.chunks) next.computeIfAbsent(chunk, ignored -> new HashSet<>()).add(field.id);
        Set<Long> changed = new HashSet<>(previous.keySet());
        changed.addAll(next.keySet());
        if (manager == current) changed.removeIf(chunk -> Objects.equals(previous.get(chunk), next.get(chunk)));
        manager = current;
        revision = current.revision();
        previous = next;
        return changed;
    }
}

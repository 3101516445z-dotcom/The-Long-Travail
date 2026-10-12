package com.thelongtravail.client;

import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import java.util.*;

// 调度状态仅由渲染线程维护，不持有客户端世界引用。
public final class LanternRebuildScheduler {
    public static final long INTERVAL_NANOS = 33_000_000L;

    public record Batch(LanternLightSnapshot snapshot, Set<Long> dirtySections) {}

    private Map<UUID, LanternLighting.Source> previous = Map.of();
    private LanternLightSnapshot snapshot = LanternLightSnapshot.EMPTY;
    private final Map<UUID, Footprint> footprints = new HashMap<>();
    private long lastRebuild;
    private boolean published;

    private record SectionBounds(int x0, int x1, int y0, int y1, int z0, int z1) {
        static SectionBounds of(LanternLighting.Source source) {
            double r = LanternLighting.RADIUS + 1;
            return new SectionBounds(
                    Mth.floor(source.x() - r) >> 4,
                    Mth.floor(source.x() + r) >> 4,
                    Mth.floor(source.y() - r) >> 4,
                    Mth.floor(source.y() + r) >> 4,
                    Mth.floor(source.z() - r) >> 4,
                    Mth.floor(source.z() + r) >> 4);
        }

        Set<Long> sections() {
            Set<Long> result = new HashSet<>();
            for (int x = x0; x <= x1; x++)
                for (int y = y0; y <= y1; y++)
                    for (int z = z0; z <= z1; z++) result.add(SectionPos.asLong(x, y, z));
            return Set.copyOf(result);
        }
    }

    private record Footprint(SectionBounds bounds, Set<Long> sections) {}

    public void clear() {
        previous = Map.of();
        snapshot = LanternLightSnapshot.EMPTY;
        footprints.clear();
        lastRebuild = 0;
        published = false;
    }

    public Batch update(long now, Map<UUID, LanternLighting.Source> current) {
        // 出现、消失与移动共用提交间隔；跳过的变化不延长截止时间，避免连续变化导致一直无法提交。
        if (published && now - lastRebuild < INTERVAL_NANOS) return null;
        boolean membership = !previous.keySet().equals(current.keySet());
        boolean moved = membership;
        if (!moved)
            for (var entry : current.entrySet()) {
                var a = entry.getValue();
                var b = previous.get(entry.getKey());
                if (Math.abs(a.x() - b.x()) > .08
                        || Math.abs(a.y() - b.y()) > .08
                        || Math.abs(a.z() - b.z()) > .08) {
                    moved = true;
                    break;
                }
            }
        if (!moved) return null;
        Set<Long> dirty = new HashSet<>();
        int changedSources = 0;
        // 始终清理上次实际发布的范围；中途出现又消失但未发布的光源无需重建。
        for (UUID id : previous.keySet())
            if (!Objects.equals(previous.get(id), current.get(id))) {
                changedSources++;
                Footprint old = footprints.get(id);
                if (old != null) dirty.addAll(old.sections());
            }
        for (UUID id : current.keySet())
            if (!Objects.equals(previous.get(id), current.get(id))) {
                if (!previous.containsKey(id)) changedSources++;
                SectionBounds bounds = SectionBounds.of(current.get(id));
                Footprint footprint = footprints.get(id);
                if (footprint == null || !footprint.bounds().equals(bounds)) {
                    footprint = new Footprint(bounds, bounds.sections());
                    footprints.put(id, footprint);
                }
                dirty.addAll(footprint.sections());
            }
        var sources = current.values().toArray(LanternLighting.Source[]::new);
        var nextSnapshot =
                changedSources < Math.max(previous.size(), current.size()) / 4
                        ? LanternLightSnapshot.update(snapshot, sources)
                        : new LanternLightSnapshot(sources);
        footprints.keySet().retainAll(current.keySet());
        previous = Map.copyOf(current);
        snapshot = nextSnapshot;
        lastRebuild = now;
        published = true;
        return new Batch(snapshot, Set.copyOf(dirty));
    }
}

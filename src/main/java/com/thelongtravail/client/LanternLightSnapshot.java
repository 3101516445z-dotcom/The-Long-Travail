package com.thelongtravail.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.SectionPos;
import java.util.*;

// 快照构建后只读，可供网格构建线程安全使用；查询时按实际采样位置选择区段。
public final class LanternLightSnapshot {
    public static final LanternLightSnapshot EMPTY =
            new LanternLightSnapshot(new LanternLighting.Source[0]);
    private static final LanternLighting.Source[] NONE = new LanternLighting.Source[0];
    private final LanternLighting.Source[] all;
    private final Set<LanternLighting.Source> unique;
    private final Long2ObjectOpenHashMap<LanternLighting.Source[]> sections;

    public LanternLightSnapshot(LanternLighting.Source[] sources) {
        all = sources.clone();
        unique = new LinkedHashSet<>(Arrays.asList(all));
        sections = build(unique);
    }

    private LanternLightSnapshot(
            LanternLighting.Source[] owned,
            Set<LanternLighting.Source> unique,
            Long2ObjectOpenHashMap<LanternLighting.Source[]> sections) {
        all = owned;
        this.unique = unique;
        this.sections = sections;
    }

    static LanternLightSnapshot update(
            LanternLightSnapshot previous, LanternLighting.Source[] sources) {
        if (sources.length < 8 || previous.unique.size() < 8)
            return new LanternLightSnapshot(sources);
        var all = sources.clone();
        Set<LanternLighting.Source> unique = new LinkedHashSet<>(Arrays.asList(all));
        if (unique.equals(previous.unique))
            return new LanternLightSnapshot(all, unique, previous.sections.clone());
        int limit = (unique.size() + previous.unique.size()) / 4, changes = 0;
        for (var source : previous.unique)
            if (!unique.contains(source) && ++changes >= limit) break;
        if (changes < limit)
            for (var source : unique)
                if (!previous.unique.contains(source) && ++changes >= limit) break;
        if (unique.size() < 8 || changes >= limit)
            return new LanternLightSnapshot(all, unique, build(unique));
        var sections = previous.sections.clone();
        var changed = new Long2ObjectOpenHashMap<List<LanternLighting.Source>>();
        for (var source : previous.unique)
            if (!unique.contains(source))
                forEachSection(source, key -> mutable(changed, sections, key).remove(source));
        for (var source : unique)
            if (!previous.unique.contains(source))
                forEachSection(source, key -> mutable(changed, sections, key).add(source));
        changed.long2ObjectEntrySet()
                .forEach(
                        entry -> {
                            if (entry.getValue().isEmpty()) sections.remove(entry.getLongKey());
                            else
                                sections.put(
                                        entry.getLongKey(),
                                        entry.getValue().toArray(LanternLighting.Source[]::new));
                        });
        return new LanternLightSnapshot(all, unique, sections);
    }

    private static Long2ObjectOpenHashMap<LanternLighting.Source[]> build(
            Set<LanternLighting.Source> unique) {
        var sections = new Long2ObjectOpenHashMap<LanternLighting.Source[]>();
        var building = new Long2ObjectOpenHashMap<List<LanternLighting.Source>>();
        double radius = LanternLighting.RADIUS + 1;
        for (var source : unique)
            for (int x = section(source.x() - radius); x <= section(source.x() + radius); x++)
                for (int y = section(source.y() - radius); y <= section(source.y() + radius); y++)
                    for (int z = section(source.z() - radius);
                            z <= section(source.z() + radius);
                            z++)
                        building.computeIfAbsent(SectionPos.asLong(x, y, z), k -> new ArrayList<>())
                                .add(source);
        building.long2ObjectEntrySet()
                .forEach(
                        entry ->
                                sections.put(
                                        entry.getLongKey(),
                                        entry.getValue().toArray(LanternLighting.Source[]::new)));
        return sections;
    }

    private static List<LanternLighting.Source> mutable(
            Long2ObjectOpenHashMap<List<LanternLighting.Source>> changed,
            Long2ObjectOpenHashMap<LanternLighting.Source[]> sections,
            long key) {
        return changed.computeIfAbsent(
                key,
                k -> {
                    var old = sections.get(k);
                    return old == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(old));
                });
    }

    private static void forEachSection(
            LanternLighting.Source source, java.util.function.LongConsumer action) {
        double radius = LanternLighting.RADIUS + 1;
        for (int x = section(source.x() - radius); x <= section(source.x() + radius); x++)
            for (int y = section(source.y() - radius); y <= section(source.y() + radius); y++)
                for (int z = section(source.z() - radius); z <= section(source.z() + radius); z++)
                    action.accept(SectionPos.asLong(x, y, z));
    }

    private static int section(double value) {
        return (int) Math.floor(value / 16);
    }

    public boolean isEmpty() {
        return all.length == 0;
    }

    public LanternLighting.Source[] sources() {
        return all.clone();
    }

    public int merge(double x, double y, double z, int packed) {
        if ((packed & 255) >= 240 || all.length == 0) return packed;
        var nearby = sections.get(SectionPos.asLong(section(x), section(y), section(z)));
        com.thelongtravail.data.HotPathMetrics.Counter.LIGHT_CANDIDATES.add(
                nearby == null ? 0 : nearby.length);
        return LanternLighting.merge(nearby == null ? NONE : nearby, x, y, z, packed);
    }
}

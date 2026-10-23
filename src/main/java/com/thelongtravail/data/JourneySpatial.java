package com.thelongtravail.data;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.config.JourneyDetectionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class JourneySpatial {
    private record Candidate(ResourceLocation id, List<BoundingBox> boxes) {}

    private record Entry(
            long until,
            Map<Structure, ResourceLocation> targets,
            List<Candidate> candidates,
            Set<Long> dependencies) {}

    private static final Map<ServerLevel, LinkedHashMap<Long, Entry>> CACHE =
            new IdentityHashMap<>();

    public static void clear() {
        CACHE.clear();
        BIOMES.clear();
    }

    private static void changed(ChunkEvent e) {
        if (e.getLevel() instanceof ServerLevel l) {
            long changed = e.getChunk().getPos().toLong();
            Runnable invalidate =
                    () -> {
                        var cache = CACHE.get(l);
                        if (cache != null)
                            cache.values().removeIf(entry -> entry.dependencies.contains(changed));
                        var biomes = BIOMES.get(l);
                        if (biomes != null)
                            biomes.values()
                                    .removeIf(
                                            entry ->
                                                    entry.dependsOn(
                                                            ChunkPos.getX(changed),
                                                            ChunkPos.getZ(changed)));
                    };
            if (l.getServer().isSameThread()) invalidate.run();
            else l.getServer().execute(invalidate);
        }
    }

    @SubscribeEvent
    public static void loaded(ChunkEvent.Load e) {
        changed(e);
    }

    @SubscribeEvent
    public static void unloaded(ChunkEvent.Unload e) {
        changed(e);
    }

    public static boolean contains(BoundingBox b, BlockPos p) {
        int h = JourneyDetectionConfig.HORIZONTAL.get(), v = JourneyDetectionConfig.VERTICAL.get();
        return (long) p.getX() >= (long) b.minX() - h
                && (long) p.getX() <= (long) b.maxX() + h
                && (long) p.getZ() >= (long) b.minZ() - h
                && (long) p.getZ() <= (long) b.maxZ() + h
                && (long) p.getY() >= (long) b.minY() - v
                && (long) p.getY() <= (long) b.maxY() + v;
    }

    private static List<BoundingBox> boxes(StructureStart start, ResourceLocation id) {
        if (JourneyDetectionConfig.PIECE_ONLY.get().contains(id.toString()))
            return start.getPieces().stream().map(p -> p.getBoundingBox()).toList();
        return List.of(start.getBoundingBox());
    }

    static boolean matches(StructureStart start, ResourceLocation id, BlockPos p) {
        return start != null
                && start.isValid()
                && boxes(start, id).stream().anyMatch(b -> contains(b, p));
    }

    public static List<ResourceLocation> loadedAt(
            ServerLevel level, BlockPos p, Map<Structure, ResourceLocation> targets) {
        if (targets.isEmpty()) return List.of();
        long key = ChunkPos.asLong(p);
        long now = level.getGameTime();
        var cache = CACHE.computeIfAbsent(level, l -> new LinkedHashMap<>(16, .75f, true));
        Entry entry = cache.get(key);
        if (entry == null
                || now >= entry.until
                || now < entry.until - 100
                || !entry.targets.equals(targets)) {
            List<Candidate> candidates = new ArrayList<>();
            Set<Long> dependencies = new HashSet<>();
            Map<Structure, Set<Long>> seen = new HashMap<>();
            int cx = p.getX() >> 4, cz = p.getZ() >> 4;
            int reach = JourneyDetectionConfig.HORIZONTAL.get() == 0 ? 0 : 1;
            for (int x = cx - reach; x <= cx + reach; x++)
                for (int z = cz - reach; z <= cz + reach; z++) {
                    dependencies.add(ChunkPos.asLong(x, z));
                    var origin = level.getChunkSource().getChunkNow(x, z);
                    if (origin == null) continue;
                    var refs = origin.getAllReferences();
                    for (var target : targets.entrySet()) {
                        var starts = refs.get(target.getKey());
                        if (starts == null) continue;
                        var visited = seen.computeIfAbsent(target.getKey(), s -> new HashSet<>());
                        for (long packed : starts) {
                            if (!visited.add(packed)) continue;
                            dependencies.add(packed);
                            var chunk =
                                    level.getChunkSource()
                                            .getChunkNow(
                                                    ChunkPos.getX(packed), ChunkPos.getZ(packed));
                            if (chunk == null) continue;
                            var start = chunk.getStartForStructure(target.getKey());
                            if (start != null && start.isValid())
                                candidates.add(
                                        new Candidate(
                                                target.getValue(),
                                                boxes(start, target.getValue())));
                        }
                    }
                }
            entry =
                    new Entry(
                            now + (candidates.isEmpty() ? 20 : 100),
                            Map.copyOf(targets),
                            List.copyOf(candidates),
                            Set.copyOf(dependencies));
            cache.put(key, entry);
            if (cache.size() > 256) cache.remove(cache.keySet().iterator().next());
        }
        Set<ResourceLocation> found = new LinkedHashSet<>();
        for (var c : entry.candidates)
            for (var box : c.boxes)
                if (contains(box, p)) {
                    found.add(c.id);
                    break;
                }
        return List.copyOf(found);
    }

    private static final Map<
                    ServerLevel,
                    it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap<BiomeSample>>
            BIOMES = new IdentityHashMap<>();

    private static final class BiomeSample {
        final BlockPos pos;
        final ResourceLocation center;
        final long created;
        final int radius;
        ResourceLocation[] neighbors;

        BiomeSample(BlockPos pos, ResourceLocation center, long created, int radius) {
            this.pos = pos.immutable();
            this.center = center;
            this.created = created;
            this.radius = radius;
        }

        boolean dependsOn(int x, int z) {
            int reach = radius + 2;
            return x >= ((pos.getX() - reach) >> 4)
                    && x <= ((pos.getX() + reach) >> 4)
                    && z >= ((pos.getZ() - reach) >> 4)
                    && z <= ((pos.getZ() + reach) >> 4);
        }
    }

    private static BiomeSample sample(
            ServerLevel level, BlockPos pos, ResourceLocation suppliedCenter) {
        var cache =
                BIOMES.computeIfAbsent(
                        level,
                        l -> new it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap<>(256));
        long key = pos.asLong(), now = level.getGameTime();
        int radius = JourneyDetectionConfig.BIOME_RADIUS.get();
        var sample = cache.getAndMoveToLast(key);
        if (sample == null
                || now < sample.created
                || now - sample.created >= 100
                || sample.radius != radius
                || suppliedCenter != null && !suppliedCenter.equals(sample.center)) {
            var center =
                    suppliedCenter != null
                            ? suppliedCenter
                            : level.getBiome(pos).unwrapKey().map(k -> k.location()).orElse(null);
            sample = new BiomeSample(pos, center, now, radius);
            cache.putAndMoveToLast(key, sample);
            if (cache.size() > 2048) cache.removeFirst();
        }
        return sample;
    }

    public static ResourceLocation biomeAt(ServerLevel level, BlockPos pos) {
        return sample(level, pos, null).center;
    }

    public static Set<ResourceLocation> biomes(
            ServerLevel level, BlockPos p, ResourceLocation center, Set<ResourceLocation> targets) {
        if (targets.isEmpty()) return Set.of();
        if (center != null && targets.contains(center)) return Set.of(center);
        int r = JourneyDetectionConfig.BIOME_RADIUS.get();
        if (r == 0) return Set.of();
        var sample = sample(level, p, center);
        if (sample.neighbors == null) {
            sample.neighbors = new ResourceLocation[4];
            var q = new BlockPos.MutableBlockPos();
            int baseX = (p.getX() - r - 2) >> 4, baseZ = (p.getZ() - r - 2) >> 4;
            int checked = 0, loaded = 0;
            for (int i = 0; i < 4; i++) {
                q.set(
                        p.getX() + (i == 0 ? r : i == 1 ? -r : 0),
                        p.getY(),
                        p.getZ() + (i == 2 ? r : i == 3 ? -r : 0));
                boolean ready = true;
                for (int x = (q.getX() - 2) >> 4; x <= (q.getX() + 2) >> 4; x++)
                    for (int z = (q.getZ() - 2) >> 4; z <= (q.getZ() + 2) >> 4; z++) {
                        int bit = 1 << ((x - baseX) * 3 + z - baseZ);
                        if ((checked & bit) == 0) {
                            checked |= bit;
                            if (level.getChunkSource().getChunkNow(x, z) != null) loaded |= bit;
                        }
                        if ((loaded & bit) == 0) ready = false;
                    }
                if (ready)
                    sample.neighbors[i] =
                            level.getBiome(q).unwrapKey().map(k -> k.location()).orElse(null);
            }
        }
        Set<ResourceLocation> found = null;
        for (var id : sample.neighbors)
            if (id != null && targets.contains(id)) {
                if (found == null) found = new HashSet<>();
                found.add(id);
            }
        return found == null ? Set.of() : found;
    }

    private JourneySpatial() {}
}

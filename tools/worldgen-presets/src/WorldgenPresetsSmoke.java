package travail.structures;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.*;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.*;
import java.util.*;

@Mod("travail_smoke")
public final class WorldgenPresetsSmoke {
    private record Target(String id, String theme, String dimension) {}

    private Target[] targets;
    private final boolean prepare = "before".equals(System.getProperty("travail.structures.stage"));
    private final Properties saved = new Properties();
    private final List<String> results = new ArrayList<>();
    private MinecraftServer server;
    private int index, checks, failures;
    private boolean done;

    public WorldgenPresetsSmoke() {
        MinecraftForge.EVENT_BUS.addListener(this::start);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }

    private void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private void start(ServerStartedEvent event) {
        server = event.getServer();
        try {
            if (!prepare) {
                try (var in = Files.newInputStream(Path.of("structures.properties"))) {
                    saved.load(in);
                }
                check(
                        !saved.getProperty("pid")
                                .equals(Long.toString(ProcessHandle.current().pid())),
                        "different JVM");
            } else saved.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
            targets =
                    new com.google.gson.Gson()
                            .fromJson(Files.readString(Path.of("targets.json")), Target[].class);
            defaults();
        } catch (Throwable error) {
            failed(error);
            finish();
        }
    }

    private void defaults() {
        check(TravailConfig.STRUCTURE_DRAW_COUNT.get() == 2, "draw count remains 2");
        for (var target : targets) {
            var aspect = TravailAspect.valueOf(target.theme());
            var entries = TravailConfig.STRUCTURE_POOLS.get(aspect).get();
            check(
                    entries.contains(
                            target.id() + "|@structure." + target.id().replace(':', '.') + "|1"),
                    "preset injection " + target.id());
            check(
                    entries.stream().filter(e -> e.startsWith(target.id() + "|")).count() == 1,
                    "no duplicate " + target.id());
        }
        if (targets.length == 0) {
            for (var entry :
                    Map.of(
                                    "primordial_caves",
                                    TravailAspect.FLOURISHING,
                                    "candy_cavity",
                                    TravailAspect.FLOURISHING,
                                    "abyssal_chasm",
                                    TravailAspect.ABYSS,
                                    "magnetic_caves",
                                    TravailAspect.DEEP_VALLEY,
                                    "toxic_caves",
                                    TravailAspect.DEEP_VALLEY,
                                    "forlorn_hollows",
                                    TravailAspect.UNDERWORLD)
                            .entrySet()) {
                String id = "alexscaves:" + entry.getKey();
                check(
                        TravailConfig.BIOME_POOLS
                                .get(entry.getValue())
                                .get()
                                .contains(id + "|@biome.alexscaves." + entry.getKey() + "|1"),
                        "biome injected " + id);
            }
        }
        results.add("PASS preset injection and draw count");
    }

    private void tick(TickEvent.ServerTickEvent event) {
        if (done
                || server == null
                || event.phase != TickEvent.Phase.END
                || server.getTickCount() % 20 != 0) return;
        if (targets.length == 0) {
            try {
                biomes();
            } catch (Throwable error) {
                failed(error);
            }
            finish();
            return;
        }
        String id = targets[index].id();
        try {
            inspect(id);
        } catch (Throwable error) {
            failed(error);
        }
        index++;
        if (index == targets.length) finish();
    }

    private void failed(Throwable error) {
        failures++;
        error.printStackTrace();
        results.add("FAIL target=" + index + " " + error);
    }

    private ServerLevel level(String name) {
        return server.getLevel(
                ResourceKey.create(
                        Registries.DIMENSION,
                        new ResourceLocation("minecraft", targets[index].dimension())));
    }

    private void biomes() throws Exception {
        ServerLevel level = server.overworld();
        for (String name :
                List.of(
                        "primordial_caves",
                        "candy_cavity",
                        "abyssal_chasm",
                        "magnetic_caves",
                        "toxic_caves",
                        "forlorn_hollows")) {
            var key =
                    ResourceKey.create(Registries.BIOME, new ResourceLocation("alexscaves", name));
            BlockPos point;
            if (prepare) {
                var found =
                        level.findClosestBiome3d(
                                biome -> biome.is(key), new BlockPos(0, 0, 0), 12000, 32, 32);
                check(found != null, "natural biome " + name);
                point = found.getFirst();
                saved.setProperty(name, point.getX() + "," + point.getY() + "," + point.getZ());
            } else {
                var xyz = saved.getProperty(name).split(",");
                point =
                        new BlockPos(
                                Integer.parseInt(xyz[0]),
                                Integer.parseInt(xyz[1]),
                                Integer.parseInt(xyz[2]));
            }
            level.getChunk(point.getX() >> 4, point.getZ() >> 4);
            check(level.getBiome(point).is(key), "actual biome at position " + name);
            results.add("PASS biome alexscaves:" + name + " at " + point);
            System.out.println(results.get(results.size() - 1));
        }
    }

    private void inspect(String name) throws Exception {
        System.out.println("BEGIN target=" + name);
        ServerLevel level = level(name);
        var id = new ResourceLocation(name);
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var structure = registry.get(id);
        int cx, cz;
        List<BlockPos> positions = new ArrayList<>();
        StructureStart start;
        if (prepare) {
            var holder = registry.getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE, id));
            BlockPos located = null;
            for (int offset : new int[] {0, 10000, -10000}) {
                var found =
                        level.getChunkSource()
                                .getGenerator()
                                .findNearestMapStructure(
                                        level,
                                        HolderSet.direct(holder),
                                        new BlockPos(offset, 80, 0),
                                        96,
                                        false);
                if (found != null) {
                    located = found.getFirst();
                    break;
                }
            }
            check(located != null, "naturally generated target exists " + id);
            cx = located.getX() >> 4;
            cz = located.getZ() >> 4;
            for (int x = cx - 1; x <= cx + 1; x++)
                for (int z = cz - 1; z <= cz + 1; z++) level.getChunk(x, z);
            start = level.getChunk(cx, cz).getStartForStructure(structure);
            check(start != null && start.isValid(), "registered start " + id);
            var pieces = start.getPieces();
            for (int n : new LinkedHashSet<>(List.of(0, pieces.size() / 2, pieces.size() - 1))) {
                var piece = pieces.get(n);
                var b = piece.getBoundingBox();
                level.getChunk((b.minX() + b.maxX()) / 2 >> 4, (b.minZ() + b.maxZ()) / 2 >> 4);
                b = piece.getBoundingBox();
                for (int y :
                        new LinkedHashSet<>(
                                List.of(
                                        b.minY() + 1,
                                        (b.minY() + b.maxY()) / 2,
                                        Math.max(b.minY(), b.maxY() - 1)))) {
                    positions.add(
                            new BlockPos((b.minX() + b.maxX()) / 2, y, (b.minZ() + b.maxZ()) / 2));
                }
            }
            saved.setProperty(name + ".start", cx + "," + cz);
            saved.setProperty(
                    name + ".points",
                    positions.stream()
                            .map(p -> p.getX() + "," + p.getY() + "," + p.getZ())
                            .collect(java.util.stream.Collectors.joining(";")));
        } else {
            var origin = saved.getProperty(name + ".start").split(",");
            cx = Integer.parseInt(origin[0]);
            cz = Integer.parseInt(origin[1]);
            for (var encoded : saved.getProperty(name + ".points").split(";")) {
                var p = encoded.split(",");
                positions.add(
                        new BlockPos(
                                Integer.parseInt(p[0]),
                                Integer.parseInt(p[1]),
                                Integer.parseInt(p[2])));
            }
            start = level.getChunk(cx, cz).getStartForStructure(structure);
            check(start != null && start.isValid(), "saved start reloaded " + id);
        }
        for (var point : positions) {
            level.getChunk(point.getX() >> 4, point.getZ() >> 4);
            JourneySpatial.clear();
            check(
                    JourneySpatial.loadedAt(level, point, Map.of(structure, id)).contains(id),
                    "visit miss " + id + " at " + point + " bounds=" + start.getBoundingBox());
            check(
                    JourneySpatial.loadedAt(level, point, Map.of(structure, id)).contains(id),
                    "cached visit miss " + id);
        }
        var b = start.getBoundingBox();
        var outside = new BlockPos(b.maxX() + 32, b.maxY() + 32, b.maxZ() + 32);
        check(
                !JourneySpatial.loadedAt(level, outside, Map.of(structure, id)).contains(id),
                "outside false positive " + id);
        String result =
                "PASS "
                        + id
                        + " dimension="
                        + level.dimension().location()
                        + " points="
                        + positions.size()
                        + " start="
                        + cx
                        + ","
                        + cz
                        + " bounds="
                        + b;
        results.add(result);
        System.out.println(result);
    }

    private void finish() {
        done = true;
        try {
            if (prepare)
                try (var out = Files.newOutputStream(Path.of("structures.properties"))) {
                    saved.store(out, "Natural structure locations for next JVM");
                }
            Files.writeString(
                    Path.of("structures-result.txt"),
                    (failures == 0 && index == targets.length ? "PASS" : "FAIL")
                            + " stage="
                            + (prepare ? "generate" : "reload")
                            + " targets="
                            + index
                            + " checks="
                            + checks
                            + " failures="
                            + failures
                            + "\n"
                            + String.join("\n", results));
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
        server.halt(false);
    }
}

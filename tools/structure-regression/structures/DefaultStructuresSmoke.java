package travail.structures;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

@Mod("travail_smoke")
public final class DefaultStructuresSmoke {
    private static final String[][] EXPECTED = {
        {"village_plains", "mansion"}, {"monument", "ruined_portal_ocean"},
        {"pillager_outpost", "village_desert"}, {"mineshaft", "ancient_city"},
        {"fortress", "bastion_remnant"}, {"stronghold", "end_city"}
    };
    private final boolean prepare = "before".equals(System.getProperty("travail.structures.stage"));
    private final Properties saved = new Properties();
    private final List<String> results = new ArrayList<>();
    private MinecraftServer server;
    private int index, checks, failures;
    private boolean done;

    public DefaultStructuresSmoke() {
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
                try (var in = Files.newInputStream(Path.of("structures.properties"))) { saved.load(in); }
                check(!saved.getProperty("pid").equals(Long.toString(ProcessHandle.current().pid())), "different JVM");
            } else saved.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
            defaults();
        } catch (Throwable error) { failed(error); finish(); }
    }
    private void defaults() {
        check(TravailConfig.STRUCTURE_DRAW_COUNT.get() == 2, "default draw count");
        var registry = server.overworld().registryAccess().registryOrThrow(Registries.STRUCTURE);
        var disabled = new RequirementPools.Pool(RequirementPools.Status.DISABLED, List.of(), 0);
        for (var aspect : TravailAspect.values()) {
            var expected = new HashSet<ResourceLocation>();
            for (var id : EXPECTED[aspect.ordinal()]) expected.add(new ResourceLocation("minecraft", id));
            var pool = RequirementPools.validate(TravailConfig.STRUCTURE_POOLS.get(aspect).get(), 2, registry::containsKey,
                    message -> { throw new AssertionError(message); });
            check(pool.status() == RequirementPools.Status.READY && pool.count() == 2, "valid pool " + aspect);
            check(new HashSet<>(pool.entries().stream().map(WeightedEntry::id).toList()).equals(expected), "exact defaults " + aspect);
            for (int seed = 0; seed < 100; seed++) {
                var generated = JourneyOperations.generate(new RequirementPools.AspectPools(disabled, pool), RandomSource.create(seed));
                var drawn = generated.getList("Structures", 8);
                check(drawn.size() == 2, "two default draws");
                check(Set.of(drawn.getString(0), drawn.getString(1)).equals(expected.stream().map(Object::toString).collect(java.util.stream.Collectors.toSet())), "both defaults selected");
            }
            var expanded = new ArrayList<String>(TravailConfig.STRUCTURE_POOLS.get(aspect).get());
            String extra = expected.contains(new ResourceLocation("minecraft:stronghold")) ? "minecraft:monument" : "minecraft:stronghold";
            expanded.add(extra + "|Extra fixture target|1");
            var larger = RequirementPools.validate(expanded, 2, registry::containsKey, message -> {throw new AssertionError(message);});
            boolean reached = false;
            for (int seed = 0; seed < 100; seed++) {
                var drawn = JourneyOperations.generate(new RequirementPools.AspectPools(disabled, larger), RandomSource.create(seed)).getList("Structures", 8);
                check(drawn.size() == 2 && !drawn.getString(0).equals(drawn.getString(1)), "expanded pool distinct draws");
                reached |= drawn.getString(0).equals(extra) || drawn.getString(1).equals(extra);
            }
            check(reached, "expanded pool can draw added target");
        }
        results.add("PASS default pools and weighted selection");
    }
    private void tick(TickEvent.ServerTickEvent event) {
        if (done || server == null || event.phase != TickEvent.Phase.END || server.getTickCount() % 20 != 0) return;
        String id = EXPECTED[index / 2][index % 2];
        try { inspect(id); } catch (Throwable error) { failed(error); }
        index++;
        if (index == 12) finish();
    }
    private void failed(Throwable error) { failures++; error.printStackTrace(); results.add("FAIL target=" + index + " " + error); }
    private ServerLevel level(String name) {
        return server.getLevel(name.equals("fortress") || name.equals("bastion_remnant") ? Level.NETHER : name.equals("end_city") ? Level.END : Level.OVERWORLD);
    }
    private void inspect(String name) throws Exception {
        ServerLevel level = level(name);
        var id = new ResourceLocation("minecraft", name);
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var structure = registry.get(id);
        int cx, cz;
        List<BlockPos> positions = new ArrayList<>();
        StructureStart start;
        if (prepare) {
            var holder = registry.getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE, id));
            BlockPos located = null;
            for (int offset : new int[]{10000, 0, -10000}) {
                var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder), new BlockPos(offset, 80, 0), 160, false);
                if (found != null) { located = found.getFirst(); break; }
            }
            check(located != null, "naturally generated target exists " + id);
            cx = located.getX() >> 4; cz = located.getZ() >> 4;
            for (int x = cx - 1; x <= cx + 1; x++) for (int z = cz - 1; z <= cz + 1; z++) level.getChunk(x, z);
            start = level.getChunk(cx, cz).getStartForStructure(structure);
            check(start != null && start.isValid(), "registered start " + id);
            var pieces = start.getPieces();
            for (int n : new LinkedHashSet<>(List.of(0, pieces.size() / 2, pieces.size() - 1))) {
                var piece = pieces.get(n);
                var b = piece.getBoundingBox();
                level.getChunk((b.minX() + b.maxX()) / 2 >> 4, (b.minZ() + b.maxZ()) / 2 >> 4);
                b = piece.getBoundingBox();
                for (int y : new LinkedHashSet<>(List.of(b.minY() + 1, (b.minY() + b.maxY()) / 2, Math.max(b.minY(), b.maxY() - 1)))) {
                    positions.add(new BlockPos((b.minX() + b.maxX()) / 2, y, (b.minZ() + b.maxZ()) / 2));
                }
            }
            saved.setProperty(name + ".start", cx + "," + cz);
            saved.setProperty(name + ".points", positions.stream().map(p -> p.getX() + "," + p.getY() + "," + p.getZ()).collect(java.util.stream.Collectors.joining(";")));
        } else {
            var origin = saved.getProperty(name + ".start").split(","); cx = Integer.parseInt(origin[0]); cz = Integer.parseInt(origin[1]);
            for (var encoded : saved.getProperty(name + ".points").split(";")) { var p = encoded.split(","); positions.add(new BlockPos(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]))); }
            start = level.getChunk(cx, cz).getStartForStructure(structure);
            check(start != null && start.isValid(), "saved start reloaded " + id);
        }
        for (var point : positions) {
            level.getChunk(point.getX() >> 4, point.getZ() >> 4);
            JourneySpatial.clear();
            check(JourneySpatial.loadedAt(level, point, Map.of(structure, id)).contains(id), "visit miss " + id + " at " + point + " bounds=" + start.getBoundingBox());
            check(JourneySpatial.loadedAt(level, point, Map.of(structure, id)).contains(id), "cached visit miss " + id);
        }
        var b = start.getBoundingBox();
        var outside = new BlockPos(b.maxX() + 32, b.maxY() + 32, b.maxZ() + 32);
        check(!JourneySpatial.loadedAt(level, outside, Map.of(structure, id)).contains(id), "outside false positive " + id);
        String result = "PASS " + id + " dimension=" + level.dimension().location() + " points=" + positions.size() + " start=" + cx + "," + cz + " bounds=" + b;
        results.add(result); System.out.println(result);
    }
    private void finish() {
        done = true;
        try {
            if (prepare) try (var out = Files.newOutputStream(Path.of("structures.properties"))) { saved.store(out, "Natural structure locations for next JVM"); }
            Files.writeString(Path.of("structures-result.txt"), (failures == 0 && index == 12 ? "PASS" : "FAIL") + " stage=" + (prepare ? "generate" : "reload") + " targets=" + index + " checks=" + checks + " failures=" + failures + "\n" + String.join("\n", results));
        } catch (Exception error) { throw new RuntimeException(error); }
        server.halt(false);
    }
}

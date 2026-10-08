package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.google.gson.GsonBuilder;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.*;
import com.thelongtravail.entity.WayguideEntity;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

// 隔离服务端真实方法耗时与分配测量，不注入正式代码，不模拟网络或客户端 GPU。
public final class WayguidePerformanceSmoke {
    private static final com.sun.management.ThreadMXBean THREADS =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static final List<Map<String, Object>> RESULTS = new ArrayList<>();
    private static final List<WayguideEntity> FLIGHTS = new ArrayList<>();
    private static final List<FakePlayer> PLAYERS = new ArrayList<>();
    private static Field tasksField, scannerField, samplesField, updatingChunks;
    private static ResourceLocation id(String path) { return new ResourceLocation("minecraft", path); }
    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }
    private static Map<?, ?> tasks() throws Exception { return (Map<?, ?>) tasksField.get(null); }
    private static long samples(List<Object> tasks) throws Exception {
        long sum = 0; for (Object task : tasks) sum += samplesField.getInt(task); return sum;
    }
    private static int chunks(ServerLevel level) throws Exception {
        return ((Map<?, ?>) updatingChunks.get(level.getChunkSource().chunkMap)).size();
    }
    private static long allocated() { return THREADS.getThreadAllocatedBytes(Thread.currentThread().getId()); }
    private static long cpu() { return THREADS.getCurrentThreadCpuTime(); }
    private static long gcCount() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b -> Math.max(0, b.getCollectionCount())).sum();
    }
    private static final class Stats {
        final List<Double> wall = new ArrayList<>();
        long allocation, cpu;
        void measure(Runnable action, int units) {
            long a = allocated(), c = cpu(), start = System.nanoTime();
            action.run();
            long elapsed = System.nanoTime() - start, endCpu = cpu(), endAllocation = allocated();
            wall.add(elapsed / 1_000_000.0 / units); cpu += endCpu - c; allocation += endAllocation - a;
        }
        Map<String, Object> report(String name, int units) {
            double[] sorted = wall.stream().mapToDouble(Double::doubleValue).sorted().toArray();
            var result = new LinkedHashMap<String, Object>();
            result.put("scenario", name); result.put("observations", sorted.length); result.put("units_per_observation", units);
            result.put("mean_ms_per_unit", Arrays.stream(sorted).average().orElse(0));
            result.put("p50_ms_per_unit", percentile(sorted, 0.50)); result.put("p95_ms_per_unit", percentile(sorted, 0.95));
            result.put("p99_ms_per_unit", percentile(sorted, 0.99)); result.put("max_ms_per_unit", percentile(sorted, 1.0));
            result.put("main_thread_cpu_ms_total", cpu / 1_000_000.0);
            result.put("main_thread_allocated_bytes_per_unit", allocation / (double) Math.max(1, sorted.length * units));
            RESULTS.add(result); return result;
        }
        static double percentile(double[] sorted, double fraction) {
            return sorted.length == 0 ? 0 : sorted[Math.min(sorted.length - 1, Math.max(0, (int) Math.ceil(sorted.length * fraction) - 1))];
        }
    }
    private static FakePlayer player(ServerLevel level, int x, int z) {
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "RoutePerf")) {
            @Override public void displayClientMessage(Component text, boolean actionBar) {}
        };
        player.setPos(x + 0.5, 90, z + 0.5); player.getAbilities().instabuild = true;
        var originChunk = new net.minecraft.world.level.ChunkPos(player.blockPosition());
        level.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.FORCED, originChunk, 2, originChunk);
        level.getChunkAt(player.blockPosition()); // 真实玩家所在区块已加载，此夹具预加载排除出被测调用。
        var inventory = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        var slots = new CurioStacksHandler(inventory, "travel_diary", 1, true, false, true, ICurio.DropRule.DEFAULT);
        inventory.setCurios(new HashMap<>(Map.of("travel_diary", slots)));
        var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        var root = new CompoundTag();
        root.putBoolean("Initialized", true); root.putInt("Version", 2); root.putUUID("JourneyId", UUID.randomUUID());
        root.putUUID("OwnerId", player.getUUID()); root.putLong("RequirementsRevision", 1);
        var requirements = new CompoundTag();
        for (var aspect : TravailAspect.values()) {
            var tag = new CompoundTag(); tag.put("Biomes", new ListTag()); tag.put("Structures", new ListTag());
            tag.putString("BiomesStatus", "READY"); tag.putString("StructuresStatus", "READY"); requirements.put(aspect.id(), tag);
        }
        root.put("Requirements", requirements); diary.getOrCreateTag().put("LongTravail", root);
        slots.getStacks().setStackInSlot(0, diary);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModRegistry.WAYGUIDE.get()));
        PLAYERS.add(player); return player;
    }
    private static void targets(FakePlayer player, List<ResourceLocation> biomes, List<ResourceLocation> structures) {
        var diary = com.thelongtravail.helper.TravailCurios.stack(player);
        var requirements = diary.getTag().getCompound("LongTravail").getCompound("Requirements");
        for (var aspect : TravailAspect.values()) {
            requirements.getCompound(aspect.id()).put("Biomes", new ListTag());
            requirements.getCompound(aspect.id()).put("Structures", new ListTag());
        }
        var biomesTag = new ListTag(); biomes.forEach(id -> biomesTag.add(StringTag.valueOf(id.toString())));
        var structuresTag = new ListTag(); structures.forEach(id -> structuresTag.add(StringTag.valueOf(id.toString())));
        requirements.getCompound(TravailAspect.FLOURISHING.id()).put("Biomes", biomesTag);
        requirements.getCompound(TravailAspect.FLOURISHING.id()).put("Structures", structuresTag);
        LongTravailData.requirementsChanged(diary); WayguideSearch.forget(player); JourneyQueries.forget(player);
        player.getCooldowns().removeCooldown(ModRegistry.WAYGUIDE.get());
    }
    private static void discardFlights() { FLIGHTS.forEach(WayguideEntity::discard); FLIGHTS.clear(); }
    private static Object start(FakePlayer player, Stats use) throws Exception {
        player.getCooldowns().removeCooldown(ModRegistry.WAYGUIDE.get());
        use.measure(() -> {
            if (!WayguideSearch.use(player, InteractionHand.MAIN_HAND)) throw new AssertionError("search did not start");
        }, 1);
        Object task = tasks().get(player.getUUID());
        if (task == null) throw new AssertionError("no active task");
        if (scannerField == null) {
            scannerField = field(task.getClass(), "scanner"); samplesField = field(task.getClass(), "samples");
        }
        return task;
    }
    private static List<ResourceLocation> alternatives(FakePlayer player) {
        var current = player.serverLevel().getBiome(player.blockPosition());
        return player.serverLevel().getChunkSource().getGenerator().getBiomeSource().possibleBiomes().stream()
                .filter(b -> !b.equals(current)).map(b -> b.unwrapKey().orElseThrow().location()).toList();
    }
    private static void cleanup() {
        WayguideSearch.clear(); discardFlights();
        for (var p : PLAYERS) {
            JourneyQueries.forget(p); var chunk = new net.minecraft.world.level.ChunkPos(p.blockPosition());
            p.serverLevel().getChunkSource().removeRegionTicket(net.minecraft.server.level.TicketType.FORCED, chunk, 2, chunk);
        }
        PLAYERS.clear();
    }

    // 在实际的服务端刻循环中复核冷结构尖峰，并记录等待栈。
    public static void runLive(ServerLevel level) throws Exception {
        THREADS.setThreadAllocatedMemoryEnabled(true); THREADS.setThreadCpuTimeEnabled(true);
        tasksField = field(WayguideSearch.class, "TASKS");
        updatingChunks = field(net.minecraft.server.level.ChunkMap.class, "updatingChunkMap");
        new LiveProbe(level).register();
    }

    public static final class LiveProbe {
        final ServerLevel level;
        final jdk.jfr.Recording recording = new jdk.jfr.Recording();
        final List<Map<String, Object>> cases = new ArrayList<>();
        Stats endHandlers = new Stats(), wholeTick = new Stats(), prep = new Stats();
        FakePlayer player;
        Object task;
        String structure;
        int stage, delay = 80, calls, chunksBefore, flightCount;
        long tickStart, endStart, allocationStart, cpuStart;
        final Consumer<EntityJoinLevelEvent> capture = event -> { if (event.getEntity() instanceof WayguideEntity entity) FLIGHTS.add(entity); };
        LiveProbe(ServerLevel level) { this.level = level; }
        void register() {
            recording.enable("jdk.ThreadPark").withThreshold(java.time.Duration.ofMillis(1)).withStackTrace();
            recording.enable("jdk.ExecutionSample").withPeriod(java.time.Duration.ofMillis(5)).withStackTrace();
            recording.enable("jdk.GarbageCollection");
            recording.start();
            MinecraftForge.EVENT_BUS.register(this); MinecraftForge.EVENT_BUS.addListener(capture);
        }
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void before(TickEvent.ServerTickEvent event) {
            if (event.getServer() != level.getServer()) return;
            if (event.phase == TickEvent.Phase.START) tickStart = System.nanoTime();
            else { allocationStart = allocated(); cpuStart = cpu(); endStart = System.nanoTime(); }
        }
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void after(TickEvent.ServerTickEvent event) throws Exception {
            if (event.phase != TickEvent.Phase.END || event.getServer() != level.getServer()) return;
            long endWall = System.nanoTime() - endStart, endCpu = cpu() - cpuStart, endAllocation = allocated() - allocationStart;
            if (task != null) {
                endHandlers.wall.add(endWall / 1_000_000.0); endHandlers.cpu += endCpu; endHandlers.allocation += endAllocation;
                wholeTick.wall.add((System.nanoTime() - tickStart) / 1_000_000.0); calls++;
                if (calls > 700) throw new AssertionError("live scenario did not finish");
                if (stage < 5 && !tasks().isEmpty()) return;
                if (stage >= 5 && calls < 200) return;
                var result = endHandlers.report("live_" + structure + "_end_handlers_" + stage, 1);
                result.put("search_samples", samplesField.getInt(task)); result.put("successful", FLIGHTS.stream().anyMatch(e -> !e.searching() && !e.isRemoved()));
                if (stage >= 5) {
                    var active = new ArrayList<Object>(tasks().values());
                    int expected = stage == 5 ? 1 : stage == 6 ? 10 : 50;
                    if (active.size() != expected) throw new AssertionError("sustained tasks cancelled unexpectedly");
                    result.put("active_jobs", active.size()); result.put("total_search_samples", samples(active));
                    var counts = active.stream().mapToInt(t -> { try { return samplesField.getInt(t); } catch (Exception e) { throw new RuntimeException(e); } }).summaryStatistics();
                    result.put("minimum_samples_per_player", counts.getMin()); result.put("maximum_samples_per_player", counts.getMax());
                    if (counts.getMin() == 0) throw new AssertionError("a player was starved by shared budget");
                }
                result.put("additional_chunk_holders", chunks(level) - chunksBefore);
                result.put("ticks_over_50_ms", endHandlers.wall.stream().filter(ms -> ms > 50).count());
                result.put("measurement", "Actual server ticks: time between HIGHEST and LOWEST Forge END handlers, includes Travail reward handler and Wayguide search.");
                cases.add(result);
                cases.add(wholeTick.report("live_cold_" + structure + "_whole_tick_" + stage, 1));
                cases.add(prep.report("live_cold_" + structure + "_right_click_" + stage, 1));
                System.out.println("WAYGUIDE_LIVE_CASE: " + structure + " max_ms=" + result.get("max_ms_per_unit"));
                cleanup(); task = null; player = null; stage++; delay = 20;
                endHandlers = new Stats(); wholeTick = new Stats(); prep = new Stats();
                if (stage >= 8) { finish(); return; }
            }
            if (--delay > 0) return;
            if (player == null) {
                structure = stage < 3 ? "village_plains" : stage == 3 ? "mineshaft" : stage == 4 ? "stronghold" : "sustained_biome_" + (stage == 5 ? 1 : stage == 6 ? 10 : 50);
                int x = stage < 3 ? 786432 + stage * 16384 : stage == 3 ? 262144 : 2048;
                int z = stage < 3 ? 786432 : stage == 3 ? 262144 : -2048;
                player = player(level, x, z);
                if (stage < 5) targets(player, List.of(), List.of(id(structure)));
                else targets(player, List.of(id("mushroom_fields")), List.of());
                delay = 20; // 原点准备后让区块管理和 I/O 队列经历正常的服务端刻。
                return;
            }
            chunksBefore = chunks(level); flightCount = FLIGHTS.size(); task = start(player, prep); calls = 0;
            if (stage >= 5) {
                int count = stage == 5 ? 1 : stage == 6 ? 10 : 50;
                WayguidePreparationSupport.prepare(task);
                var impossible = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(ResourceKey.create(Registries.BIOME, id("nether_wastes")));
                field(scannerField.get(task).getClass(), "biomes").set(scannerField.get(task), Set.of(impossible));
                for (int n = 1; n < count; n++) {
                    var p = player(level, player.blockPosition().getX(), player.blockPosition().getZ()); targets(p, List.of(id("mushroom_fields")), List.of());
                    Object other = start(p, prep); WayguidePreparationSupport.prepare(other);
                    field(scannerField.get(other).getClass(), "biomes").set(scannerField.get(other), Set.of(impossible));
                }
            }
        }
        private void finish() throws Exception {
            var path = Path.of(System.getProperty("travail.smoke.result")).resolveSibling("wayguide-live-performance.json");
            recording.stop(); recording.dump(path.resolveSibling("wayguide-live.jfr")); recording.close();
            var report = new LinkedHashMap<String, Object>();
            report.put("world_seed", level.getSeed()); report.put("results", cases);
            report.put("searchRadius", TravailConfig.WAYGUIDE_RADIUS.get()); report.put("searchTimeoutSeconds", TravailConfig.WAYGUIDE_TIMEOUT_SECONDS.get());
            report.put("notes", "Real dedicated-server tick loop; Forge+Curios development environment, no real clients. Fresh fixed-seed world; origin preparation excluded and kept loaded to model real players, which can warm nearby candidate chunks. Whole-tick timings include other server work. Three village origins and one origin for each other structure. Sustained biome cases force a nonmatching set for 200 actual ticks with 1/10/50 same-origin creative FakePlayers; main-thread counters exclude worker threads.");
            Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(report));
            Files.writeString(Path.of(System.getProperty("travail.smoke.result")), "PASS");
            MinecraftForge.EVENT_BUS.unregister(this); MinecraftForge.EVENT_BUS.unregister(capture); cleanup();
            System.out.println("TRAVAIL_WAYGUIDE_LIVE_PERFORMANCE_PASS: " + path);
            level.getServer().halt(false);
        }
    }

    public static void run(ServerLevel level) throws Exception { runLive(level); }
}

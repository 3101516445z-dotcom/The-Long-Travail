package com.thelongtravail.data;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.entity.WayguideEntity;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.mixin.StructureRingsAccessor;
import com.thelongtravail.mixin.ChunkFutureInvoker;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import java.util.*;
import java.util.concurrent.CompletableFuture;

// 仅在服务端游戏线程工作；全服共享采样、时间和候选区块读取预算。
public final class WayguideSearch {
    private record Journey(UUID id, long revision) {
        static Journey of(ItemStack diary) {
            var identity = LongTravailData.queryIdentity(diary);
            return new Journey(identity.journey(), identity.revision());
        }
    }
    private record Target(ResourceLocation id, BlockPos pos) {}
    private record Cached(Journey journey, ResourceKey<Level> dimension, BlockPos origin, Target target, long expires) {}
    private static final Map<UUID, Task> TASKS = new HashMap<>();
    private static final Map<UUID, Cached> CACHE = new HashMap<>();
    private static final Deque<UUID> QUEUE = new ArrayDeque<>();
    private static final List<ChunkRequest> REQUESTS = new ArrayList<>();
    private static final TicketType<UUID> SEARCH_TICKET = TicketType.create("the_long_travail_wayguide", UUID::compareTo);
    private static final int TICKET_DISTANCE = 33 - ChunkLevel.byStatus(ChunkStatus.STRUCTURE_STARTS);

    public static void clear() {
        for (Task task : List.copyOf(TASKS.values())) cancel(task, null);
        TASKS.clear(); CACHE.clear(); QUEUE.clear();
    }
    public static void stopped() {
        clear(); REQUESTS.forEach(ChunkRequest::release); REQUESTS.clear();
    }
    public static boolean ownsSearch(UUID player, UUID pearl) {
        Task task = TASKS.get(player); return task != null && task.entity.getUUID().equals(pearl);
    }
    public static void forget(ServerPlayer player) {
        UUID id = player.getUUID(); Task task = TASKS.remove(id);
        CACHE.remove(id); QUEUE.remove(id);
        if (task != null) cancel(task, null);
    }

    public static boolean use(ServerPlayer player, InteractionHand hand) {
        ItemStack diary = TravailCurios.stack(player);
        if (diary.isEmpty()) return fail(player, "unavailable");
        if (!LongTravailData.tryInitialize(diary, player)) return fail(player, "invalid");
        // 不在右键路径中同步读取结构区块；普通到访检测由原有玩家周期任务负责。
        if (TASKS.containsKey(player.getUUID())) return fail(player, "busy");
        if (player.getCooldowns().isOnCooldown(ModRegistry.WAYGUIDE.get())) return false;
        // 脚下群系只读已加载区块；既不触发加载，也不等待原来的整组结构到访查询。
        var loaded = player.serverLevel().getChunkSource().getChunkNow(player.blockPosition().getX() >> 4, player.blockPosition().getZ() >> 4);
        if (loaded != null) loaded.getNoiseBiome(QuartPos.fromBlock(player.getBlockX()), QuartPos.fromBlock(player.getBlockY()), QuartPos.fromBlock(player.getBlockZ()))
                .unwrapKey().ifPresent(key -> LongTravailData.visitBiome(diary, key.location()));
        Cached cached = CACHE.get(player.getUUID());
        boolean cacheHit = cached != null && cached.journey.equals(Journey.of(diary)) && cached.dimension.equals(player.level().dimension())
                && player.serverLevel().getWorldBorder().isWithinBounds(cached.target.pos)
                && cached.expires > player.level().getGameTime() && cached.origin.distSqr(player.blockPosition()) <= 128.0 * 128.0
                && Math.hypot(cached.target.pos.getX() - player.getX(), cached.target.pos.getZ() - player.getZ()) <= Math.min(5000, TravailConfig.WAYGUIDE_RADIUS.get());
        if (!cacheHit) CACHE.remove(player.getUUID());
        long accepted = System.nanoTime();
        Scanner scanner = cacheHit ? new CachedScanner(player.serverLevel(), player.blockPosition(), cached.target) : null;
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ModRegistry.WAYGUIDE.get()) || held.isEmpty()) return false;
        var entity = ModRegistry.WAYGUIDE_ENTITY.get().create(player.serverLevel());
        if (entity == null) return fail(player, "error");
        entity.beginSearch(player, held);
        if (!player.serverLevel().addFreshEntity(entity)) return fail(player, "error");
        boolean survival = !player.getAbilities().instabuild;
        if (survival) held.shrink(1);
        entity.reserve(survival);
        Task task = new Task(player, diary, scanner, entity, accepted);
        TASKS.put(player.getUUID(), task); QUEUE.addLast(player.getUUID());
        player.getCooldowns().addCooldown(ModRegistry.WAYGUIDE.get(), 20);
        message(player, "searching");
        return true;
    }

    // 每次推进一个目标或放置项；不在右键路径读取生成器放置数据。
    private static final class Preparation {
        final ServerLevel level;
        ItemStack diary;
        final BlockPos origin;
        final Set<Holder<Biome>> biomes = new HashSet<>();
        final Map<StructurePlacement, List<Holder<Structure>>> groups = new LinkedHashMap<>();
        Iterator<ResourceLocation> targets;
        Iterator<StructurePlacement> placements = Collections.emptyIterator();
        Iterator<Map.Entry<StructurePlacement, List<Holder<Structure>>>> cursors;
        Holder<Structure> structure;
        StructureScanner structureScanner;
        Scanner result;
        int phase;
        boolean finished;

        Preparation(ServerLevel level, ItemStack diary, BlockPos origin) {
            this.level = level; this.diary = diary; this.origin = origin;
        }
        void step() {
            if (phase == 0) {
                targets = LongTravailData.remainingBiomes(diary).iterator(); phase = 1;
            } else if (phase == 1) {
                if (targets.hasNext()) {
                    var source = level.getChunkSource().getGenerator().getBiomeSource();
                    level.registryAccess().registryOrThrow(Registries.BIOME)
                            .getHolder(ResourceKey.create(Registries.BIOME, targets.next()))
                            .filter(source.possibleBiomes()::contains).ifPresent(biomes::add);
                } else if (!biomes.isEmpty()) {
                    result = new BiomeScanner(level, origin, biomes); finished = true;
                } else if (!level.structureManager().shouldGenerateStructures()) finished = true;
                else { targets = LongTravailData.remainingStructures(diary).iterator(); phase = 2; }
            } else if (phase == 2) {
                if (placements.hasNext()) {
                    groups.computeIfAbsent(placements.next(), p -> new ArrayList<>()).add(structure);
                } else if (targets.hasNext()) {
                    structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                            .getHolder(ResourceKey.create(Registries.STRUCTURE, targets.next())).orElse(null);
                    if (structure != null) placements = level.getChunkSource().getGeneratorState()
                            .getPlacementsForStructure(structure).iterator();
                } else if (groups.isEmpty()) finished = true;
                else {
                    structureScanner = new StructureScanner(level, origin);
                    cursors = groups.entrySet().iterator(); phase = 3;
                }
            } else if (cursors.hasNext()) {
                var entry = cursors.next();
                structureScanner.placements.add(new PlacementCursor(level, origin, structureScanner.radius, entry.getKey(), entry.getValue()));
            } else { result = structureScanner; finished = true; }
        }
    }

    private static final class Task {
        final ServerPlayer player;
        final WayguideEntity entity;
        final Journey journey;
        final ResourceKey<Level> dimension;
        final BlockPos origin;
        Scanner scanner;
        Preparation preparation;
        final int maxSamples;
        final long started, timeout;
        final long startTick;
        int samples;

        Task(ServerPlayer player, ItemStack diary, Scanner scanner, WayguideEntity entity, long accepted) {
            this.player = player; this.entity = entity;
            this.journey = Journey.of(diary); this.dimension = player.level().dimension();
            this.origin = player.blockPosition().immutable(); this.scanner = scanner;
            if (scanner == null) this.preparation = new Preparation(player.serverLevel(), diary, origin);
            this.maxSamples = TravailConfig.WAYGUIDE_MAX_SAMPLES.get();
            this.started = accepted; this.timeout = TravailConfig.WAYGUIDE_TIMEOUT_SECONDS.get() * 1_000_000_000L;
            this.startTick = player.level().getGameTime();
        }

        boolean valid() {
            ItemStack diary = TravailCurios.stack(player);
            boolean valid = !player.isRemoved() && player.isAlive() && player.level().dimension().equals(dimension)
                    && !diary.isEmpty() && journey.equals(Journey.of(diary))
                    && !entity.isRemoved()
                    && origin.distSqr(player.blockPosition()) <= 128.0 * 128.0;
            // 同旅程的 NBT/装备副本替换不应中断搜索，也不能继续读取脱离槽位的旧对象。
            if (valid && preparation != null) preparation.diary = diary;
            return valid;
        }
    }

    public static void tick() {
        LoadBudget loads = new LoadBudget();
        // 仅释放自身的区块票据，不取消原版共享的异步请求，待请求完成或失败后再释放全服在途名额。
        REQUESTS.removeIf(request -> { if (!request.future.isDone()) return false; request.release(); return true; });
        if (QUEUE.isEmpty()) return;
        int budget = TravailConfig.WAYGUIDE_SAMPLES_PER_TICK.get();
        int blocked = 0;
        while (!QUEUE.isEmpty() && loads.take()) {
            UUID id = QUEUE.removeFirst();
            Task task = TASKS.get(id);
            if (task == null) continue;
            boolean progressed = false;
            try {
                if (!task.valid()) { TASKS.remove(id); cancel(task, "cancelled"); continue; }
                if (System.nanoTime() - task.started >= task.timeout) { TASKS.remove(id); cancel(task, "timeout"); continue; }
                if (task.preparation != null) {
                    for (int i = 0; i < 32 && !task.preparation.finished && loads.take(); i++) {
                        task.preparation.step(); progressed = true;
                    }
                    if (task.preparation.finished) {
                        task.scanner = task.preparation.result; task.preparation = null;
                        if (task.scanner == null) { TASKS.remove(id); cancel(task, "no_targets"); continue; }
                    }
                    QUEUE.addLast(id);
                    blocked = progressed ? 0 : blocked + 1;
                    if (blocked >= QUEUE.size()) break;
                    continue;
                }
                // 每轮至多 32 次采样，使多人搜索轮流取得全服预算。
                for (int i = 0; i < 32 && budget > 0 && loads.take(); i++) {
                    if (task.scanner.target != null || task.samples >= task.maxSamples || task.scanner.finished) break;
                    if (!task.scanner.step(loads)) break;
                    progressed = true; task.samples++; budget--;
                    if (task.scanner.target != null) break;
                }
                // 单个外部调用可能超过软预算；发射前重新核对超时与旅程身份。
                if (System.nanoTime() - task.started >= task.timeout) { TASKS.remove(id); cancel(task, "timeout"); continue; }
                if ((task.scanner.target != null || task.scanner.finished || task.samples >= task.maxSamples)
                        && task.player.level().getGameTime() - task.startTick < 20) {
                    QUEUE.addLast(id); blocked++; if (blocked >= QUEUE.size()) break; continue;
                }
                if (task.scanner.target != null) {
                    // 搜索和悬停会持续多个游戏刻，发射前须重新检查可能缩小的世界边界。
                    if (!task.scanner.within(task.scanner.target.pos)) { TASKS.remove(id); cancel(task, "not_found"); continue; }
                    if (task.valid()) {
                        task.entity.launch(task.player, task.scanner.target.pos);
                        task.player.getCooldowns().addCooldown(ModRegistry.WAYGUIDE.get(), 60);
                        task.player.serverLevel().playSound(null, task.entity.getX(), task.entity.getY(), task.entity.getZ(),
                                SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 0.7F, 1.0F);
                        CACHE.put(id, new Cached(task.journey, task.dimension, task.origin, task.scanner.target,
                                task.player.level().getGameTime() + TravailConfig.WAYGUIDE_CACHE_TICKS.get()));
                    } else cancel(task, "cancelled");
                    TASKS.remove(id);
                } else if (task.scanner.finished || task.samples >= task.maxSamples) {
                    TASKS.remove(id); cancel(task, "not_found");
                } else {
                    QUEUE.addLast(id);
                    blocked = progressed ? 0 : blocked + 1;
                    if (blocked >= QUEUE.size()) break;
                }
            } catch (RuntimeException error) {
                TASKS.remove(id);
                cancel(task, "error");
                TheLongTravail.LOGGER.error("Wayguide search failed in {}", task.dimension.location(), error);
            }
        }
    }

    private static void cancel(Task task, String reason) {
        if (task.scanner != null) task.scanner.close();
        task.entity.refund(task.player);
        if (reason != null) message(task.player, reason);
    }

    private static boolean fail(ServerPlayer player, String key) { message(player, key); return false; }
    private static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("message.the_long_travail.wayguide." + key), true);
    }

    private static final class LoadBudget {
        int remaining = 1;
        // 等待、校验和准备也计工作次数；即使时钟粒度很粗，也不会无限空转。
        int operations = Math.max(4096, TravailConfig.WAYGUIDE_SAMPLES_PER_TICK.get() * 2);
        final long deadline = System.nanoTime() + TravailConfig.WAYGUIDE_SEARCH_MILLIS.get() * 1_000_000L;
        boolean take() { return operations-- > 0 && System.nanoTime() < deadline; }
    }
    private abstract static class Scanner {
        final ServerLevel level;
        final BlockPos origin;
        final int radius;
        Target target;
        boolean finished;
        Scanner(ServerLevel level, BlockPos origin) {
            this.level = level; this.origin = origin.immutable(); this.radius = Math.min(5000, TravailConfig.WAYGUIDE_RADIUS.get());
        }
        abstract boolean step(LoadBudget budget);
        void close() {}
        boolean within(BlockPos pos) {
            return Math.hypot((double) pos.getX() - origin.getX(), (double) pos.getZ() - origin.getZ()) <= radius
                    && level.getWorldBorder().isWithinBounds(pos);
        }
    }

    private static final class CachedScanner extends Scanner {
        CachedScanner(ServerLevel level, BlockPos origin, Target target) { super(level, origin); this.target = target; }
        @Override boolean step(LoadBudget budget) { return false; }
    }

    private static final class ChunkRequest {
        final ServerLevel level; final ChunkPos pos; final UUID ticket = UUID.randomUUID();
        final CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future;
        boolean released;
        ChunkRequest(ServerLevel level, ChunkPos pos) {
            this.level = level; this.pos = pos;
            level.getChunkSource().addRegionTicket(SEARCH_TICKET, pos, TICKET_DISTANCE, ticket);
            try { future = ((ChunkFutureInvoker) level.getChunkSource()).travail$requestChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS, true); }
            catch (RuntimeException error) { release(); throw error; }
        }
        void release() {
            if (released) return; released = true;
            level.getChunkSource().removeRegionTicket(SEARCH_TICKET, pos, TICKET_DISTANCE, ticket);
        }
    }

    private static final class BiomeScanner extends Scanner {
        final Set<Holder<Biome>> biomes;
        final WayguideGeometry.Spiral grid;
        final int spacing;
        final int[] heights;
        int height;
        BiomeScanner(ServerLevel level, BlockPos origin, Set<Holder<Biome>> biomes) {
            super(level, origin); this.biomes = Set.copyOf(biomes);
            spacing = TravailConfig.WAYGUIDE_BIOME_SPACING.get();
            grid = new WayguideGeometry.Spiral((radius + spacing - 1) / spacing);
            // 包含玩家高度及整个维度的高度层，地下目标与地表目标同样参与搜索。
            Set<Integer> ys = new LinkedHashSet<>();
            int min = level.getMinBuildHeight(), max = level.getMaxBuildHeight() - 1;
            ys.add(Math.max(min, Math.min(max, origin.getY())));
            for (int y = min; y <= max; y += TravailConfig.WAYGUIDE_HEIGHT_SPACING.get()) ys.add(y);
            ys.add(max);
            heights = ys.stream().sorted(Comparator.comparingInt(y -> Math.abs(y - origin.getY()))).mapToInt(Integer::intValue).toArray();
        }
        @Override boolean step(LoadBudget budget) {
            int x = origin.getX() + grid.x() * spacing, z = origin.getZ() + grid.z() * spacing;
            BlockPos pos = new BlockPos(x, heights[height], z);
            if (within(pos)) {
                var biome = level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(
                        QuartPos.fromBlock(x), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(z),
                        level.getChunkSource().randomState().sampler());
                if (biomes.contains(biome)) target = new Target(biome.unwrapKey().orElseThrow().location(), pos);
            }
            if (++height >= heights.length) { height = 0; grid.advance(); finished = grid.finished(); }
            return true;
        }
    }

    private static final class StructureScanner extends Scanner {
        final Deque<PlacementCursor> placements = new ArrayDeque<>();
        PlacementCursor pending;
        ChunkPos chunk;
        ChunkRequest request;
        StructureScanner(ServerLevel level, BlockPos origin) {
            super(level, origin);
        }
        @Override boolean step(LoadBudget budget) {
            if (pending == null) {
                if (placements.isEmpty()) { finished = true; return true; }
                pending = placements.removeFirst();
                chunk = null;
            }
            if (chunk == null) {
                if (!pending.ready(budget)) {
                    // 原版异步结果未就绪时仍轮换任务；仅在排序尚未完成时保留当前游标，以保持候选顺序。
                    if (pending.ringFuture != null && !pending.ringFuture.isDone()) {
                        placements.addLast(pending); pending = null;
                    }
                    return false;
                }
                chunk = pending.next();
                if (chunk == null) { pending = null; finished = placements.isEmpty(); return true; }
            }
            BlockPos locatePos = pending.placement.getLocatePos(chunk);
            if (((StructureRingsAccessor) level.getChunkSource().getGeneratorState()).travail$ringPositions().values().stream().anyMatch(f -> !f.isDone())) return false;
            if (within(locatePos) && pending.placement.isStructureChunk(level.getChunkSource().getGeneratorState(), chunk.x, chunk.z)) {
                if (request == null) {
                    if (budget.remaining == 0 || !REQUESTS.isEmpty()) return false;
                    budget.remaining--;
                    // 全服最多一个在途请求，每刻最多启动一次；绝不在搜索循环等待 I/O 或生成。
                    request = new ChunkRequest(level, chunk); REQUESTS.add(request);
                }
                if (!request.future.isDone()) return false;
                var access = request.future.getNow(null).left().orElse(null);
                request.release(); request = null;
                if (access != null) {
                    for (Holder<Structure> holder : pending.structures) {
                        var start = access.getStartForStructure(holder.value());
                        if (start != null && start.isValid()) {
                            target = new Target(holder.unwrapKey().orElseThrow().location(), locatePos); break;
                        }
                    }
                }
            }
            placements.addLast(pending); pending = null;
            return true;
        }
        @Override void close() { if (request != null) request.release(); }
    }

    private static final class PlacementCursor {
        final ServerLevel level;
        final StructurePlacement placement;
        final List<Holder<Structure>> structures;
        final WayguideGeometry.Spiral grid;
        final int baseX, baseZ, spacing;
        List<ChunkPos> rings;
        record Ring(ChunkPos pos, int order, double distance) {}
        PriorityQueue<Ring> sortedRings;
        final CompletableFuture<List<ChunkPos>> ringFuture;
        final BlockPos origin;
        int ringIndex;
        PlacementCursor(ServerLevel level, BlockPos origin, int radius, StructurePlacement placement, List<Holder<Structure>> structures) {
            this.level = level; this.placement = placement; this.structures = List.copyOf(structures);
            this.origin = origin;
            int cx = origin.getX() >> 4, cz = origin.getZ() >> 4;
            spacing = placement instanceof RandomSpreadStructurePlacement random ? random.spacing() : 1;
            baseX = Math.floorDiv(cx, spacing); baseZ = Math.floorDiv(cz, spacing);
            grid = new WayguideGeometry.Spiral((radius + 15) / 16 / spacing + 2);
            if (placement instanceof ConcentricRingsStructurePlacement concentric) {
                ringFuture = ((StructureRingsAccessor) level.getChunkSource().getGeneratorState()).travail$ringPositions().get(concentric);
                if (ringFuture == null) rings = List.of();
            } else ringFuture = null;
        }
        boolean ready(LoadBudget budget) {
            if (ringFuture == null) return true;
            if (!ringFuture.isDone()) return false;
            if (rings == null) {
                rings = ringFuture.getNow(List.of());
                sortedRings = new PriorityQueue<>(Comparator.comparingDouble(Ring::distance).thenComparingInt(Ring::order));
            }
            for (int i = 0; i < 32 && ringIndex < rings.size() && budget.take(); i++) {
                var pos = rings.get(ringIndex);
                sortedRings.add(new Ring(pos, ringIndex++, pos.getMiddleBlockPosition(origin.getY()).distSqr(origin)));
            }
            return ringIndex == rings.size();
        }
        ChunkPos next() {
            if (rings != null) return sortedRings == null || sortedRings.isEmpty() ? null : sortedRings.remove().pos();
            if (grid.finished()) return null;
            int x = (baseX + grid.x()) * spacing, z = (baseZ + grid.z()) * spacing;
            grid.advance();
            return placement instanceof RandomSpreadStructurePlacement random
                    ? random.getPotentialStructureChunk(level.getSeed(), x, z) : new ChunkPos(x, z);
        }
    }

    private WayguideSearch() {}
}

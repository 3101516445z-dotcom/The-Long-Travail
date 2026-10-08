package com.thelongtravail.data;

import com.mojang.datafixers.util.Either;
import com.thelongtravail.TheLongTravail;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.mixin.ChunkFutureInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.structure.Structure;
import java.util.*;
import java.util.concurrent.CompletableFuture;

// 记录观察位置后异步确认，所有世界访问均在服务端线程执行，不阻塞等待异步结果。
public final class JourneyVisits {
    private static final int MAX_PENDING = 256, MAX_PER_PLAYER = 16, STEPS_PER_TICK = 256;
    private static final long TIMEOUT_NANOS = 30_000_000_000L, TICK_BUDGET_NANOS = 1_000_000L;
    private static final TicketType<UUID> TICKET = TicketType.create("travail_journey_visit", UUID::compareTo);
    private static final Deque<Observation> PENDING = new ArrayDeque<>();
    private static Request inFlight;
    private record Identity(UUID journey, long revision) {
        static Identity of(ItemStack stack) {
            var identity = LongTravailData.queryIdentity(stack);
            return new Identity(identity.journey(), identity.revision());
        }
    }
    private static final class Observation {
        final ServerPlayer player;
        final ServerLevel level;
        final BlockPos position;
        final Map<Structure, ResourceLocation> targets;
        final Map<Long, Map<Structure, ResourceLocation>> missing = new LinkedHashMap<>();
        final long created = System.nanoTime();
        Identity identity;
        boolean references;
        Observation(ServerPlayer player, ItemStack stack, Map<Structure, ResourceLocation> targets) {
            this.player = player;
            level = player.serverLevel();
            position = player.blockPosition().immutable();
            identity = Identity.of(stack);
            this.targets = new LinkedHashMap<>(targets);
        }
        boolean valid() {
            return !player.isRemoved() && !player.hasDisconnected() && player.isAlive()
                    && player.level() == level && System.nanoTime() - created < TIMEOUT_NANOS
                    && identity.journey() != null && identity.equals(Identity.of(TravailCurios.stack(player)));
        }
        boolean pending() { return references || !missing.isEmpty(); }
    }
    private static final class Request {
        final Observation observation;
        final ChunkPos position;
        final boolean references;
        final int distance;
        final UUID ticket = UUID.randomUUID();
        final CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future;
        boolean released;
        Request(Observation observation, long packed, boolean references) {
            this.observation = observation;
            this.references = references;
            position = new ChunkPos(packed);
            ChunkStatus status = references ? ChunkStatus.STRUCTURE_REFERENCES : ChunkStatus.STRUCTURE_STARTS;
            distance = 33 - ChunkLevel.byStatus(status);
            var chunks = observation.level.getChunkSource();
            chunks.addRegionTicket(TICKET, position, distance, ticket);
            try {
                future = ((ChunkFutureInvoker)chunks).travail$requestChunk(position.x, position.z, status, true);
                HotPathMetrics.Counter.JOURNEY_REQUESTS.add(1);
            } catch (RuntimeException failure) { release(); throw failure; }
        }
        void release() {
            if (released) return;
            released = true;
            observation.level.getChunkSource().removeRegionTicket(TICKET, position, distance, ticket);
        }
    }
    public static List<ResourceLocation> observe(ServerPlayer player, ItemStack stack, Map<Structure, ResourceLocation> targets) {
        if (targets.isEmpty()) return List.of();
        Observation observation = new Observation(player, stack, targets);
        List<ResourceLocation> found = new ArrayList<>();
        var chunk = observation.level.getChunkSource().getChunkNow(observation.position.getX()>>4, observation.position.getZ()>>4);
        if (chunk == null) observation.references = true;
        else references(observation, chunk, found);
        if (observation.pending()) {
            // 相同位置和需求版本只保留一条记录；容量不足时释放被移除记录的区块票据，不取消原版共享的异步请求。
            boolean duplicate = PENDING.stream().anyMatch(old -> old.player == player && old.level == observation.level
                    && old.position.equals(observation.position) && old.identity.equals(observation.identity));
            if (!duplicate) {
                if (PENDING.stream().filter(old -> old.player == player).count() >= MAX_PER_PLAYER) {
                    for (var old : PENDING) if (old.player == player) { remove(old); break; }
                }
                if (PENDING.size() >= MAX_PENDING) remove(PENDING.peekFirst());
                PENDING.addLast(observation);
            }
        }
        return found;
    }
    static List<ResourceLocation> loadedAt(ServerLevel level, BlockPos position, Map<Structure, ResourceLocation> targets) {
        if (targets.isEmpty()) return List.of();
        var origin = level.getChunkSource().getChunkNow(position.getX()>>4, position.getZ()>>4);
        if (origin == null) return List.of();
        List<ResourceLocation> found = new ArrayList<>();
        inspectReferences(level, position, targets, origin, null, found);
        return found;
    }
    private static void references(Observation observation, ChunkAccess origin, List<ResourceLocation> found) {
        observation.references = false;
        inspectReferences(observation.level, observation.position, observation.targets, origin, observation.missing, found);
    }
    private static void inspectReferences(ServerLevel level, BlockPos position, Map<Structure, ResourceLocation> targets,
            ChunkAccess origin, Map<Long, Map<Structure, ResourceLocation>> missing, List<ResourceLocation> found) {
        var refs = origin.getAllReferences();
        for (var target : targets.entrySet()) {
            var starts = refs.get(target.getKey());
            if (starts == null) continue;
            for (long packed : starts) {
                var chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(packed), ChunkPos.getZ(packed));
                if (chunk == null) {
                    if (missing != null) missing.computeIfAbsent(packed, ignored -> new LinkedHashMap<>()).put(target.getKey(), target.getValue());
                } else if (matches(level, position, chunk, target.getKey())) {
                    found.add(target.getValue());
                    break;
                }
            }
        }
        // 若已通过加载完成的起点确认同一结构，就不再为其他起点重复申请区块。
        if (missing != null && !found.isEmpty()) {
            missing.values().forEach(values -> values.values().removeIf(found::contains));
            missing.values().removeIf(Map::isEmpty);
        }
    }
    static boolean matches(ServerLevel level, BlockPos position, ChunkAccess chunk, Structure type) {
        var start = chunk.getStartForStructure(type);
        return start != null && start.isValid() && level.structureManager().structureHasPieceAt(position, start);
    }
    // 仅本模块确认的进度变化可以更新待处理任务的版本，外部修改仍使原任务失效。
    public static void rebase(ServerPlayer player, LongTravailData.RequirementIdentity before, ItemStack after) {
        Identity old = new Identity(before.journey(), before.revision());
        Identity next = Identity.of(after);
        for (Observation observation : PENDING)
            if (observation.player == player && observation.identity.equals(old)) observation.identity = next;
    }
    private static void complete(Request request) {
        Observation observation = request.observation;
        if (!PENDING.contains(observation) || !observation.valid()) return;
        var chunk = request.future.getNow(null).left().orElse(null);
        List<ResourceLocation> found = new ArrayList<>();
        if (request.references) {
            observation.references = false;
            if (chunk != null) references(observation, chunk, found);
        } else {
            var targets = observation.missing.remove(request.position.toLong());
            if (chunk != null && targets != null) for (var target : targets.entrySet())
                if (matches(observation.level, observation.position, chunk, target.getKey())) found.add(target.getValue());
        }
        ItemStack diary = TravailCurios.stack(observation.player);
        var before = LongTravailData.queryIdentity(diary);
        for (ResourceLocation id : found) if (LongTravailData.visitStructure(diary, id))
            HotPathMetrics.Counter.JOURNEY_CONFIRMED.add(1);
        rebase(observation.player, before, diary);
        if (!observation.pending()) remove(observation);
    }
    public static void tick() {
        long deadline = System.nanoTime() + TICK_BUDGET_NANOS;
        if (inFlight != null && inFlight.future.isDone()) {
            Request finished = inFlight;
            inFlight = null;
            try { complete(finished); }
            catch (RuntimeException failure) {
                remove(finished.observation);
                TheLongTravail.LOGGER.warn("Structure visit confirmation failed at {}", finished.observation.position, failure);
            } finally { finished.release(); }
        }
        int steps = Math.min(STEPS_PER_TICK, PENDING.size());
        boolean started = false;
        while (steps-- > 0 && !PENDING.isEmpty() && System.nanoTime() < deadline) {
            Observation observation = PENDING.removeFirst();
            if (!observation.valid()) {
                cancelRequest(observation);
                HotPathMetrics.Counter.JOURNEY_EXPIRED.add(1);
                continue;
            }
            PENDING.addLast(observation);
            if (inFlight != null || started) continue;
            long chunk = observation.references ? ChunkPos.asLong(observation.position) : observation.missing.keySet().iterator().next();
            try {
                started = true;
                inFlight = new Request(observation, chunk, observation.references);
            } catch (RuntimeException failure) {
                remove(observation);
                TheLongTravail.LOGGER.warn("Unable to request structure visit chunk {}", chunk, failure);
            }
            // 提交请求后立即轮换到下一名观察者，避免包含多个起点的任务独占在途名额。
            break;
        }
    }
    private static void cancelRequest(Observation observation) {
        if (inFlight != null && inFlight.observation == observation) inFlight.release();
        // 未完成的共享请求仍占用在途名额，避免取消和重试导致生成请求无限累积。
    }
    private static void remove(Observation observation) { PENDING.remove(observation); cancelRequest(observation); }
    public static void forget(ServerPlayer player) {
        for (Observation observation : List.copyOf(PENDING)) if (observation.player == player) remove(observation);
    }
    public static void clear() {
        invalidate();
        inFlight = null;
    }
    public static void invalidate() {
        PENDING.clear();
        if (inFlight != null) inFlight.release();
    }
    private JourneyVisits() {}
}

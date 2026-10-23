package com.thelongtravail.boundless;

import com.mojang.datafixers.util.Either;
import com.thelongtravail.mixin.ChunkFutureInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class DreamLandingSearch {
    public static final int RADIUS = 16;
    private static final TicketType<UUID> TICKET =
            TicketType.create("travail_dream_landing", UUID::compareTo);
    private static final Map<ServerPlayer, Search> SEARCHES = new IdentityHashMap<>();
    private static final Deque<Search> ORDER = new ArrayDeque<>();
    private static Request inFlight;
    // 取消搜索时保留原版共享的异步请求；遗留请求最多一个，与活动请求合计最多两个。
    private static Request retired;
    private static int requestTick = Integer.MIN_VALUE;

    private static final class Request {
        final Search search;
        final ChunkPos chunk;
        final CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future;

        Request(Search search, ChunkPos chunk) {
            this.search = search;
            this.chunk = chunk;
            var source = search.level.getChunkSource();
            source.addRegionTicket(TICKET, chunk, 0, search.ticket);
            search.pins.add(chunk);
            future =
                    ((ChunkFutureInvoker) source)
                            .travail$requestChunk(chunk.x, chunk.z, ChunkStatus.FULL, true);
        }

        boolean active() {
            return SEARCHES.get(search.player) == search && search.valid();
        }
    }

    static final class Search {
        final ServerPlayer player;
        final ServerLevel level;
        final CompoundTag record;
        final Vec3 anchor;
        final UUID ticket = UUID.randomUUID();
        final Set<ChunkPos> pins = new HashSet<>();
        final PriorityQueue<Vec3> candidates;
        final Set<BlockPos> seen = new HashSet<>();
        int retryAfter;
        boolean anchorChecked, done, exhausted;
        Vec3 result;

        Search(ServerPlayer player, CompoundTag record, Vec3 anchor) {
            this.player = player;
            this.level = player.serverLevel();
            this.record = record;
            this.anchor = anchor;
            if (!Double.isFinite(anchor.lengthSqr())
                    || Math.abs(anchor.x) > 30_000_000
                    || Math.abs(anchor.z) > 30_000_000
                    || Math.abs(anchor.y) > 30_000_000)
                throw new IllegalArgumentException("Invalid dream anchor");
            candidates =
                    new PriorityQueue<>(
                            Comparator.<Vec3>comparingDouble(anchor::distanceToSqr)
                                    .thenComparingDouble(v -> v.z)
                                    .thenComparingDouble(v -> v.x)
                                    .thenComparingDouble(v -> v.y));
            offer(
                    new BlockPos(
                            net.minecraft.util.Mth.floor(anchor.x),
                            net.minecraft.util.Mth.floor(anchor.y + .5),
                            net.minecraft.util.Mth.floor(anchor.z)));
        }

        void offer(BlockPos pos) {
            if (!seen.add(pos)) return;
            Vec3 point = Vec3.atBottomCenterOf(pos);
            if (anchor.distanceToSqr(point) <= RADIUS * RADIUS) candidates.add(point);
        }

        boolean valid() {
            return DreamState.active(player)
                    && player.getPersistentData().getCompound(DreamState.RECORD) == record
                    && player.serverLevel() == level
                    && player.isSpectator()
                    && !player.isRemoved();
        }

        void release() {
            for (var chunk : pins)
                level.getChunkSource().removeRegionTicket(TICKET, chunk, 0, ticket);
            pins.clear();
        }

        boolean prepare(Vec3 point) {
            var box = player.getDimensions(Pose.STANDING).makeBoundingBox(point);
            for (int cx = net.minecraft.util.Mth.floor(box.minX) >> 4;
                    cx <= net.minecraft.util.Mth.floor(box.maxX) >> 4;
                    cx++)
                for (int cz = net.minecraft.util.Mth.floor(box.minZ) >> 4;
                        cz <= net.minecraft.util.Mth.floor(box.maxZ) >> 4;
                        cz++) {
                    var chunk = new ChunkPos(cx, cz);
                    if (level.hasChunk(cx, cz)) {
                        if (pins.add(chunk))
                            level.getChunkSource().addRegionTicket(TICKET, chunk, 0, ticket);
                        continue;
                    }
                    int now = level.getServer().getTickCount();
                    if (inFlight == null && requestTick != now && now >= retryAfter) {
                        requestTick = now;
                        inFlight = new Request(this, chunk);
                    }
                    return false;
                }
            return true;
        }

        boolean step() {
            if (!anchorChecked) {
                if (!prepare(anchor)) return false;
                boolean safe = DreamState.safe(player, anchor);
                anchorChecked = true;
                if (safe) {
                    done = true;
                    result = anchor;
                }
                return true;
            }
            if (candidates.isEmpty()) {
                done = true;
                exhausted = true;
                result = anchor;
                return true;
            }
            Vec3 point = candidates.peek();
            if (!prepare(point)) return false;
            boolean safe = DreamState.safe(player, point);
            candidates.remove();
            BlockPos cell = BlockPos.containing(point);
            for (Direction direction : Direction.values()) offer(cell.relative(direction));
            if (safe) {
                done = true;
                result = point;
            }
            return true;
        }
    }

    static Vec3 result(ServerPlayer player, CompoundTag record, Vec3 anchor) {
        Search search = SEARCHES.get(player);
        if (search == null
                || search.record != record
                || search.level != player.serverLevel()
                || !search.anchor.equals(anchor)) {
            cancel(player);
            search = new Search(player, record, anchor);
            SEARCHES.put(player, search);
            ORDER.addLast(search);
        }
        if (!search.done) return null;
        if (!search.exhausted && !DreamState.safe(player, search.result)) {
            search.done = false;
            search.result = null;
            if (!ORDER.contains(search)) ORDER.addLast(search);
            return null;
        }
        return search.result;
    }

    public static void cancel(ServerPlayer player) {
        Search search = SEARCHES.remove(player);
        if (search != null) {
            ORDER.remove(search);
            search.release();
        }
        retireInvalidRequest();
    }

    private static void retireInvalidRequest() {
        if (retired != null && retired.future.isDone()) retired = null;
        if (inFlight == null || inFlight.active()) return;
        if (inFlight.future.isDone()) inFlight = null;
        else if (retired == null) {
            retired = inFlight;
            inFlight = null;
        }
    }

    public static void clear() {
        for (var search : SEARCHES.values()) search.release();
        SEARCHES.clear();
        ORDER.clear();
        inFlight = null;
        retired = null;
        requestTick = Integer.MIN_VALUE;
    }

    public static void tick(MinecraftServer server) {
        retireInvalidRequest();
        if (inFlight != null && inFlight.future.isDone()) {
            Request request = inFlight;
            inFlight = null;
            // 异步请求可能在失效检查后才完成；迟到的结果不得恢复已取消的搜索。
            if (request.active())
                try {
                    if (request.future.join().left().isEmpty())
                        throw new IllegalStateException("Dream chunk unavailable");
                } catch (RuntimeException failure) {
                    request.search.retryAfter = server.getTickCount() + 20;
                    com.thelongtravail.TheLongTravail.LOGGER.warn(
                            "Dream landing chunk request failed at {}; will retry",
                            request.chunk,
                            failure);
                }
        }
        long deadline = System.nanoTime() + 2_000_000L;
        int budget = 2048, blocked = 0;
        while (budget > 0 && !ORDER.isEmpty() && System.nanoTime() < deadline) {
            Search search = ORDER.removeFirst();
            if (!search.valid()) {
                cancel(search.player);
                continue;
            }
            if (TimeStopManager.frozen(search.player)
                    || server.getTickCount() < search.retryAfter) {
                ORDER.addLast(search);
                if (++blocked >= ORDER.size()) break;
                continue;
            }
            boolean progressed = false;
            try {
                for (int i = 0;
                        i < 32 && budget > 0 && !search.done && System.nanoTime() < deadline;
                        i++, budget--) {
                    if (!search.step()) break;
                    progressed = true;
                }
            } catch (RuntimeException failure) {
                // 发生异常不代表候选位置已经查尽；须保留记录和候选位置，等待下次重试。
                search.retryAfter = server.getTickCount() + 20;
                com.thelongtravail.TheLongTravail.LOGGER.warn(
                        "Dream landing search deferred for {}", search.player.getUUID(), failure);
            }
            if (search.done) {
                search.record.putInt("retryDelay", 0);
                DreamState.finish(search.player, search.record.getBoolean("notifyWake"));
            } else {
                ORDER.addLast(search);
                blocked = progressed ? 0 : blocked + 1;
                if (blocked >= ORDER.size()) break;
            }
        }
    }

    private DreamLandingSearch() {}
}

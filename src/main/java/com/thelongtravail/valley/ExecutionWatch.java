package com.thelongtravail.valley;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.network.TravailNetwork;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class ExecutionWatch {
    private static final Map<Level, State> WATCHED = new WeakHashMap<>();

    private static final class Entry {
        final WeakReference<LivingEntity> entity;
        final ExecutionLease lease;
        long nextProbe, nextDeep, lastSync = -100;
        int restorations, failures, quiet;

        Entry(LivingEntity entity, long now) {
            this.entity = new WeakReference<>(entity);
            this.lease = ((ExecutionBody) entity).travail$executionLease();
            nextDeep = now + 1 + Math.floorMod(entity.getId(), 100);
        }
    }

    private static final class State {
        final List<Entry> entries = new ArrayList<>();
        Entry[] snapshot = new Entry[0];
        boolean dirty;
        long clock, fast, deep, repairs, failures, nanos;
    }

    public record Diagnostics(long polls, long fastChecks, long deepChecks, long repairs, long failures, long nanos) {}

    public static synchronized Diagnostics diagnostics(Level level) {
        var state = WATCHED.get(level);
        return state == null
                ? new Diagnostics(0, 0, 0, 0, 0, 0)
                : new Diagnostics(state.clock, state.fast, state.deep, state.repairs, state.failures, state.nanos);
    }

    public static synchronized void track(LivingEntity entity) {
        if (entity instanceof Player) return;
        var body = (ExecutionBody) entity;
        if (body.travail$executionLease() == null)
            body.travail$executionLease(ExecutionLease.start(entity.level().getGameTime()));
        if (body.travail$executionLease().expired(entity.level().getGameTime())) return;
        var state = WATCHED.computeIfAbsent(entity.level(), ignored -> new State());
        for (var entry : state.entries) if (entry.entity.get() == entity) return;
        state.entries.add(new Entry(entity, state.clock));
        state.dirty = true;
    }

    public static void sweep(Level level) {
        run(level, true);
    }

    public static void poll(Level level) {
        run(level, false);
    }

    private static void run(Level level, boolean force) {
        if (level instanceof ServerLevel server && !server.getServer().isSameThread()) return;
        State state;
        Entry[] entries;
        synchronized (ExecutionWatch.class) {
            state = WATCHED.get(level);
            if (state == null) return;
            if (!force) state.clock++;
            if (state.entries.removeIf(entry -> entry.lease.expired(level.getGameTime()))) state.dirty = true;
            if (state.dirty || state.clock % 100 == 0) {
                state.entries.removeIf(entry -> entry.entity.get() == null);
                if (state.entries.isEmpty()) {
                    WATCHED.remove(level);
                    return;
                }
                state.snapshot = state.entries.toArray(Entry[]::new);
                state.dirty = false;
            }
            entries = state.snapshot;
        }
        long started = System.nanoTime(), now = state.clock;
        try {
            Set<net.minecraft.world.entity.Entity> deepTargets = Collections.newSetFromMap(new IdentityHashMap<>());
            for (var entry : entries) {
                var entity = entry.entity.get();
                if ((force || now >= entry.nextProbe && now >= entry.nextDeep)
                        && entity != null
                        && entity.level() == level
                        && ExecutionCleanup.quarantined(entity)
                        && !FinalExecution.cleanupPending(entity)) deepTargets.add(entity);
            }
            Set<net.minecraft.world.entity.Entity> present = null;
            if (deepTargets.size() > 1) {
                try {
                    present = ((ExecutionStorage) level).travail$present(deepTargets);
                } catch (RuntimeException failure) {
                    TheLongTravail.LOGGER.warn("Execution batch audit unavailable; using individual checks", failure);
                }
            }
            for (var entry : entries) {
                if (!force && now < entry.nextProbe) continue;
                var entity = entry.entity.get();
                if (entity == null || entity.level() != level || !ExecutionCleanup.quarantined(entity)) continue;
                boolean full = force || now >= entry.nextDeep;
                try {
                    if (!level.isClientSide && FinalExecution.cleanupPending(entity)) {
                        state.deep++;
                        present = null; // 清理可能触发第三方回调，后续目标不能复用旧快照。
                        FinalExecution.retryCleanup(entity);
                    } else {
                        if (full) {
                            state.deep++;
                            entry.nextDeep = now + 100;
                        } else state.fast++;
                        boolean detached =
                                full && present != null && deepTargets.contains(entity)
                                        ? ((com.thelongtravail.mixin.ExecutionRawEntity) entity).travail$reason()
                                                        == net.minecraft.world.entity.Entity.RemovalReason.KILLED
                                                && !present.contains(entity)
                                        : ExecutionCleanup.detached(entity, full);
                        if ((level.isClientSide && FinalExecution.cleanupPending(entity)) || !detached) {
                            present = null;
                            ExecutionCleanup.rawDetach(entity, full);
                            entry.lease.restored();
                            if (level.isClientSide) {
                                ((ExecutionBody) entity).travail$executionTerminated(true);
                                ((ExecutionBody) entity).travail$cleanupPending(false);
                            }
                            state.repairs++;
                            entry.restorations++;
                            entry.quiet = 0;
                            if (!level.isClientSide && now - entry.lastSync >= 100) {
                                TravailNetwork.sendExecutionTermination(entity);
                                entry.lastSync = now;
                            }
                            if (entry.restorations == 1 || entry.restorations % 200 == 0)
                                TheLongTravail.LOGGER.info(
                                        "Execution reinsertion repaired side={} uuid={} count={}",
                                        level.isClientSide ? "client" : "server",
                                        entity.getUUID(),
                                        entry.restorations);
                            entry.nextProbe = now + 1;
                            entry.failures = 0;
                            continue;
                        }
                    }
                    entry.failures = 0;
                    entry.quiet = Math.min(5, entry.quiet + 1);
                    entry.nextProbe = Math.min(entry.nextDeep, now + Math.min(20, 1 << entry.quiet));
                } catch (RuntimeException ex) {
                    entry.lease.restored();
                    state.failures++;
                    entry.failures++;
                    entry.nextProbe = now + Math.min(200, 1 << Math.min(entry.failures, 8));
                    if (entry.failures == 1 || entry.failures % 200 == 0)
                        TheLongTravail.LOGGER.warn("Execution repair deferred for " + entity.getUUID(), ex);
                }
            }
        } finally {
            state.nanos += System.nanoTime() - started;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(TickEvent.LevelTickEvent event) {
        if (!event.level.isClientSide && event.phase == TickEvent.Phase.END) poll(event.level);
    }

    @SubscribeEvent
    public static synchronized void unload(LevelEvent.Unload event) {
        WATCHED.remove(event.getLevel());
    }

    private ExecutionWatch() {}
}

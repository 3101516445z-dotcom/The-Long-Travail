package com.thelongtravail.valley;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;

public final class FinalExecution {
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();

    private static final class Frame {
        final LivingEntity target;
        final DamageSource source;
        boolean deathStarted, entered, admitted;

        Frame(LivingEntity target, DamageSource source) {
            this.target = target;
            this.source = source;
        }
    }

    public static boolean locked(LivingEntity entity) {
        return ((ExecutionBody) entity).travail$executionLocked();
    }

    public static void sealDeath(LivingEntity entity) {
        ((ExecutionBody) entity).travail$executionLocked(true);
    }

    public static boolean enterDeath(LivingEntity target, DamageSource source) {
        if (!locked(target)) return true;
        Frame frame = CURRENT.get();
        if (frame == null || frame.target != target || frame.source != source || frame.entered) return false;
        frame.entered = true;
        return true;
    }

    public static boolean bypassDeathCheck(LivingEntity target, DamageSource source) {
        Frame frame = CURRENT.get();
        if (frame == null || frame.target != target || frame.source != source || !frame.entered) return false;
        frame.admitted = true;
        AzraelExecution.deathResult(target, source, false);
        return true;
    }

    public static boolean terminated(LivingEntity target) {
        return ((ExecutionBody) target).travail$executionTerminated();
    }

    public static boolean cleanupPending(LivingEntity target) {
        return ((ExecutionBody) target).travail$cleanupPending();
    }

    public static boolean retryCleanup(LivingEntity target) {
        if (target instanceof net.minecraft.world.entity.player.Player || !cleanupPending(target)) return false;
        if (CURRENT.get() != null) return false;
        if (target.level().isClientSide || !target.level().getServer().isSameThread())
            throw new IllegalStateException("Cleanup must run on server thread");
        Frame frame = new Frame(target, target.damageSources().genericKill());
        CURRENT.set(frame);
        try (var lootObservation = ExecutionLoot.observe(target)) {
            terminate(target, VanillaExecution.resolve());
            return true;
        } finally {
            CURRENT.remove();
        }
    }

    private static void terminate(LivingEntity target, VanillaExecution vanilla) {
        var body = (ExecutionBody) target;
        body.travail$cleanupPending(true);
        ExecutionWatch.track(target);
        boolean stageFive = false;
        RuntimeException removalFailure = null;
        try {
            vanilla.remove(target);
        } catch (RuntimeException ex) {
            removalFailure = ex;
        }
        if (!ExecutionCleanup.detached(target) || body.travail$rawCleanupRequired()) {
            stageFive = true;
            body.travail$rawCleanupRequired(true);
            try {
                ExecutionCleanup.finishServer(target);
                body.travail$rawCleanupRequired(false);
            } catch (RuntimeException | Error failure) {
                if (removalFailure != null && removalFailure != failure) failure.addSuppressed(removalFailure);
                throw failure;
            }
        }
        if (removalFailure != null)
            com.thelongtravail.TheLongTravail.LOGGER.warn(
                    "Vanilla removal failed; cleanup recovered {}", target.getUUID(), removalFailure);
        com.thelongtravail.network.TravailNetwork.sendExecutionTermination(target);
        body.travail$executionTerminated(true);
        body.travail$cleanupPending(false);
        ExecutionLoot.finish(target);
        com.thelongtravail.TheLongTravail.LOGGER.info(
                "Final execution terminated stage={} entity={} uuid={} class={}; custom death/drop completion is not guaranteed",
                stageFive ? 5 : 4,
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()),
                target.getUUID(),
                target.getClass().getName());
    }

    public static boolean commit(LivingEntity target, DamageSource source) {
        if (CURRENT.get() != null || locked(target)) return false;
        if (target.level().isClientSide || !target.level().getServer().isSameThread())
            throw new IllegalStateException("Execution must run on the server thread");
        boolean player = target instanceof net.minecraft.world.entity.player.Player;
        VanillaExecution vanilla = player ? null : VanillaExecution.resolve();
        var body = (ExecutionBody) target;
        var data = (ExecutionData) target.getEntityData();
        float before = data.travail$storedHealth();
        Frame frame = new Frame(target, source);
        CURRENT.set(frame);
        body.travail$executionLocked(true);
        try (var lootObservation = ExecutionLoot.observe(target)) {
            ExecutionLoot.arm(target);
            data.travail$commitHealth(0);
            if (data.travail$storedHealth() != 0 || player && target.getHealth() != 0) return false;
            frame.deathStarted = true;
            if (!player) {
                // 不能先调用第三方重写的 die：它可能先生成掉落，再拒绝死亡。
                try {
                    vanilla.die(target, source);
                } catch (RuntimeException | Error failure) {
                    ExecutionLoot.abort(target);
                    try {
                        terminate(target, vanilla);
                    } catch (RuntimeException | Error removalFailure) {
                        failure.addSuppressed(removalFailure);
                    }
                    throw failure;
                }
                terminate(target, vanilla);
                return true;
            }
            target.die(source);
            boolean completed = frame.admitted && data.travail$storedHealth() == 0 && target.getHealth() == 0;
            if (!completed)
                com.thelongtravail.TheLongTravail.LOGGER.warn(
                        "Final execution incomplete entity={} uuid={} admitted={} storedHealth={}; retaining old-instance lock",
                        target.getType(),
                        target.getUUID(),
                        frame.admitted,
                        data.travail$storedHealth());
            return completed;
        } finally {
            try {
                // die 开始执行后，不得恢复实体或重试死亡，避免重复掉落。
                if (!frame.deathStarted) {
                    body.travail$executionLoot(null);
                    body.travail$executionLocked(false);
                    data.travail$commitHealth(before);
                }
            } finally {
                CURRENT.remove();
            }
        }
    }

    private FinalExecution() {}
}

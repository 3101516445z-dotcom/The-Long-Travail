package com.thelongtravail.valley;

import com.thelongtravail.config.AzraelConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;

public final class AzraelExecution {
    public static final ResourceKey<DamageType> TYPE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation("the_long_travail", "azrael_execution"));
    public static final ResourceKey<DamageType> SELF =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation("the_long_travail", "azrael_self"));
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();

    private static final class Frame {
        final LivingEntity target;
        DamageSource source;
        boolean acceptedDeath, cancelledDeath, invalidDamage;
        final StringBuilder diagnostic;

        Frame(LivingEntity target) {
            this.target = target;
            diagnostic = beginDiagnostic(target);
        }

        void begin(DamageSource source) {
            this.source = source;
            acceptedDeath = cancelledDeath = invalidDamage = false;
        }
    }

    public enum Result {
        DEAD,
        SURVIVED,
        INVALID
    }

    private enum Attempt {
        DEAD,
        REMOVED,
        NONFINITE,
        CANCELLED,
        NO_DEATH
    }

    private static final boolean DIAGNOSTICS = Boolean.getBoolean("travail.execution.diagnostics");
    private static final java.util.Map<ResourceLocation, Long> NEXT_REPORT =
            new java.util.HashMap<>();

    public static void clearDiagnostics() {
        NEXT_REPORT.clear();
    }

    private static StringBuilder beginDiagnostic(LivingEntity target) {
        if (!DIAGNOSTICS) return null;
        ResourceLocation type =
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(
                        target.getType());
        long now = System.nanoTime();
        Long next = NEXT_REPORT.get(type);
        if (next != null && now - next < 0) return null;
        NEXT_REPORT.put(type, now + 60_000_000_000L);
        return new StringBuilder().append("entity=").append(type);
    }

    private static void report(Frame frame, String stage, int number, Attempt result) {
        if (frame.diagnostic != null)
            frame.diagnostic
                    .append("; ")
                    .append(stage)
                    .append('#')
                    .append(number)
                    .append('=')
                    .append(result);
    }

    private static Attempt outcome(Frame frame) {
        // 死亡状态已在修复前确定；修复后不要为了诊断分类再次读取第三方生命值。
        if (frame.target.isRemoved()) return Attempt.REMOVED;
        if (frame.invalidDamage) return Attempt.NONFINITE;
        return frame.cancelledDeath ? Attempt.CANCELLED : Attempt.NO_DEATH;
    }

    public static boolean active() {
        return CURRENT.get() != null;
    }

    public static boolean matches(LivingEntity target, DamageSource source) {
        Frame frame = CURRENT.get();
        return frame != null && frame.target == target && frame.source == source;
    }

    public static void deathResult(LivingEntity target, DamageSource source, boolean cancelled) {
        if (!matches(target, source)) return;
        Frame frame = CURRENT.get();
        frame.cancelledDeath = cancelled;
        frame.acceptedDeath = !cancelled;
    }

    private static DamageSource source(
            LivingEntity player, LivingEntity target, ResourceKey<DamageType> type) {
        var holder =
                target.level()
                        .registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(type);
        boolean self = target == player;
        return new DamageSource(holder, self ? null : player, self ? null : player) {
            @Override
            public net.minecraft.network.chat.Component getLocalizedDeathMessage(
                    LivingEntity victim) {
                return net.minecraft.network.chat.Component.translatable(
                        "death.attack.the_long_travail."
                                + (self ? "azrael_self" : "azrael_execution"),
                        victim.getDisplayName());
            }
        };
    }

    private static boolean dead(Frame frame) {
        LivingEntity target = frame.target;
        return target.getRemovalReason() == Entity.RemovalReason.KILLED
                || frame.acceptedDeath
                        && Float.isFinite(target.getHealth())
                        && target.getHealth() <= 0;
    }

    // 每次调用前须先记录伤害归属；第三阶段处决不能依赖仅在 hurt 成功后写入的记录。
    private static void credit(LivingEntity player, LivingEntity target) {
        if (player == null || target == player) return;
        if (!(player instanceof ServerPlayer)) target.setLastHurtByMob(player);
        if (player instanceof ServerPlayer p) {
            target.setLastHurtByPlayer(p);
            ((com.thelongtravail.mixin.AzraelLivingAccessor) target).travail$recentPlayerHit(100);
        }
    }

    private static float snapshotHealth(LivingEntity target) {
        float value = target.getHealth();
        if (Float.isFinite(value) && value > 0) return value;
        float maximum = target.getMaxHealth();
        return Float.isFinite(maximum) ? Math.max(1, maximum) : 1;
    }

    private static void repairAfterFailure(
            Frame frame, float health, float absorption, Throwable failure) {
        // 不追加处决，也不吞掉原异常；修复失败时将其异常附加到原异常，避免掩盖第三方故障。
        try {
            if (!frame.target.isRemoved()) {
                repair(frame, health, absorption);
                if (!frame.acceptedDeath && frame.target.getHealth() <= 0)
                    frame.target.setHealth(health);
            }
        } catch (RuntimeException | Error repairFailure) {
            if (repairFailure != failure) failure.addSuppressed(repairFailure);
        }
    }

    private static void repair(Frame frame, float health, float absorption) {
        var target = frame.target;
        // NaN 不表示死亡；死亡事件已被取消时，即使生命值尚未恢复，也应视为存活。
        if (!Float.isFinite(target.getHealth()) || frame.cancelledDeath && target.getHealth() <= 0)
            target.setHealth(health);
        if (!Float.isFinite(target.getAbsorptionAmount()))
            target.setAbsorptionAmount(Float.isFinite(absorption) ? absorption : 0);
    }

    private static Attempt hit(
            Frame frame, LivingEntity player, DamageSource source, float amount) {
        var target = frame.target;
        float health = snapshotHealth(target), absorption = target.getAbsorptionAmount();
        frame.begin(source);
        target.invulnerableTime = 0;
        credit(player, target);
        try {
            target.hurt(source, amount);
        } catch (RuntimeException | Error failure) {
            repairAfterFailure(frame, health, absorption, failure);
            throw failure;
        }
        if (dead(frame)) {
            FinalExecution.sealDeath(target);
            return Attempt.DEAD;
        }
        frame.invalidDamage =
                !Float.isFinite(target.getHealth())
                        || !Float.isFinite(target.getAbsorptionAmount());
        if (frame.invalidDamage)
            com.thelongtravail.data.HotPathMetrics.Counter.EXECUTION_NONFINITE.add(1);
        repair(frame, health, absorption);
        return outcome(frame);
    }

    private static Attempt infiniteDamage(Frame frame, LivingEntity player, DamageSource source) {
        com.thelongtravail.data.HotPathMetrics.Counter.EXECUTION_INFINITE.add(1);
        return hit(frame, player, source, Float.POSITIVE_INFINITY);
    }

    private static Attempt genericKill(Frame frame, LivingEntity player) {
        com.thelongtravail.data.HotPathMetrics.Counter.EXECUTION_GENERIC.add(1);
        return hit(
                frame,
                player,
                source(player, frame.target, DamageTypes.GENERIC_KILL),
                Float.MAX_VALUE);
    }

    private static Attempt directDeath(Frame frame, LivingEntity player, DamageSource execution) {
        com.thelongtravail.data.HotPathMetrics.Counter.EXECUTION_DIRECT.add(1);
        var target = frame.target;
        float health = snapshotHealth(target);
        frame.begin(execution);
        credit(player, target);
        target.getCombatTracker().recordDamage(execution, health);
        return FinalExecution.commit(target, execution) ? Attempt.DEAD : Attempt.NO_DEATH;
    }

    public static Result execute(ServerPlayer player, LivingEntity target) {
        return execute((LivingEntity) player, target);
    }

    public static Result execute(LivingEntity player, LivingEntity target) {
        return execute(player, target, false);
    }

    public static Result executeForCommand(LivingEntity actor, LivingEntity target) {
        return execute(actor, target, true);
    }

    private static Result execute(LivingEntity player, LivingEntity target, boolean command) {
        var rule =
                target == player
                        ? com.thelongtravail.config.ModeEffectRules.AZRAEL_SELF
                        : com.thelongtravail.config.ModeEffectRules.AZRAEL_TARGET;
        if (!command && !rule.applies(target)) return Result.INVALID;
        if (!target.level().isClientSide
                && FinalExecution.cleanupPending(target)
                && !active()
                && AzraelConfig.DEATH_FALLBACK.get())
            return FinalExecution.retryCleanup(target) ? Result.DEAD : Result.SURVIVED;
        if (target.level().isClientSide
                || FinalExecution.locked(target)
                || active()
                || target.isRemoved()
                || !target.isAlive()
                || !command && (player == null || target.level() != player.level()))
            return Result.INVALID;
        // 随机判定后，PvP 开关或队伍关系仍可能变化；在本游戏刻末尾执行第三阶段处决前须重新检查。
        if (!command
                && target != player
                && target instanceof net.minecraft.world.entity.player.Player other
                && !com.thelongtravail.compat.maid.MaidAccessories.canHarm(player, other))
            return Result.INVALID;
        var soulClaim = com.thelongtravail.underworld.SoulBannerKills.capture(target, player);
        Frame frame = new Frame(target);
        CURRENT.set(frame);
        try (var lootObservation = ExecutionLoot.observe(target)) {
            DamageSource execution = source(player, target, target == player ? SELF : TYPE);
            for (int i = 0; i < AzraelConfig.ATTEMPTS.get(); i++) {
                Attempt result = infiniteDamage(frame, player, execution);
                report(frame, "infinite", i + 1, result);
                if (result == Attempt.DEAD) {
                    com.thelongtravail.underworld.SoulBannerKills.executed(target, soulClaim);
                    return Result.DEAD;
                }
                if (result == Attempt.REMOVED) return Result.INVALID;
                if (result == Attempt.NONFINITE) break;
            }
            if (AzraelConfig.KILL_FALLBACK.get()) {
                Attempt result = genericKill(frame, player);
                report(frame, "generic_kill", 1, result);
                if (result == Attempt.DEAD) {
                    com.thelongtravail.underworld.SoulBannerKills.executed(target, soulClaim);
                    return Result.DEAD;
                }
                if (result == Attempt.REMOVED) return Result.INVALID;
            }
            if (AzraelConfig.DEATH_FALLBACK.get()) {
                Attempt result = directDeath(frame, player, execution);
                report(frame, "direct_death", 1, result);
                if (result == Attempt.DEAD) {
                    com.thelongtravail.underworld.SoulBannerKills.executed(target, soulClaim);
                    return Result.DEAD;
                }
            }
            return Result.SURVIVED;
        } finally {
            CURRENT.remove();
            if (frame.diagnostic != null)
                com.thelongtravail.TheLongTravail.LOGGER.info(
                        "Azrael execution: {}", frame.diagnostic);
        }
    }

    private AzraelExecution() {}
}

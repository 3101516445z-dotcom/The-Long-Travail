package com.thelongtravail.valley;

import com.thelongtravail.config.AzraelConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;

// 只调用正常伤害/死亡流程；不会删除实体、补发掉落或强行取消其他模组的复活。
public final class AzraelExecution {
    public static final ResourceKey<DamageType> TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation("the_long_travail", "azrael_execution"));
    public static final ResourceKey<DamageType> SELF = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation("the_long_travail", "azrael_self"));
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();
    private static final class Frame {
        final LivingEntity target;
        DamageSource source;
        boolean acceptedDeath, cancelledDeath, invalidDamage;
        final StringBuilder diagnostic;
        Frame(LivingEntity target) { this.target = target; diagnostic = beginDiagnostic(target); }
        void begin(DamageSource source) { this.source = source; acceptedDeath = cancelledDeath = invalidDamage = false; }
    }
    public enum Result { DEAD, SURVIVED, INVALID }
    private enum Attempt { DEAD, REMOVED, NONFINITE, CANCELLED, NO_DEATH }
    private static final boolean DIAGNOSTICS = Boolean.getBoolean("travail.execution.diagnostics");
    private static final java.util.Map<ResourceLocation, Long> NEXT_REPORT = new java.util.HashMap<>();
    public static void clearDiagnostics() { NEXT_REPORT.clear(); }
    private static StringBuilder beginDiagnostic(LivingEntity target) {
        if (!DIAGNOSTICS) return null;
        ResourceLocation type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        long now = System.nanoTime();
        Long next = NEXT_REPORT.get(type);
        if (next != null && now - next < 0) return null;
        NEXT_REPORT.put(type, now + 60_000_000_000L);
        return new StringBuilder().append("entity=").append(type);
    }
    private static void report(Frame frame, String stage, int number, Attempt result) {
        if (frame.diagnostic != null) frame.diagnostic.append("; ").append(stage).append('#').append(number).append('=').append(result);
    }
    private static Attempt outcome(Frame frame) {
        // 死亡已在修复前判定；不要因诊断分类而在修复后再次触发第三方生命值读取。
        if (frame.target.isRemoved()) return Attempt.REMOVED;
        if (frame.invalidDamage) return Attempt.NONFINITE;
        return frame.cancelledDeath ? Attempt.CANCELLED : Attempt.NO_DEATH;
    }
    public static boolean active() { return CURRENT.get() != null; }
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
    private static DamageSource source(ServerPlayer player, LivingEntity target, ResourceKey<DamageType> type) {
        var holder = target.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type);
        // 反噬自身不属于 PvP，服务器关闭 PvP 或队伍禁用友伤时仍须生效。
        boolean self = target == player;
        return new DamageSource(holder, self ? null : player, self ? null : player) {
            @Override public net.minecraft.network.chat.Component getLocalizedDeathMessage(LivingEntity victim) {
                // 仅定制本次处决的死亡提示，保留 generic_kill 的类型和标签，避免武器名称影响提示。
                return net.minecraft.network.chat.Component.translatable("death.attack.the_long_travail."
                        + (self ? "azrael_self" : "azrael_execution"), victim.getDisplayName());
            }
        };
    }
    private static boolean dead(Frame frame) {
        LivingEntity target = frame.target;
        return target.getRemovalReason() == Entity.RemovalReason.KILLED
                || frame.acceptedDeath && Float.isFinite(target.getHealth()) && target.getHealth() <= 0;
    }
    // 每次调用前都须记录伤害归属，第三层处决不能依赖 hurt 成功后才写入的记录。
    private static void credit(ServerPlayer player, LivingEntity target) {
        if (target == player) return;
        target.setLastHurtByPlayer(player);
        ((com.thelongtravail.mixin.AzraelLivingAccessor) target).travail$recentPlayerHit(100);
    }
    private static float snapshotHealth(LivingEntity target) {
        float value = target.getHealth();
        return Float.isFinite(value) && value > 0 ? value : Math.max(1, target.getMaxHealth());
    }
    private static void repair(Frame frame, float health, float absorption) {
        var target = frame.target;
        // NaN 不视为死亡；若死亡被取消但生命值尚未恢复，仍保留此次保命后的存活状态。
        if (!Float.isFinite(target.getHealth()) || frame.cancelledDeath && target.getHealth() <= 0) target.setHealth(health);
        if (!Float.isFinite(target.getAbsorptionAmount())) target.setAbsorptionAmount(Float.isFinite(absorption) ? absorption : 0);
    }
    private static Attempt hit(Frame frame, ServerPlayer player, DamageSource source, float amount) {
        var target = frame.target;
        float health = snapshotHealth(target), absorption = target.getAbsorptionAmount();
        frame.begin(source);
        target.invulnerableTime = 0;
        credit(player, target);
        target.hurt(source, amount);
        if (dead(frame)) return Attempt.DEAD;
        frame.invalidDamage = !Float.isFinite(target.getHealth()) || !Float.isFinite(target.getAbsorptionAmount());
        if(frame.invalidDamage)com.thelongtravail.data.HotPathMetrics.Counter.EXECUTION_NONFINITE.add(1);
        repair(frame, health, absorption);
        return outcome(frame);
    }
    private static Attempt infiniteDamage(Frame frame,ServerPlayer player,DamageSource source){
        com.thelongtravail.data.HotPathMetrics.Counter.EXECUTION_INFINITE.add(1);
        return hit(frame,player,source,Float.POSITIVE_INFINITY);
    }
    private static Attempt genericKill(Frame frame,ServerPlayer player){
        com.thelongtravail.data.HotPathMetrics.Counter.EXECUTION_GENERIC.add(1);
        return hit(frame,player,source(player,frame.target,DamageTypes.GENERIC_KILL),Float.MAX_VALUE);
    }
    private static Attempt directDeath(Frame frame,ServerPlayer player,DamageSource execution){
        com.thelongtravail.data.HotPathMetrics.Counter.EXECUTION_DIRECT.add(1);
        var target=frame.target;
        float health=snapshotHealth(target),absorption=target.getAbsorptionAmount();
        frame.begin(execution);
        credit(player,target);
        target.getCombatTracker().recordDamage(execution,health);
        target.setHealth(0);
        target.die(execution);
        if(dead(frame))return Attempt.DEAD;
        frame.invalidDamage = !Float.isFinite(target.getHealth()) || !Float.isFinite(target.getAbsorptionAmount());
        repair(frame,health,absorption);
        return outcome(frame);
    }
    public static Result execute(ServerPlayer player, LivingEntity target) {
        if (active() || target.isRemoved() || !target.isAlive() || target.level() != player.level()) return Result.INVALID;
        // 随机判定后到本刻末尾执行前，PvP 开关或队伍关系可能变化，第三层处决也须重新检查。
        if (target != player && target instanceof net.minecraft.world.entity.player.Player other
                && (!player.server.isPvpAllowed() || !player.canHarmPlayer(other))) return Result.INVALID;
        Frame frame = new Frame(target);
        CURRENT.set(frame);
        try {
            DamageSource execution = source(player, target, target == player ? SELF : TYPE);
            for (int i = 0; i < AzraelConfig.ATTEMPTS.get(); i++) {
                Attempt result = infiniteDamage(frame, player, execution);
                report(frame, "infinite", i + 1, result);
                if (result == Attempt.DEAD) return Result.DEAD;
                if (result == Attempt.REMOVED) return Result.INVALID;
                if (result == Attempt.NONFINITE) break;
            }
            if (AzraelConfig.KILL_FALLBACK.get()) {
                Attempt result = genericKill(frame, player);
                report(frame, "generic_kill", 1, result);
                if (result == Attempt.DEAD) return Result.DEAD;
                if (result == Attempt.REMOVED) return Result.INVALID;
            }
            if (AzraelConfig.DEATH_FALLBACK.get()) {
                Attempt result = directDeath(frame, player, execution);
                report(frame, "direct_death", 1, result);
                if (result == Attempt.DEAD) return Result.DEAD;
            }
            return Result.SURVIVED;
        } finally {
            CURRENT.remove();
            if (frame.diagnostic != null) com.thelongtravail.TheLongTravail.LOGGER.info("Azrael execution: {}", frame.diagnostic);
        }
    }
    private AzraelExecution() {}
}

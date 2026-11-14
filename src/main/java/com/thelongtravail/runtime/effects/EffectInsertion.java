package com.thelongtravail.runtime.effects;

import com.thelongtravail.common.RuntimeFailures;
import com.thelongtravail.mixin.effects.LivingEffectsAccessor;

import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

// 显式调用的效果植入工具；不修改全局药水免疫规则。
public final class EffectInsertion {
    // 服务端正常添加后返回实际实例；首次拒收时写入真实表并执行添加回调。
    // 已有效果的合并先遵循原版规则，拒绝更新时返回既有实例。
    // 返回 null 表示失败；业务方自行决定目标限制、叠层、时长和消耗。
    // 业务方完成实例修改后调用 synchronize；本方法不反复补回被清除的效果。
    @Nullable
    public static MobEffectInstance addOrGet(
            LivingEntity target, MobEffectInstance incoming, @Nullable Entity source) {
        return addOrGet(target, incoming, source, actual -> {});
    }

    // 调用方的实例调整与同步也纳入恢复范围；物品消耗必须在成功返回之后执行。
    @Nullable
    public static MobEffectInstance addOrGet(
            LivingEntity target,
            MobEffectInstance incoming,
            @Nullable Entity source,
            java.util.function.Consumer<MobEffectInstance> finish) {
        if (!(target.level() instanceof ServerLevel) || !target.isAlive()) return null;
        EffectInsertionState before;
        try {
            before = new EffectInsertionState(target, incoming.getEffect());
        } catch (RuntimeException failure) {
            RuntimeFailures.report("effect insertion snapshot", failure);
            return null;
        }
        RuntimeException failure = null;
        try {
            target.addEffect(incoming, source);
            var actual = target.getEffect(incoming.getEffect());
            if (actual == null && writeVerified(target, incoming)) {
                ((LivingEffectsAccessor) target).travail$onEffectAdded(incoming, source);
                actual = target.getEffect(incoming.getEffect());
            }
            if (actual != null) {
                finish.accept(actual);
                if (target.getEffect(incoming.getEffect()) == actual) return actual;
            }
        } catch (RuntimeException caught) {
            failure = caught;
        }
        // 外部回调只执行一次；恢复失败继续向上传播异常。
        try {
            before.restore();
        } catch (RuntimeException rollback) {
            if (failure != null) rollback.addSuppressed(failure);
            throw rollback;
        }
        if (failure != null) RuntimeFailures.report("effect insertion callback", failure);
        return null;
    }

    // 写入并验证原版存储；失败恢复原键。调用方负责属性回调与同步。
    public static boolean writeVerified(LivingEntity target, MobEffectInstance incoming) {
        var effects = ((LivingEffectsAccessor) target).travail$activeEffects();
        var key = incoming.getEffect();
        var previous = effects.get(key);
        RuntimeException failure = null;
        try {
            effects.put(key, incoming);
            if (target.getEffect(key) == incoming) return true;
        } catch (RuntimeException caught) {
            failure = caught;
        }
        try {
            if (previous == null) effects.remove(key);
            else effects.put(key, previous);
            if (effects.get(key) != previous)
                throw new IllegalStateException(
                        "Effect storage did not restore the previous entry");
        } catch (RuntimeException rollback) {
            if (failure != null) rollback.addSuppressed(failure);
            throw rollback;
        }
        if (failure != null) throw failure;
        return false;
    }

    // 刷新效果显示状态；仅服务端向现有追踪者发送原版效果包。
    public static void synchronize(LivingEntity target, MobEffectInstance actual) {
        ((LivingEffectsAccessor) target).travail$effectsDirty(true);
        if (target.level() instanceof ServerLevel level)
            level.getChunkSource()
                    .broadcastAndSend(
                            target, new ClientboundUpdateMobEffectPacket(target.getId(), actual));
    }

    private EffectInsertion() {}
}

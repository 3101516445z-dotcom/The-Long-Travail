package com.thelongtravail.runtime.effects;

import com.thelongtravail.mixin.effects.LivingEffectsAccessor;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

// 显式调用的效果植入工具；不修改全局药水免疫规则。
public final class EffectInsertion {
    // 服务端正常添加后返回实际实例；首次拒收时写入真实表并执行添加回调。
    // 已有效果的合并先遵循原版规则，拒绝更新时返回既有实例。
    // 返回 null 表示失败；业务方自行决定目标限制、叠层、时长和消耗。
    // 业务方完成实例修改后调用 synchronize；本方法不反复补回被清除的效果。
    @Nullable
    public static MobEffectInstance addOrGet(
            LivingEntity target, MobEffectInstance incoming, @Nullable Entity source) {
        if (!(target.level() instanceof ServerLevel) || !target.isAlive()) return null;
        target.addEffect(incoming, source);
        var actual = target.getEffect(incoming.getEffect());
        if (actual != null) return actual;
        if (!writeVerified(target, incoming)) return null;
        ((LivingEffectsAccessor) target).travail$onEffectAdded(incoming, source);
        return target.getEffect(incoming.getEffect());
    }

    // 写入并验证原版存储；失败恢复原键。调用方负责属性回调与同步。
    public static boolean writeVerified(LivingEntity target, MobEffectInstance incoming) {
        var effects = ((LivingEffectsAccessor) target).travail$activeEffects();
        var previous = effects.put(incoming.getEffect(), incoming);
        if (target.getEffect(incoming.getEffect()) == incoming) return true;
        if (previous == null) effects.remove(incoming.getEffect(), incoming);
        else effects.put(incoming.getEffect(), previous);
        return false;
    }

    // 刷新效果显示状态；仅服务端向现有追踪者发送原版效果包。
    public static void synchronize(LivingEntity target, MobEffectInstance actual) {
        ((LivingEffectsAccessor) target).travail$effectsDirty(true);
        if (target.level() instanceof ServerLevel level)
            level.getChunkSource().broadcastAndSend(
                    target, new ClientboundUpdateMobEffectPacket(target.getId(), actual));
    }

    private EffectInsertion() {}
}

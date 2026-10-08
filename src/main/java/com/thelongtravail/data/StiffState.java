package com.thelongtravail.data;

import com.thelongtravail.network.TravailNetwork;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

// 玩家僵硬独立于药水效果；客户端的限制由服务端开启和解除。
public final class StiffState {
    private static final String UNTIL = "LongTravailStiffUntil";

    public static boolean active(LivingEntity entity) {
        // 保留命令等途径向非玩家生物施加僵硬的原有行为。
        if (!(entity instanceof Player player)) return entity.hasEffect(ModRegistry.STIFF.get());
        // 本地倒计时仅供预测显示，不能在低 TPS 时抢先解除服务端状态。
        return player.level().isClientSide ? player.getPersistentData().contains(UNTIL) : remaining(player) > 0;
    }

    public static int remaining(Player player) {
        var data = player.getPersistentData();
        if (!data.contains(UNTIL)) return 0;
        long left = data.getLong(UNTIL) - clock(player);
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, left));
    }

    public static void start(ServerPlayer player, int ticks) {
        if (ticks <= 0) return;
        int duration = Math.max(ticks, remaining(player));
        accept(player, duration);
        TravailNetwork.sendStiff(player, duration);
        // 图标被拒绝或移除均不影响上面已经建立的状态。
        EffectChanges.add(player, new MobEffectInstance(ModRegistry.STIFF.get(), duration));
    }

    public static void accept(Player player, int ticks) {
        if (ticks <= 0) player.getPersistentData().remove(UNTIL);
        else player.getPersistentData().putLong(UNTIL, clock(player) + ticks);
    }

    private static long clock(Player player) {
        // 客户端使用本地刻计时，避免世界时间同步校正改变预测剩余时长。
        return player.level().isClientSide ? player.tickCount : player.level().getGameTime();
    }

    public static void clear(ServerPlayer player) {
        accept(player, 0);
        if (player.hasEffect(ModRegistry.STIFF.get())) EffectChanges.remove(player, ModRegistry.STIFF.get(), false);
        TravailNetwork.sendStiff(player, 0);
    }

    public static void tick(ServerPlayer player) {
        if (player.getPersistentData().contains(UNTIL) && remaining(player) == 0) clear(player);
    }

    public static void resend(ServerPlayer player) {
        int left = remaining(player);
        if (left <= 0) clear(player);
        else TravailNetwork.sendStiff(player, left);
    }

    // 时停期间推迟权威截止时间，与冻结的显示效果保持一致。
    public static void pause(ServerPlayer player) {
        var data = player.getPersistentData();
        if (data.contains(UNTIL)) data.putLong(UNTIL, data.getLong(UNTIL) + 1);
    }
    private StiffState() {}
}

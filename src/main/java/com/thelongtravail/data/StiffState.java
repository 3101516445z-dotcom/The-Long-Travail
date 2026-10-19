package com.thelongtravail.data;

import com.thelongtravail.network.TravailNetwork;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class StiffState {
    private static final String UNTIL = "LongTravailStiffUntil";

    public static boolean active(LivingEntity entity) {
        if (!(entity instanceof Player player)) return entity.hasEffect(ModRegistry.STIFF.get());
        if (!com.thelongtravail.config.ModeEffectRules.diaryApplies(player)) return false;
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
        if (ticks <= 0 || !com.thelongtravail.config.ModeEffectRules.diaryApplies(player)) return;
        int duration = Math.max(ticks, remaining(player));
        accept(player, duration);
        TravailNetwork.sendStiff(player, duration);
        EffectChanges.add(player, new MobEffectInstance(ModRegistry.STIFF.get(), duration));
    }

    public static void accept(Player player, int ticks) {
        if (ticks <= 0) player.getPersistentData().remove(UNTIL);
        else player.getPersistentData().putLong(UNTIL, clock(player) + ticks);
    }

    private static long clock(Player player) {
        return player.level().isClientSide ? player.tickCount : player.level().getGameTime();
    }

    public static void clear(ServerPlayer player) {
        accept(player, 0);
        if (player.hasEffect(ModRegistry.STIFF.get())) EffectChanges.remove(player, ModRegistry.STIFF.get(), false);
        TravailNetwork.sendStiff(player, 0);
    }

    public static void tick(ServerPlayer player) {
        if (player.getPersistentData().contains(UNTIL)
                && (remaining(player) == 0 || !com.thelongtravail.config.ModeEffectRules.diaryApplies(player)))
            clear(player);
    }

    public static void resend(ServerPlayer player) {
        int left = remaining(player);
        if (left <= 0) clear(player);
        else TravailNetwork.sendStiff(player, left);
    }

    public static void pause(ServerPlayer player) {
        var data = player.getPersistentData();
        if (data.contains(UNTIL)) data.putLong(UNTIL, data.getLong(UNTIL) + 1);
    }

    private StiffState() {}
}

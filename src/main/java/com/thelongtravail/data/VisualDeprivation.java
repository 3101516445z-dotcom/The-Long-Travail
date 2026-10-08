package com.thelongtravail.data;

import com.thelongtravail.network.TravailNetwork;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * 服务端以时间戳保存权威状态，不依赖效果实例，避免受到效果层防护的影响。
 * 其他模组即使拒绝或移除效果实例，诅咒状态仍然有效；效果实例仅用于显示原版 HUD 图标。
 */
public final class VisualDeprivation {
    public enum ClearReason { COMMAND, MILK, EXPIRED, DEATH, RESPAWN, DIMENSION_CHANGE, DIARY_RESET, NO_ACTIVE_STATE, API }
    private static final String UNTIL = "LongTravailVisualDeprivationUntil";
    private static final String TOTAL = "LongTravailVisualDeprivationTotal";

    public static void start(ServerPlayer player, int ticks) {
        if (ticks <= 0) {
            clear(player, ClearReason.NO_ACTIVE_STATE);
            return;
        }
        CompoundTag data = player.getPersistentData();
        data.putLong(UNTIL, player.level().getGameTime() + ticks);
        data.putLong(TOTAL, ticks);
        TravailNetwork.sendVisualDeprivation(player, ticks, ticks);
        MobEffect effect = ModRegistry.VISUAL_DEPRIVATION.get();
        // 每次刷新用于显示的效果以覆盖完整持续时间，原版会保留现有实例并延长持续时间。
        EffectChanges.add(player, new MobEffectInstance(effect, ticks));
        com.thelongtravail.TheLongTravail.LOGGER.debug(
                "Visual deprivation started for {} ({} ticks, effect applied: {})",
                player.getName().getString(), ticks, player.hasEffect(effect));
    }

    public static void clear(ServerPlayer player) { clear(player, ClearReason.API); }

    public static void clear(ServerPlayer player, ClearReason reason) {
        boolean had = player.getPersistentData().contains(UNTIL);
        forget(player);
        MobEffect effect = ModRegistry.VISUAL_DEPRIVATION.get();
        if (player.hasEffect(effect)) EffectChanges.remove(player, effect, false);
        // 显式清除时，即使服务端标签已不存在，也要修正客户端残留状态。
        TravailNetwork.sendVisualDeprivation(player, 0, 0, switch (reason) {
            case EXPIRED, DEATH, RESPAWN, DIMENSION_CHANGE, DIARY_RESET, NO_ACTIVE_STATE -> true;
            default -> false;
        });
        if (had) {
            com.thelongtravail.TheLongTravail.LOGGER.debug(
                    "Visual deprivation cleared for {} ({})", player.getName().getString(), reason);
        }
    }

    // 仅清理存储；调用方应使用 clear，同时清理显示实例并同步客户端。
    private static void forget(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.remove(UNTIL);
        data.remove(TOTAL);
    }

    public static int remaining(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(UNTIL)) return 0;
        long left = data.getLong(UNTIL) - player.level().getGameTime();
        return left <= 0L ? 0 : (int) Math.min(Integer.MAX_VALUE, left);
    }

    public static void tick(ServerPlayer player) {
        if (!player.getPersistentData().contains(UNTIL)) return;
        int left = remaining(player);
        if (left == 0) clear(player, ClearReason.EXPIRED);
        else TravailNetwork.sendVisualProgress(player, left);
    }

    // 重新向刚加入的客户端同步尚未结束的状态，保证重新登录后视觉效果仍持续。
    public static void resend(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        int left = remaining(player);
        if (left <= 0) {
            clear(player, ClearReason.NO_ACTIVE_STATE);
            return;
        }
        int total = (int) Math.min(Integer.MAX_VALUE, data.getLong(TOTAL));
        TravailNetwork.sendVisualDeprivation(player, left, Math.max(left, total));
    }

    // 时停期间推迟权威截止时间，与冻结的显示效果保持一致。
    public static void pause(ServerPlayer player) {
        var data = player.getPersistentData();
        if (data.contains(UNTIL)) data.putLong(UNTIL, data.getLong(UNTIL) + 1);
    }
    private VisualDeprivation() {}
}

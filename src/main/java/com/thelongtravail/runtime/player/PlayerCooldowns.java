package com.thelongtravail.runtime.player;

import com.thelongtravail.runtime.scheduling.RemainingTicks;
import com.thelongtravail.runtime.scheduling.TickDeadline;

import net.minecraft.world.entity.player.Player;

/** 按物品原有的键读写玩家持久数据中的冷却。 */
public final class PlayerCooldowns {
    public static int remainingTicks(Player player, String key) {
        return RemainingTicks.get(player.getPersistentData(), key);
    }

    /** 复制或同步时保留存储的原始有符号值。 */
    public static int storedTicks(Player player, String key) {
        return player.getPersistentData().getInt(key);
    }

    public static void startTicks(Player player, String key, int ticks) {
        RemainingTicks.start(player.getPersistentData(), key, ticks);
    }

    public static boolean tick(Player player, String key) {
        return RemainingTicks.tick(player.getPersistentData(), key);
    }

    public static void extendTicks(Player player, String key, int ticks) {
        RemainingTicks.extend(player.getPersistentData(), key, ticks);
    }

    public static long remainingUntil(Player player, String key, long now) {
        return TickDeadline.remaining(player.getPersistentData(), key, now);
    }

    public static void startDeadline(Player player, String key, long now, int duration) {
        TickDeadline.start(player.getPersistentData(), key, now, duration);
    }

    public static void copyDeadlineIfPresent(Player source, Player target, String key) {
        var data = source.getPersistentData();
        if (data.contains(key)) target.getPersistentData().putLong(key, data.getLong(key));
    }

    private PlayerCooldowns() {}
}

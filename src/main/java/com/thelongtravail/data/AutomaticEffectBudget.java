package com.thelongtravail.data;

import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.TheLongTravail;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.WeakHashMap;

// 额度仅由穷遐净化、效果赋予和幽谷攻击侧的见证效果赋予共用。
public final class AutomaticEffectBudget {
    private static final Map<ServerPlayer, EffectActionWindow> WINDOWS = new WeakHashMap<>();
    public static boolean acquire(ServerPlayer player) {
        int limit = TravailConfig.MAX_AUTOMATIC_EFFECT_ACTIONS.get();
        if (limit == 0) { WINDOWS.remove(player); return true; }
        return WINDOWS.computeIfAbsent(player, unused -> new EffectActionWindow()).acquire(player.level().getGameTime(), limit);
    }
    public static void forget(ServerPlayer player) { WINDOWS.remove(player); }
    public static void clear() { WINDOWS.clear(); }
    public static void diagnoseConfiguration() {
        int limit = TravailConfig.MAX_AUTOMATIC_EFFECT_ACTIONS.get();
        if (limit <= 0) return;
        int actions = TravailConfig.FAR_WITNESS_ACTION_COUNT.get();
        long interval = Math.max(1L, Math.round(TravailConfig.FAR_WITNESS_INTERVAL_SECONDS.get() * 20));
        double grantRate = 20D * actions / interval;
        double worstRate = actions == 0 ? 0 : 20D * (actions + Math.min(64, Math.max(32, actions))) / interval;
        long peak = actions == 0 ? 0 : (1 + 19 / interval) * (actions + Math.min(64, Math.max(32, actions)));
        if (peak > limit) TheLongTravail.LOGGER.warn(
                "Automatic effect budget {} / 20 ticks may limit Far Reach: grant-only average {} / second, with failed cleanses up to {} / second on average, {} attempts in a 20-tick burst. Excess is skipped, never queued.", limit, grantRate, worstRate, peak);
        TheLongTravail.LOGGER.info("Automatic effects limited to {} attempts per player in any rolling 20 ticks. Far Reach and Deep Valley witness share the allowance; cancellations also count.", limit);
    }
    private AutomaticEffectBudget() {}
}

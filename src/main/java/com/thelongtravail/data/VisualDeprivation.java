package com.thelongtravail.data;

import com.thelongtravail.network.TravailNetwork;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Server side truth for visual deprivation: a plain timestamp, never an effect instance.
 *
 * Keeping the state outside the effect map is what makes it immune to effect level protection.
 * Mods can refuse to apply the effect or strip it, but they cannot see this state, so the curse
 * still works. The effect instance is only a presentation layer for the vanilla HUD icon.
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
        // Always refresh the presentation instance so it covers the whole episode; vanilla keeps
        // the existing instance and only extends the duration.
        EffectChanges.add(player, new MobEffectInstance(effect, ticks));
        com.thelongtravail.TheLongTravail.LOGGER.debug(
                "Visual deprivation started for {} ({} ticks, effect applied: {})",
                player.getName().getString(), ticks, player.hasEffect(effect));
    }

    /** Ends the state and drops the presentation instance; used by milk and by debug commands. */
    public static void clear(ServerPlayer player) { clear(player, ClearReason.API); }

    public static void clear(ServerPlayer player, ClearReason reason) {
        boolean had = player.getPersistentData().contains(UNTIL);
        forget(player);
        MobEffect effect = ModRegistry.VISUAL_DEPRIVATION.get();
        if (player.hasEffect(effect)) EffectChanges.remove(player, effect, false);
        // Explicit clears also repair a stale client when the server tag is already absent.
        TravailNetwork.sendVisualDeprivation(player, 0, 0, switch (reason) {
            case DEATH, RESPAWN, DIMENSION_CHANGE, DIARY_RESET, NO_ACTIVE_STATE -> true;
            default -> false;
        });
        if (had) {
            com.thelongtravail.TheLongTravail.LOGGER.debug(
                    "Visual deprivation cleared for {} ({})", player.getName().getString(), reason);
        }
    }

    /** Storage-only step; callers must use clear so presentation and client state also converge. */
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

    /** Housekeeping: drop the stored timestamp once it has elapsed. */
    public static void tick(ServerPlayer player) {
        if (player.getPersistentData().contains(UNTIL) && remaining(player) == 0) clear(player, ClearReason.EXPIRED);
    }

    /** Re-sends the running episode to a client that just joined, so a relog keeps the visuals. */
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

    private VisualDeprivation() {}
}

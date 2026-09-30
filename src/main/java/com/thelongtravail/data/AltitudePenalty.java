package com.thelongtravail.data;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.network.TooltipConfigSync;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * High-altitude slowdown of Boundless Malice, evaluated where the speed is read.
 *
 * The penalty is deliberately not stored as an AttributeModifier: nothing appears on the
 * movement speed attribute, so no other mod can inspect it, compensate it with a counter
 * modifier or strip it while recomputing the attribute value.
 */
public final class AltitudePenalty {
    /** Implemented on each Player instance: no global player references or shared cache lock. */
    public interface Cache {
        float travail$altitudeFactor();
        void travail$invalidateAltitude();
    }
    private static long serverRevision;
    public static void configReloaded() { serverRevision++; }
    public static long revision(Player player) {
        return player.level().isClientSide ? TooltipConfigSync.revision() : serverRevision;
    }

    /** Multiplier applied to the player's movement speed; 1.0 means no penalty. */
    public static float factor(Player player) {
        if (player == null) return 1.0F;
        return player instanceof Cache cache ? cache.travail$altitudeFactor() : compute(player);
    }

    public static void forget(Player player) {
        if (player instanceof Cache cache) cache.travail$invalidateAltitude();
    }

    public static float compute(Player player) {
        ItemStack diary = TravailCurios.stack(player);
        if (diary.isEmpty() || LongTravailData.hasWitness(diary, TravailAspect.BOUNDLESS)) return 1.0F;
        double threshold = TravailConfig.BOUNDLESS_HEIGHT_THRESHOLD.get();
        double reduction = TravailConfig.BOUNDLESS_SPEED_REDUCTION.get();
        if (player.level().isClientSide) {
            threshold = TooltipConfigSync.decimal("boundless.heightThreshold", threshold);
            reduction = TooltipConfigSync.decimal("boundless.speedReduction", reduction);
        }
        if (reduction <= 0.0D || player.getY() <= threshold) return 1.0F;
        return (float) (1.0D - Math.min(1.0D, reduction));
    }

    private AltitudePenalty() {}
}

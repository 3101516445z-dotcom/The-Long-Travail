package com.thelongtravail.api;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

/**
 * Public, read-only aspect queries for item integrations. Player queries use the same first
 * equipped Curios diary as the gameplay rules. Inventory-only diaries never activate a state.
 * Call on the logical game thread; the server is authoritative, clients see their synced copy.
 * MALICE/WITNESS identifies an aspect, not whether its conditional effect is firing this instant.
 */
public final class TravailStateApi {
    public enum AspectState { INACTIVE, MALICE, WITNESS }

    /** No diary is INACTIVE, not MALICE. An equipped uninitialized diary follows existing rules. */
    public static AspectState state(@Nullable Player player, TravailAspect aspect) {
        Objects.requireNonNull(aspect, "aspect");
        ItemStack diary = equipped(player);
        if (diary.isEmpty()) return AspectState.INACTIVE;
        return LongTravailData.hasWitness(diary, aspect) ? AspectState.WITNESS : AspectState.MALICE;
    }

    public static boolean hasMalice(@Nullable Player player, TravailAspect aspect) {
        return state(player, aspect) == AspectState.MALICE;
    }
    public static boolean hasWitness(@Nullable Player player, TravailAspect aspect) {
        return state(player, aspect) == AspectState.WITNESS;
    }

    public static boolean hasFlourishingMalice(@Nullable Player player) { return hasMalice(player, TravailAspect.FLOURISHING); }
    public static boolean hasFlourishingWitness(@Nullable Player player) { return hasWitness(player, TravailAspect.FLOURISHING); }
    public static boolean hasAbyssMalice(@Nullable Player player) { return hasMalice(player, TravailAspect.ABYSS); }
    public static boolean hasAbyssWitness(@Nullable Player player) { return hasWitness(player, TravailAspect.ABYSS); }
    public static boolean hasFarReachMalice(@Nullable Player player) { return hasMalice(player, TravailAspect.FAR_REACH); }
    public static boolean hasFarReachWitness(@Nullable Player player) { return hasWitness(player, TravailAspect.FAR_REACH); }
    public static boolean hasDeepValleyMalice(@Nullable Player player) { return hasMalice(player, TravailAspect.DEEP_VALLEY); }
    public static boolean hasDeepValleyWitness(@Nullable Player player) { return hasWitness(player, TravailAspect.DEEP_VALLEY); }
    public static boolean hasUnderworldMalice(@Nullable Player player) { return hasMalice(player, TravailAspect.UNDERWORLD); }
    public static boolean hasUnderworldWitness(@Nullable Player player) { return hasWitness(player, TravailAspect.UNDERWORLD); }
    public static boolean hasBoundlessMalice(@Nullable Player player) { return hasMalice(player, TravailAspect.BOUNDLESS); }
    public static boolean hasBoundlessWitness(@Nullable Player player) { return hasWitness(player, TravailAspect.BOUNDLESS); }

    /** One equipment lookup for callers that need several aspects. Capture again after mutations. */
    public static Snapshot snapshot(@Nullable Player player) { return inspectDiary(equipped(player)); }

    /** Inspects stored item state only. This does NOT establish that somebody has equipped it. */
    public static Snapshot inspectDiary(@Nullable ItemStack diary) {
        if (diary == null || diary.isEmpty() || !diary.is(ModRegistry.LONG_TRAVAIL.get())) return Snapshot.ABSENT;
        int witnesses = 0;
        for (TravailAspect aspect : TravailAspect.values()) {
            if (LongTravailData.hasWitness(diary, aspect)) witnesses |= aspect.mask();
        }
        return new Snapshot(true, LongTravailData.isInitialized(diary), witnesses);
    }

    /** Immutable value: keeps neither an ItemStack nor a Player reference and never writes NBT. */
    public static final class Snapshot {
        private static final Snapshot ABSENT = new Snapshot(false, false, 0);
        private final boolean diaryPresent, initialized;
        private final int witnesses;
        private Snapshot(boolean diaryPresent, boolean initialized, int witnesses) {
            this.diaryPresent = diaryPresent; this.initialized = initialized; this.witnesses = witnesses;
        }
        public boolean diaryPresent() { return diaryPresent; }
        public boolean initialized() { return initialized; }
        public AspectState state(TravailAspect aspect) {
            Objects.requireNonNull(aspect, "aspect");
            if (!diaryPresent) return AspectState.INACTIVE;
            return (witnesses & aspect.mask()) != 0 ? AspectState.WITNESS : AspectState.MALICE;
        }
        public boolean hasMalice(TravailAspect aspect) { return state(aspect) == AspectState.MALICE; }
        public boolean hasWitness(TravailAspect aspect) { return state(aspect) == AspectState.WITNESS; }
    }

    private static ItemStack equipped(@Nullable Player player) {
        return player == null ? ItemStack.EMPTY : TravailCurios.stack(player);
    }
    private TravailStateApi() {}
}

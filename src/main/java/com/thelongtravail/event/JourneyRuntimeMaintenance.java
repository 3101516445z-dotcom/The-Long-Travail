package com.thelongtravail.event;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.data.*;
import com.thelongtravail.network.TravailNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Post-commit journey maintenance shared by the service and Forge lifecycle adapters. */
public final class JourneyRuntimeMaintenance {
    public static void onDiaryReset(ServerPlayer player, ItemStack diary) {
        resetRuntime(player, VisualDeprivation.ClearReason.DIARY_RESET);
        TravailPlayerMaintenance.updateExtraSlots(player, diary);
        TravailPlayerMaintenance.updateAltitudePenalty(player, diary);
        TravailNetwork.sendJourney(player);
    }

    public static void onJourneyChanged(
            ServerPlayer player, ItemStack diary, JourneyService.OperationResult change) {
        if (change.queryPolicy() == JourneyService.QueryPolicy.INVALIDATE) {
            JourneyQueries.forget(player);
            WayguideSearch.forget(player);
        }
        int switched = change.beforeMask() ^ change.afterMask();
        if ((switched & TravailAspect.DEEP_VALLEY.mask()) != 0
                || change.resetAspects().contains(TravailAspect.DEEP_VALLEY))
            com.thelongtravail.valley.AzraelState.forget(player);
        if ((switched & TravailAspect.FAR_REACH.mask()) != 0
                || change.resetAspects().contains(TravailAspect.FAR_REACH))
            CleanseRotation.forget(player);
        if ((switched & TravailAspect.BOUNDLESS.mask()) != 0
                || change.resetAspects().contains(TravailAspect.BOUNDLESS)) {
            AltitudePenalty.forget(player);
            TravailPlayerMaintenance.updateExtraSlots(player, diary);
            TravailPlayerMaintenance.updateAltitudePenalty(player, diary);
        }
    }

    static void resetRuntime(ServerPlayer player, VisualDeprivation.ClearReason reason) {
        clearRuntimeCaches(player);
        VisualDeprivation.clear(player, reason);
        StiffState.clear(player);
    }

    static void clearRuntimeCaches(ServerPlayer player) {
        com.thelongtravail.valley.AzraelState.forget(player);
        AltitudePenalty.forget(player);
        com.thelongtravail.helper.ReceivedMalice.forget(player);
        AutomaticEffectBudget.forget(player);
        JourneyQueries.forget(player);
        CleanseRotation.forget(player);
        EffectChanges.forget(player);
        WayguideSearch.forget(player);
    }

    private JourneyRuntimeMaintenance() {}
}

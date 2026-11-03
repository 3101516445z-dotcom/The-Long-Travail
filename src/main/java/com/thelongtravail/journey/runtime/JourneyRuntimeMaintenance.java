package com.thelongtravail.journey.runtime;

import com.thelongtravail.runtime.attributes.AltitudePenalty;

import com.thelongtravail.runtime.effects.AutomaticEffectBudget;
import com.thelongtravail.runtime.effects.CleanseRotation;
import com.thelongtravail.runtime.effects.EffectChanges;
import com.thelongtravail.runtime.effects.StiffState;
import com.thelongtravail.runtime.effects.VisualDeprivation;

import com.thelongtravail.journey.JourneyService;
import com.thelongtravail.journey.JourneyQueries;

import com.thelongtravail.event.TravailPlayerMaintenance;

import com.thelongtravail.exploration.WayguideSearch;

import com.thelongtravail.TravailAspect;

import com.thelongtravail.network.TravailNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

// 协调旅程提交后的运行状态维护，也供 Forge 生命周期入口调用。
public final class JourneyRuntimeMaintenance {
    public static void onDiaryReset(ServerPlayer player, ItemStack diary) {
        resetRuntime(player, VisualDeprivation.ClearReason.DIARY_RESET);
        TravailPlayerMaintenance.refreshJourneyEffects(player, diary);
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
            com.thelongtravail.valley.execution.AzraelState.forget(player);
        if ((switched & TravailAspect.FAR_REACH.mask()) != 0
                || change.resetAspects().contains(TravailAspect.FAR_REACH))
            CleanseRotation.forget(player);
        if ((switched & TravailAspect.BOUNDLESS.mask()) != 0
                || change.resetAspects().contains(TravailAspect.BOUNDLESS)) {
            AltitudePenalty.forget(player);
            TravailPlayerMaintenance.refreshJourneyEffects(player, diary);
        }
    }

    private static void resetRuntime(ServerPlayer player, VisualDeprivation.ClearReason reason) {
        clearRuntimeCaches(player);
        VisualDeprivation.clear(player, reason);
        StiffState.clear(player);
    }

    private static void clearRuntimeCaches(ServerPlayer player) {
        com.thelongtravail.valley.execution.AzraelState.forget(player);
        AltitudePenalty.forget(player);
        com.thelongtravail.combat.ReceivedMalice.forget(player);
        AutomaticEffectBudget.forget(player);
        JourneyQueries.forget(player);
        CleanseRotation.forget(player);
        EffectChanges.forget(player);
        WayguideSearch.forget(player);
    }

    public static void onRespawn(ServerPlayer player) {
        resetRuntime(player, VisualDeprivation.ClearReason.RESPAWN);
    }

    public static void onDimensionChange(ServerPlayer player) {
        resetRuntime(player, VisualDeprivation.ClearReason.DIMENSION_CHANGE);
    }

    public static void onDeath(ServerPlayer player) {
        resetRuntime(player, VisualDeprivation.ClearReason.DEATH);
    }

    public static void onLogout(ServerPlayer player) {
        clearRuntimeCaches(player);
    }

    private JourneyRuntimeMaintenance() {}
}

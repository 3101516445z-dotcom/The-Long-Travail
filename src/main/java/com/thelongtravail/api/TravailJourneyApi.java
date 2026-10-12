package com.thelongtravail.api;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.data.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

// 服务端写入接口；不消耗物品。查询继续使用 TravailStateApi。
public final class TravailJourneyApi {
    public static JourneyState.AspectJourneyKey identity(
            ServerPlayer player, TravailAspect aspect) {
        return JourneyState.identity(com.thelongtravail.helper.TravailCurios.stack(player), aspect);
    }

    public static JourneyService.OperationResult completeRequirement(
            ServerPlayer player, TravailAspect aspect, JourneyOperations.RequirementRef target) {
        return JourneyService.mutate(
                player,
                Set.of(aspect),
                Set.of(),
                false,
                JourneyService.QueryPolicy.INVALIDATE,
                null,
                copy -> JourneyOperations.complete(JourneyState.root(copy), aspect, target));
    }

    public static JourneyService.OperationResult completeRandomRequirement(
            ServerPlayer player, TravailAspect aspect) {
        return JourneyService.mutate(
                player,
                Set.of(aspect),
                Set.of(),
                false,
                JourneyService.QueryPolicy.INVALIDATE,
                null,
                copy ->
                        JourneyOperations.completeRandom(
                                JourneyState.root(copy), aspect, player.getRandom()));
    }

    public static JourneyService.OperationResult resetAspect(
            ServerPlayer player, TravailAspect aspect) {
        return JourneyService.mutate(
                player,
                Set.of(aspect),
                Set.of(aspect),
                false,
                JourneyService.QueryPolicy.INVALIDATE,
                null,
                copy -> JourneyOperations.resetAspect(JourneyState.root(copy), aspect, player));
    }

    public static JourneyService.OperationResult resetJourney(ServerPlayer player) {
        return JourneyService.resetJourney(player, null);
    }

    public static JourneyService.OperationResult grantWitness(
            ServerPlayer player, JourneyState.AspectJourneyKey key, ResourceLocation source) {
        Objects.requireNonNull(key);
        return JourneyService.mutate(
                player,
                Set.of(key.aspect()),
                Set.of(),
                false,
                JourneyService.QueryPolicy.KEEP,
                null,
                copy -> JourneyOperations.grantWitness(JourneyState.root(copy), key, source));
    }

    public static JourneyService.OperationResult setOverride(
            ServerPlayer player, TravailAspect aspect, JourneyOperations.Override value) {
        return override(player, Set.of(aspect), value);
    }

    public static JourneyService.OperationResult setAllOverrides(
            ServerPlayer player, JourneyOperations.Override value) {
        return override(player, Set.of(TravailAspect.values()), value);
    }

    public static JourneyService.OperationResult clearOverride(
            ServerPlayer player, TravailAspect aspect) {
        return setOverride(player, aspect, JourneyOperations.Override.NATURAL);
    }

    public static JourneyService.OperationResult clearAllOverrides(ServerPlayer player) {
        return setAllOverrides(player, JourneyOperations.Override.NATURAL);
    }

    private static JourneyService.OperationResult override(
            ServerPlayer player, Set<TravailAspect> aspects, JourneyOperations.Override value) {
        return JourneyService.mutate(
                player,
                aspects,
                Set.of(),
                false,
                JourneyService.QueryPolicy.KEEP,
                null,
                copy -> JourneyOperations.override(JourneyState.root(copy), aspects, value));
    }

    private TravailJourneyApi() {}
}

package com.thelongtravail.farreach;

import com.thelongtravail.api.CarryingApi;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.ServerPlayer;

public final class BabelCarrying {
    public static void inventoryTick(ServerPlayer p, int slot) {
        CarryingApi.inventoryTick(p, ModRegistry.TOWER_OF_BABEL.get(), slot);
    }

    public static boolean carrying(ServerPlayer p) {
        return CarryingApi.query(p, ModRegistry.TOWER_OF_BABEL.get()) != CarryingApi.CarryingResult.ABSENT;
    }

    public static void forget(ServerPlayer p) {
        CarryingApi.forget(p);
    }

    private BabelCarrying() {}
}

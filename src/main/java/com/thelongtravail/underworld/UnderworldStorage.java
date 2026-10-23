package com.thelongtravail.underworld;

import com.thelongtravail.data.HotPathMetrics;
import com.thelongtravail.data.HotPathMetrics.Counter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import top.theillusivec4.curios.api.CuriosApi;

public final class UnderworldStorage {
    public static void loaded(BlockEntity blockEntity) {
        com.thelongtravail.compat.gravestone.GravestoneStorage.loaded(blockEntity);
    }

    public static void afterLoads(MinecraftServer server) {
        com.thelongtravail.compat.gravestone.GravestoneStorage.afterLoads(server);
    }

    public static void clearPending() {
        com.thelongtravail.compat.gravestone.GravestoneStorage.clearPending();
    }

    public static void player(ServerPlayer player) {
        var ledger = UnderworldLedger.get(player.server);
        if (!ledger.hasChanges()) return;
        long started = HotPathMetrics.start();
        try {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
                ledger.reconcile(player.getInventory().getItem(slot));
            CuriosApi.getCuriosInventory(player)
                    .ifPresent(
                            inventory ->
                                    inventory
                                            .getCurios()
                                            .values()
                                            .forEach(
                                                    handler -> {
                                                        for (var items :
                                                                java.util.List.of(
                                                                        handler.getStacks(),
                                                                        handler
                                                                                .getCosmeticStacks()))
                                                            for (int slot = 0;
                                                                    slot < items.getSlots();
                                                                    slot++)
                                                                ledger.reconcile(
                                                                        items.getStackInSlot(slot));
                                                    }));
            ledger.reconcileTag(player.getPersistentData(), 0);
        } finally {
            HotPathMetrics.elapsed(Counter.STORAGE_PLAYER_NANOS, started);
        }
    }

    public static void grave(BlockEntity blockEntity) {
        com.thelongtravail.compat.gravestone.GravestoneStorage.grave(blockEntity);
    }

    public static void sweep(MinecraftServer server) {
        sweep(server, null);
    }

    static void sweep(MinecraftServer server, ServerPlayer alreadyReconciled) {
        Counter.STORAGE_SWEEPS.add(1);
        var ledger = UnderworldLedger.get(server);
        ledger.recordSize();
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            if (player != alreadyReconciled) player(player);
        long started = HotPathMetrics.start();
        try {
            for (var level : server.getAllLevels())
                for (var item : DroppedItemIndex.snapshot(level)) {
                    Counter.STORAGE_ENTITY_VISITS.add(1);
                    ledger.reconcile(item.getItem());
                }
        } finally {
            HotPathMetrics.elapsed(Counter.STORAGE_ITEM_NANOS, started);
        }
        com.thelongtravail.compat.gravestone.GravestoneStorage.sweep(server);
        ledger.recordSize();
    }

    private UnderworldStorage() {}
}

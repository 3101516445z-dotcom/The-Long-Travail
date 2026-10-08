package com.thelongtravail.underworld;

import com.thelongtravail.data.HotPathMetrics;
import com.thelongtravail.data.HotPathMetrics.Counter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

public final class UnderworldStorage {
    private static final java.util.Queue<BlockEntity> LOADED = new java.util.concurrent.ConcurrentLinkedQueue<>();
    public static void loaded(BlockEntity blockEntity) { LOADED.add(blockEntity); }
    public static void afterLoads(MinecraftServer server) {
        BlockEntity blockEntity;
        while ((blockEntity = LOADED.poll()) != null) {
            if (!blockEntity.isRemoved() && blockEntity.getLevel() != null && blockEntity.getLevel().getServer() == server)
                grave(blockEntity);
        }
    }
    public static void clearPending() { LOADED.clear(); }
    public static void player(ServerPlayer player) {
        var ledger = UnderworldLedger.get(player.server);
        if (!ledger.hasChanges()) return;
        long started = HotPathMetrics.start();
        try {
            for (int slot=0; slot<player.getInventory().getContainerSize(); slot++) ledger.reconcile(player.getInventory().getItem(slot));
            CuriosApi.getCuriosInventory(player).ifPresent(inventory -> inventory.getCurios().values().forEach(handler -> {
                for (var items : java.util.List.of(handler.getStacks(), handler.getCosmeticStacks()))
                    for (int slot=0; slot<items.getSlots(); slot++) ledger.reconcile(items.getStackInSlot(slot));
            }));
            ledger.reconcileTag(player.getPersistentData(), 0);
        } finally { HotPathMetrics.elapsed(Counter.STORAGE_PLAYER_NANOS, started); }
    }
    public static void grave(BlockEntity blockEntity) {
        if (blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide) return;
        long started = HotPathMetrics.start();
        try {
            var ledger = UnderworldLedger.get(blockEntity.getLevel().getServer());
            String key = blockEntity.getLevel().dimension().location() + "|" + blockEntity.getBlockPos().asLong();
            Iterable<?> items = GravestoneAccess.items(blockEntity);
            if (items == null) return;
            boolean relevant = false;
            for (Object entry : items) {
                if (!(entry instanceof ItemStack stack)) throw new IllegalStateException("Gravestone getAllItems returned a non-ItemStack entry");
                relevant |= stack.hasTag() && stack.getTag().hasUUID(UnderworldLedger.BOOK_ID);
                ledger.reconcile(stack);
            }
            if (relevant && ledger.graves.add(key)) ledger.setDirty();
            blockEntity.setChanged();
        } finally { HotPathMetrics.elapsed(Counter.STORAGE_GRAVE_NANOS, started); }
    }
    public static void sweep(MinecraftServer server) { sweep(server, null); }
    // 已在同一同步事务中完成校正的玩家不重复遍历；不建立跨事件缓存。
    static void sweep(MinecraftServer server, ServerPlayer alreadyReconciled) {
        Counter.STORAGE_SWEEPS.add(1);
        var ledger = UnderworldLedger.get(server);
        ledger.recordSize();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) if (player != alreadyReconciled) player(player);
        long started = HotPathMetrics.start();
        try {
            for (var level : server.getAllLevels()) for (var entity : level.getAllEntities()) {
                Counter.STORAGE_ENTITY_VISITS.add(1);
                if (entity instanceof ItemEntity item) ledger.reconcile(item.getItem());
            }
        } finally { HotPathMetrics.elapsed(Counter.STORAGE_ITEM_NANOS, started); }
        for (String key : java.util.List.copyOf(ledger.graves)) {
            int separator = key.lastIndexOf('|');
            var level = server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(key.substring(0, separator))));
            BlockPos position = BlockPos.of(Long.parseLong(key.substring(separator+1)));
            if (level == null || !level.hasChunkAt(position)) continue;
            var blockEntity = level.getBlockEntity(position);
            if (blockEntity != null && blockEntity.getClass().getName().equals(GravestoneAccess.TILE)) grave(blockEntity);
            else { ledger.graves.remove(key); ledger.setDirty(); }
        }
        ledger.recordSize();
    }
    private UnderworldStorage() {}
}

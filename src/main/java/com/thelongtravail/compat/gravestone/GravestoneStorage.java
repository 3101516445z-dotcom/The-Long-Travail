package com.thelongtravail.compat.gravestone;

import com.thelongtravail.compat.OptionalCompatState;
import com.thelongtravail.data.HotPathMetrics.Counter;
import com.thelongtravail.data.HotPathMetrics;
import com.thelongtravail.underworld.UnderworldLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class GravestoneStorage {
    private static final java.util.Queue<BlockEntity> LOADED =
            new java.util.concurrent.ConcurrentLinkedQueue<>();

    public static void loaded(BlockEntity blockEntity) {
        if (OptionalCompatState.GRAVESTONE.enabled()) LOADED.add(blockEntity);
    }

    public static void afterLoads(MinecraftServer server) {
        if (!OptionalCompatState.GRAVESTONE.enabled()) {
            clearPending();
            return;
        }
        BlockEntity blockEntity;
        while ((blockEntity = LOADED.poll()) != null) {
            if (!OptionalCompatState.GRAVESTONE.enabled()) {
                clearPending();
                return;
            }
            if (!blockEntity.isRemoved()
                    && blockEntity.getLevel() != null
                    && blockEntity.getLevel().getServer() == server) grave(blockEntity);
        }
    }

    public static void clearPending() {
        LOADED.clear();
    }

    public static void grave(BlockEntity blockEntity) {
        if (!OptionalCompatState.GRAVESTONE.enabled()
                || blockEntity.getLevel() == null
                || blockEntity.getLevel().isClientSide) return;
        long started = HotPathMetrics.start();
        try {
            Iterable<?> items = GravestoneAccess.items(blockEntity);
            if (items == null) return;
            var stacks = new java.util.ArrayList<ItemStack>();
            for (Object entry : items) {
                if (!(entry instanceof ItemStack stack)) {
                    OptionalCompatState.GRAVESTONE.disable(
                            "getAllItems returned a non-ItemStack entry");
                    clearPending();
                    return;
                }
                stacks.add(stack);
            }
            var ledger = UnderworldLedger.get(blockEntity.getLevel().getServer());
            String key =
                    blockEntity.getLevel().dimension().location()
                            + "|"
                            + blockEntity.getBlockPos().asLong();
            boolean relevant = false;
            for (ItemStack stack : stacks) {
                relevant |= stack.hasTag() && stack.getTag().hasUUID(UnderworldLedger.BOOK_ID);
                ledger.reconcile(stack);
            }
            if (relevant && ledger.graves.add(key)) ledger.setDirty();
            blockEntity.setChanged();
        } finally {
            HotPathMetrics.elapsed(Counter.STORAGE_GRAVE_NANOS, started);
        }
    }

    public static void sweep(MinecraftServer server) {
        if (!OptionalCompatState.GRAVESTONE.enabled()) return;
        var ledger = UnderworldLedger.get(server);
        for (String key : java.util.List.copyOf(ledger.graves)) {
            if (!OptionalCompatState.GRAVESTONE.enabled()) return;
            int separator = key.lastIndexOf('|');
            var level =
                    server.getLevel(
                            ResourceKey.create(
                                    Registries.DIMENSION,
                                    new ResourceLocation(key.substring(0, separator))));
            BlockPos position = BlockPos.of(Long.parseLong(key.substring(separator + 1)));
            if (level == null || !level.hasChunkAt(position)) continue;
            var blockEntity = level.getBlockEntity(position);
            if (blockEntity != null
                    && blockEntity.getClass().getName().equals(GravestoneAccess.TILE))
                grave(blockEntity);
            else {
                ledger.graves.remove(key);
                ledger.setDirty();
            }
        }
    }

    private GravestoneStorage() {}
}

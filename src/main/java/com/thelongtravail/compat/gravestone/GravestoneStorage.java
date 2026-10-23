package com.thelongtravail.compat.gravestone;

import com.thelongtravail.underworld.UnderworldLedger;
import com.thelongtravail.compat.gravestone.GravestoneAccess;

import com.thelongtravail.data.HotPathMetrics;
import com.thelongtravail.data.HotPathMetrics.Counter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;

public final class GravestoneStorage {
    private static final java.util.Queue<BlockEntity> LOADED =
            new java.util.concurrent.ConcurrentLinkedQueue<>();

    public static void loaded(BlockEntity blockEntity) {
        LOADED.add(blockEntity);
    }

    public static void afterLoads(MinecraftServer server) {
        BlockEntity blockEntity;
        while ((blockEntity = LOADED.poll()) != null) {
            if (!blockEntity.isRemoved()
                    && blockEntity.getLevel() != null
                    && blockEntity.getLevel().getServer() == server) grave(blockEntity);
        }
    }

    public static void clearPending() {
        LOADED.clear();
    }

    public static void grave(BlockEntity blockEntity) {
        if (blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide) return;
        long started = HotPathMetrics.start();
        try {
            var ledger = UnderworldLedger.get(blockEntity.getLevel().getServer());
            String key =
                    blockEntity.getLevel().dimension().location()
                            + "|"
                            + blockEntity.getBlockPos().asLong();
            Iterable<?> items = GravestoneAccess.items(blockEntity);
            if (items == null) return;
            boolean relevant = false;
            for (Object entry : items) {
                if (!(entry instanceof ItemStack stack))
                    throw new IllegalStateException(
                            "Gravestone getAllItems returned a non-ItemStack entry");
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
        var ledger = UnderworldLedger.get(server);
        for (String key : java.util.List.copyOf(ledger.graves)) {
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

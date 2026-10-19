package com.thelongtravail.valley;

import com.google.common.collect.ImmutableList;
import com.thelongtravail.TheLongTravail;
import com.thelongtravail.mixin.ExecutionRawEntity;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.entity.EntityInLevelCallback;

public final class ExecutionCleanup {
    public static boolean quarantined(Entity entity) {
        return entity instanceof ExecutionBody body
                && (body.travail$cleanupPending() || body.travail$executionTerminated());
    }

    public static boolean detached(LivingEntity entity) {
        return detached(entity, true);
    }

    public static boolean detached(LivingEntity entity, boolean full) {
        var storage = (ExecutionStorage) entity.level();
        return ((ExecutionRawEntity) entity).travail$reason() == Entity.RemovalReason.KILLED
                && !(full ? storage.travail$contains(entity) : storage.travail$containsKnown(entity));
    }

    public static void rawDetach(LivingEntity entity) {
        rawDetach(entity, true);
    }

    public static void rawDetach(LivingEntity entity, boolean full) {
        if (entity instanceof Player) throw new IllegalArgumentException("Player lifecycle cannot be force detached");
        var raw = (ExecutionRawEntity) entity;
        raw.travail$reason(Entity.RemovalReason.KILLED);
        // 先移除全部存储记录，再丢弃回调，以便清理失败时重试。
        if (full) ((ExecutionStorage) entity.level()).travail$erase(entity);
        else ((ExecutionStorage) entity.level()).travail$eraseKnown(entity);
        var vehicle = raw.travail$vehicle();
        if (vehicle != null) {
            var parent = (ExecutionRawEntity) vehicle;
            parent.travail$passengers(
                    ImmutableList.copyOf(parent.travail$passengers().stream().filter(e -> e != entity).toList()));
            raw.travail$vehicle(null);
        }
        for (var passenger : raw.travail$passengers()) {
            var child = (ExecutionRawEntity) passenger;
            if (child.travail$vehicle() == entity) child.travail$vehicle(null);
        }
        raw.travail$passengers(ImmutableList.of());
        raw.travail$callback(EntityInLevelCallback.NULL);
        raw.travail$added(false);
        if (!detached(entity, full))
            throw new IllegalStateException("Stage five storage cleanup incomplete: " + entity.getUUID());
    }

    public static void finishServer(LivingEntity entity) {
        var level = (ServerLevel) entity.level();
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Cleanup requires server thread");
        boolean notify = entity.isAddedToWorld();
        rawDetach(entity);
        if (notify) notifyRemoval(entity);
        try {
            entity.updateDynamicGameEventListener((listener, world) -> listener.remove(world));
        } catch (RuntimeException ex) {
            TheLongTravail.LOGGER.warn("Execution listener cleanup failed for {}", entity.getUUID(), ex);
        }
        level.getScoreboard().entityRemoved(entity);
    }

    public static void notifyRemoval(LivingEntity entity) {
        try {
            entity.onRemovedFromWorld();
        } catch (RuntimeException ex) {
            TheLongTravail.LOGGER.warn("Execution removed callback failed for {}", entity.getUUID(), ex);
        }
        try {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                    new net.minecraftforge.event.entity.EntityLeaveLevelEvent(entity, entity.level()));
        } catch (RuntimeException ex) {
            TheLongTravail.LOGGER.warn("Execution leave event failed for {}", entity.getUUID(), ex);
        }
        // 第三方回调可能绕过常规加入事件，将实体重新插入世界。
        if (!detached(entity)) rawDetach(entity);
    }

    private ExecutionCleanup() {}
}

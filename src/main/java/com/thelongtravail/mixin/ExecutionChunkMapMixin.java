package com.thelongtravail.mixin;

import com.thelongtravail.valley.ExecutionStorage;
import com.thelongtravail.TheLongTravail;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ChunkMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.List;
import org.spongepowered.asm.mixin.*;

@Mixin(ChunkMap.class)
public abstract class ExecutionChunkMapMixin implements ExecutionStorage {
    @Shadow @Final private Int2ObjectMap<?> entityMap;

    public boolean travail$contains(Entity e) {
        var entry = entityMap.get(e.getId());
        return entry instanceof ExecutionTrackedAccessor a && a.travail$entity() == e;
    }

    public void travail$erase(Entity e) {
        var entry = entityMap.get(e.getId());
        if (!(entry instanceof ExecutionTrackedAccessor a) || a.travail$entity() != e) return;
        for (var viewer : List.copyOf(a.travail$seenBy())) {
            try {
                a.travail$serverEntity().removePairing(viewer.getPlayer());
            } catch (RuntimeException ex) {
                TheLongTravail.LOGGER.warn(
                        "Execution tracking callback failed for {}", e.getUUID(), ex);
            }
        }
        a.travail$seenBy().clear();
        // 移除回调可能注册相同 ID 的新追踪器。
        // TrackedEntity.equals 只比较 ID，remove(key, value) 无法保证移除的是原实例。
        if (entityMap.get(e.getId()) == entry) entityMap.remove(e.getId());
    }
}

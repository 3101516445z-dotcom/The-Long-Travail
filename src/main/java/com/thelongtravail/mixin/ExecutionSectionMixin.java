package com.thelongtravail.mixin;

import com.thelongtravail.valley.ExecutionStorage;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.level.entity.*;
import org.spongepowered.asm.mixin.*;

@Mixin(EntitySection.class)
public abstract class ExecutionSectionMixin<T extends EntityAccess> implements ExecutionStorage {
    @Shadow @Final private ClassInstanceMultiMap<T> storage;

    public java.util.Set<Entity> travail$present(java.util.Set<Entity> targets) {
        return ((ExecutionStorage) storage).travail$present(targets);
    }

    public void travail$erase(Entity e) {
        ((ExecutionStorage) storage).travail$erase(e);
    }

    public boolean travail$contains(Entity e) {
        return ((ExecutionStorage) storage).travail$contains(e);
    }
}

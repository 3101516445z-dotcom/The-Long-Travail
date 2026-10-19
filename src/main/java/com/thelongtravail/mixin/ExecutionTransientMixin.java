package com.thelongtravail.mixin;

import com.thelongtravail.valley.ExecutionStorage;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.*;
import java.util.*;
import org.spongepowered.asm.mixin.*;

@Mixin(TransientEntitySectionManager.class)
public abstract class ExecutionTransientMixin<T extends EntityAccess> implements ExecutionStorage {
    @Shadow @Final private EntityLookup<T> entityStorage;
    @Shadow @Final private EntitySectionStorage<T> sectionStorage;

    public void travail$eraseKnown(Entity e) {
        ((ExecutionStorage) sectionStorage).travail$eraseKnown(e);
        ((ExecutionStorage) entityStorage).travail$erase(e);
    }

    public boolean travail$containsKnown(Entity e) {
        return ((ExecutionStorage) entityStorage).travail$contains(e)
                || ((ExecutionStorage) sectionStorage).travail$containsKnown(e);
    }

    public void travail$erase(Entity e) {
        ((ExecutionStorage) sectionStorage).travail$erase(e);
        ((ExecutionStorage) entityStorage).travail$erase(e);
    }

    public boolean travail$contains(Entity e) {
        return ((ExecutionStorage) entityStorage).travail$contains(e)
                || ((ExecutionStorage) sectionStorage).travail$contains(e);
    }
}

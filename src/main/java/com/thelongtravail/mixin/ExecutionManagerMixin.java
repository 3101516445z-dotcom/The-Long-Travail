package com.thelongtravail.mixin;

import com.thelongtravail.valley.ExecutionStorage;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.*;
import java.util.*;
import org.spongepowered.asm.mixin.*;

@Mixin(PersistentEntitySectionManager.class)
public abstract class ExecutionManagerMixin<T extends EntityAccess> implements ExecutionStorage {
    @Shadow @Final private EntityLookup<T> visibleEntityStorage;
    @Shadow @Final private EntitySectionStorage<T> sectionStorage;
    @Shadow @Final private Set<UUID> knownUuids;

    public Set<Entity> travail$present(Set<Entity> targets) {
        var found = ((ExecutionStorage) sectionStorage).travail$present(targets);
        for (Entity e : targets)
            if (((ExecutionStorage) visibleEntityStorage).travail$contains(e)
                    || (knownUuids.contains(e.getUUID()) && visibleEntityStorage.getEntity(e.getUUID()) == null))
                found.add(e);
        return found;
    }

    public void travail$eraseKnown(Entity e) {
        ((ExecutionStorage) sectionStorage).travail$eraseKnown(e);
        ((ExecutionStorage) visibleEntityStorage).travail$erase(e);
        if (visibleEntityStorage.getEntity(e.getUUID()) == null) knownUuids.remove(e.getUUID());
    }

    public boolean travail$containsKnown(Entity e) {
        return ((ExecutionStorage) visibleEntityStorage).travail$contains(e)
                || ((ExecutionStorage) sectionStorage).travail$containsKnown(e)
                || (knownUuids.contains(e.getUUID()) && visibleEntityStorage.getEntity(e.getUUID()) == null);
    }

    public void travail$erase(Entity e) {
        ((ExecutionStorage) sectionStorage).travail$erase(e);
        ((ExecutionStorage) visibleEntityStorage).travail$erase(e);
        if (visibleEntityStorage.getEntity(e.getUUID()) == null) knownUuids.remove(e.getUUID());
    }

    public boolean travail$contains(Entity e) {
        return ((ExecutionStorage) visibleEntityStorage).travail$contains(e)
                || ((ExecutionStorage) sectionStorage).travail$contains(e)
                || (knownUuids.contains(e.getUUID()) && visibleEntityStorage.getEntity(e.getUUID()) == null);
    }
}

package com.thelongtravail.valley;

import net.minecraft.world.entity.Entity;

public interface ExecutionStorage {
    default java.util.Set<Entity> travail$present(java.util.Set<Entity> targets) {
        java.util.Set<Entity> found =
                java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (Entity entity : targets) if (travail$contains(entity)) found.add(entity);
        return found;
    }

    void travail$erase(Entity entity);

    boolean travail$contains(Entity entity);

    default void travail$eraseKnown(Entity entity) {
        travail$erase(entity);
    }

    default boolean travail$containsKnown(Entity entity) {
        return travail$contains(entity);
    }
}

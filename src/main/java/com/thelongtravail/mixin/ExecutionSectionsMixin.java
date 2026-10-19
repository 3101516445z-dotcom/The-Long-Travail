package com.thelongtravail.mixin;

import com.thelongtravail.valley.ExecutionStorage;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.*;
import it.unimi.dsi.fastutil.longs.*;
import org.spongepowered.asm.mixin.*;

@Mixin(EntitySectionStorage.class)
public abstract class ExecutionSectionsMixin<T extends EntityAccess> implements ExecutionStorage {
    @Shadow @Final private Long2ObjectMap<EntitySection<T>> sections;
    @Shadow @Final private LongSortedSet sectionIds;

    public void travail$eraseKnown(Entity e) {
        long key = net.minecraft.core.SectionPos.asLong(e.blockPosition());
        var section = sections.get(key);
        if (section == null || !((ExecutionStorage) section).travail$contains(e)) return;
        ((ExecutionStorage) section).travail$erase(e);
        if (section.isEmpty()) {
            sectionIds.remove(key);
            sections.remove(key, section);
        }
    }

    public boolean travail$containsKnown(Entity e) {
        var section = sections.get(net.minecraft.core.SectionPos.asLong(e.blockPosition()));
        return section != null && ((ExecutionStorage) section).travail$contains(e);
    }

    public void travail$erase(Entity e) {
        var it = sections.long2ObjectEntrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            var section = entry.getValue();
            if (!((ExecutionStorage) section).travail$contains(e)) continue;
            ((ExecutionStorage) section).travail$erase(e);
            if (section.isEmpty()) {
                sectionIds.remove(entry.getLongKey());
                it.remove();
            }
        }
    }

    public java.util.Set<Entity> travail$present(java.util.Set<Entity> targets) {
        com.thelongtravail.data.HotPathMetrics.Counter.EXECUTION_SECTION_BATCHES.add(1);
        java.util.Set<Entity> found = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (var section : sections.values()) {
            com.thelongtravail.data.HotPathMetrics.Counter.EXECUTION_SECTION_VISITS.add(1);
            found.addAll(((ExecutionStorage) section).travail$present(targets));
        }
        return found;
    }

    public boolean travail$contains(Entity e) {
        for (var section : sections.values()) if (((ExecutionStorage) section).travail$contains(e)) return true;
        return false;
    }
}

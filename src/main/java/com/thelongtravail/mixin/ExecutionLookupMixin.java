package com.thelongtravail.mixin;

import com.thelongtravail.valley.ExecutionStorage;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.*;
import org.spongepowered.asm.mixin.*;

@Mixin(EntityLookup.class)
public abstract class ExecutionLookupMixin<T extends EntityAccess> implements ExecutionStorage {
    @Shadow @Final private Int2ObjectMap<T> byId;
    @Shadow @Final private Map<UUID, T> byUuid;

    public void travail$erase(Entity e) {
        if (byId.get(e.getId()) == e) byId.remove(e.getId());
        if (byUuid.get(e.getUUID()) == e) byUuid.remove(e.getUUID());
    }

    public boolean travail$contains(Entity e) {
        return byId.get(e.getId()) == e || byUuid.get(e.getUUID()) == e;
    }
}

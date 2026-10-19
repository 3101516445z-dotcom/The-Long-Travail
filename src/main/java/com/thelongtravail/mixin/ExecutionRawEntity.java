package com.thelongtravail.mixin;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface ExecutionRawEntity {
    @Accessor("removalReason")
    Entity.RemovalReason travail$reason();

    @Accessor("removalReason")
    void travail$reason(Entity.RemovalReason reason);

    @Accessor("levelCallback")
    void travail$callback(EntityInLevelCallback callback);

    @Accessor("vehicle")
    Entity travail$vehicle();

    @Accessor("vehicle")
    void travail$vehicle(Entity vehicle);

    @Accessor("passengers")
    ImmutableList<Entity> travail$passengers();

    @Accessor("passengers")
    void travail$passengers(ImmutableList<Entity> passengers);

    @Accessor(value = "isAddedToWorld", remap = false)
    void travail$added(boolean added);
}

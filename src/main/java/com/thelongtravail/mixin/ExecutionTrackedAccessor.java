package com.thelongtravail.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.network.ServerPlayerConnection;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public interface ExecutionTrackedAccessor {
    @Accessor("entity")
    Entity travail$entity();

    @Accessor("serverEntity")
    ServerEntity travail$serverEntity();

    @Accessor("seenBy")
    Set<ServerPlayerConnection> travail$seenBy();
}

package com.thelongtravail.mixin;

import com.thelongtravail.valley.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.*;
import org.spongepowered.asm.mixin.*;

@Mixin(ClientLevel.class)
public abstract class ExecutionClientLevelMixin implements ExecutionStorage {
    @Shadow @Final private EntityTickList tickingEntities;

    @Shadow(remap = false)
    @Final
    private Int2ObjectMap<net.minecraftforge.entity.PartEntity<?>> partEntities;

    @Shadow @Final private TransientEntitySectionManager<Entity> entityStorage;

    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void travail$rejectRestoredSpawn(int id, Entity entity, CallbackInfo ci) {
        if (com.thelongtravail.client.ExecutionTerminationClient.suppressIncoming(entity, id)) ci.cancel();
    }

    public void travail$erase(Entity e) {
        ((ExecutionStorage) tickingEntities).travail$erase(e);
        ((ExecutionStorage) entityStorage).travail$erase(e);
        partEntities.values().removeIf(part -> part.getParent() == e);
    }

    public void travail$eraseKnown(Entity e) {
        ((ExecutionStorage) tickingEntities).travail$erase(e);
        ((ExecutionStorage) entityStorage).travail$eraseKnown(e);
        partEntities.values().removeIf(part -> part.getParent() == e);
    }

    public boolean travail$containsKnown(Entity e) {
        return ((ExecutionStorage) tickingEntities).travail$contains(e)
                || ((ExecutionStorage) entityStorage).travail$containsKnown(e)
                || partEntities.values().stream().anyMatch(part -> part.getParent() == e);
    }

    public boolean travail$contains(Entity e) {
        return ((ExecutionStorage) tickingEntities).travail$contains(e)
                || ((ExecutionStorage) entityStorage).travail$contains(e)
                || partEntities.values().stream().anyMatch(part -> part.getParent() == e);
    }

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void travail$stop(Entity e, CallbackInfo ci) {
        if (com.thelongtravail.client.ExecutionTerminationClient.suppress(e)) ci.cancel();
    }

    @Inject(method = "tickPassenger", at = @At("HEAD"), cancellable = true)
    private void travail$stopPassenger(Entity vehicle, Entity e, CallbackInfo ci) {
        if (com.thelongtravail.client.ExecutionTerminationClient.suppress(e)) ci.cancel();
    }
}

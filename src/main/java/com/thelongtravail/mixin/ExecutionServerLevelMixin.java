package com.thelongtravail.mixin;

import com.thelongtravail.valley.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.entity.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Set;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ExecutionServerLevelMixin implements ExecutionStorage {
    @Shadow @Final private EntityTickList entityTickList;
    @Shadow @Final private PersistentEntitySectionManager<Entity> entityManager;
    @Shadow @Final private Set<Mob> navigatingMobs;
    @Shadow private volatile boolean isUpdatingNavigations;
    @Shadow @Final private Int2ObjectMap<net.minecraftforge.entity.PartEntity<?>> dragonParts;

    @Inject(method = "addEntity", at = @At("RETURN"))
    private void travail$observeExecutionDrop(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && entity instanceof net.minecraft.world.entity.item.ItemEntity)
            ExecutionLoot.admitted(entity);
    }

    public void travail$erase(Entity e) {
        travail$eraseBody(e, true);
    }

    public void travail$eraseKnown(Entity e) {
        travail$eraseBody(e, false);
    }

    @Unique
    private void travail$eraseBody(Entity e, boolean full) {
        // 保留原集合及其引用；清理失败时等到下一游戏刻边界再重试。
        if (isUpdatingNavigations && navigatingMobs.contains(e))
            throw new IllegalStateException(
                    "Execution cleanup deferred during navigation iteration");
        ((ExecutionStorage) entityTickList).travail$erase(e);
        if (full) ((ExecutionStorage) entityManager).travail$erase(e);
        else ((ExecutionStorage) entityManager).travail$eraseKnown(e);
        if (navigatingMobs.contains(e)) navigatingMobs.removeIf(value -> value == e);
        dragonParts.values().removeIf(part -> part.getParent() == e);
        ((ExecutionStorage) ((ServerLevel) (Object) this).getChunkSource().chunkMap)
                .travail$erase(e);
    }

    public Set<Entity> travail$present(Set<Entity> targets) {
        var found = ((ExecutionStorage) entityManager).travail$present(targets);
        for (Entity e : targets)
            if (((ExecutionStorage) entityTickList).travail$contains(e)
                    || ((ExecutionStorage) ((ServerLevel) (Object) this).getChunkSource().chunkMap)
                            .travail$contains(e)
                    || navigatingMobs.stream().anyMatch(value -> value == e)
                    || dragonParts.values().stream().anyMatch(part -> part.getParent() == e))
                found.add(e);
        return found;
    }

    public boolean travail$contains(Entity e) {
        return ((ExecutionStorage) entityTickList).travail$contains(e)
                || ((ExecutionStorage) entityManager).travail$contains(e)
                || ((ExecutionStorage) ((ServerLevel) (Object) this).getChunkSource().chunkMap)
                        .travail$contains(e)
                || navigatingMobs.stream().anyMatch(value -> value == e)
                || dragonParts.values().stream().anyMatch(part -> part.getParent() == e);
    }

    public boolean travail$containsKnown(Entity e) {
        return ((ExecutionStorage) entityTickList).travail$contains(e)
                || ((ExecutionStorage) entityManager).travail$containsKnown(e)
                || ((ExecutionStorage) ((ServerLevel) (Object) this).getChunkSource().chunkMap)
                        .travail$contains(e)
                || navigatingMobs.contains(e)
                || dragonParts.values().stream().anyMatch(part -> part.getParent() == e);
    }

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void travail$stopTerminated(Entity e, CallbackInfo ci) {
        if (ExecutionCleanup.quarantined(e)) ci.cancel();
    }

    @Inject(method = "tickPassenger", at = @At("HEAD"), cancellable = true)
    private void travail$stopPassenger(Entity vehicle, Entity e, CallbackInfo ci) {
        if (ExecutionCleanup.quarantined(e)) ci.cancel();
    }

    @Inject(method = "save", at = @At("HEAD"))
    private void travail$beforeSave(CallbackInfo ci) {
        ExecutionWatch.sweep((ServerLevel) (Object) this);
    }
}

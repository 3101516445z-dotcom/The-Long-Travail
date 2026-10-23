package com.thelongtravail.mixin;

import com.thelongtravail.valley.*;
import net.minecraft.world.entity.*;
import net.minecraft.network.syncher.*;
import org.spongepowered.asm.mixin.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(SynchedEntityData.class)
public abstract class ExecutionDataMixin implements ExecutionData {
    @Shadow @Final private Entity entity;
    @Shadow private boolean isDirty;

    @Shadow
    protected abstract <T> SynchedEntityData.DataItem<T> getItem(EntityDataAccessor<T> key);

    public float travail$storedHealth() {
        return getItem(ExecutionHealthAccessor.travail$healthKey()).getValue();
    }

    public void travail$commitHealth(float health) {
        var key = ExecutionHealthAccessor.travail$healthKey();
        var item = getItem(key);
        item.setValue(health);
        entity.onSyncedDataUpdated(key);
        item.setDirty(true);
        isDirty = true;
    }

    @WrapMethod(
            method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;Z)V")
    private <T> void travail$holdDeath(
            EntityDataAccessor<T> key, T value, boolean force, Operation<Void> original) {
        if (entity instanceof LivingEntity living
                && FinalExecution.locked(living)
                && key.equals(ExecutionHealthAccessor.travail$healthKey())) return;
        original.call(key, value, force);
    }
}

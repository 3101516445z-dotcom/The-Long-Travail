package com.thelongtravail.mixin.execution;

import com.thelongtravail.valley.execution.ExecutionBody;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class ExecutionStateMixin implements ExecutionBody {
    @Unique private com.thelongtravail.valley.execution.ExecutionLoot.State travail$executionLoot;

    public com.thelongtravail.valley.execution.ExecutionLoot.State travail$executionLoot() {
        return travail$executionLoot;
    }

    public void travail$executionLoot(
            com.thelongtravail.valley.execution.ExecutionLoot.State value) {
        travail$executionLoot = value;
    }

    @Unique private com.thelongtravail.valley.execution.ExecutionLease travail$executionLease;

    public com.thelongtravail.valley.execution.ExecutionLease travail$executionLease() {
        return travail$executionLease;
    }

    public void travail$executionLease(com.thelongtravail.valley.execution.ExecutionLease value) {
        travail$executionLease = value;
    }

    @Unique private volatile boolean travail$rawCleanupRequired;

    public boolean travail$rawCleanupRequired() {
        return travail$rawCleanupRequired;
    }

    public void travail$rawCleanupRequired(boolean value) {
        travail$rawCleanupRequired = value;
    }

    @Unique private volatile boolean travail$cleanupPending;

    public boolean travail$cleanupPending() {
        return travail$cleanupPending;
    }

    public void travail$cleanupPending(boolean value) {
        travail$cleanupPending = value;
    }

    @Unique private volatile boolean travail$executionTerminated;

    public boolean travail$executionTerminated() {
        return travail$executionTerminated;
    }

    public void travail$executionTerminated(boolean value) {
        travail$executionTerminated = value;
    }

    @Unique private volatile boolean travail$executionLocked;

    public boolean travail$executionLocked() {
        return travail$executionLocked;
    }

    public void travail$executionLocked(boolean value) {
        travail$executionLocked = value;
    }
}

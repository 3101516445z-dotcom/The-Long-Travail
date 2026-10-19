package com.thelongtravail.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface ExecutionHealthAccessor {
    @Accessor("DATA_HEALTH_ID")
    static EntityDataAccessor<Float> travail$healthKey() {
        throw new AssertionError();
    }
}

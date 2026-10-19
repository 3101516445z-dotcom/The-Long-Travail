package com.thelongtravail.mixin;

import com.thelongtravail.valley.ExecutionCleanup;
import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ExecutionPersistenceMixin {
    @Inject(method = "saveAsPassenger", at = @At("HEAD"), cancellable = true)
    private void travail$excludeTerminated(CompoundTag tag, CallbackInfoReturnable<Boolean> ci) {
        if (ExecutionCleanup.quarantined((Entity) (Object) this)) ci.setReturnValue(false);
    }
}

package com.thelongtravail.mixin;

import com.thelongtravail.valley.ExecutionCleanup;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerEntity.class)
public abstract class ExecutionTrackingMixin {
    @Shadow @Final private Entity entity;

    @Inject(
            method = {"addPairing", "sendPairingData", "sendChanges"},
            at = @At("HEAD"),
            cancellable = true)
    private void travail$stopRestoredTracking(CallbackInfo ci) {
        if (ExecutionCleanup.quarantined(entity)) ci.cancel();
    }
}

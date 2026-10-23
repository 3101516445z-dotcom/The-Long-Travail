package com.thelongtravail.mixin;

import com.thelongtravail.client.ExecutionTerminationClient;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class ExecutionRenderMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void travail$hideRestored(
            Entity entity,
            Frustum frustum,
            double x,
            double y,
            double z,
            CallbackInfoReturnable<Boolean> ci) {
        if (ExecutionTerminationClient.suppress(entity)) ci.setReturnValue(false);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void travail$blockDirectRender(
            Entity entity,
            double x,
            double y,
            double z,
            float yaw,
            float partialTick,
            com.mojang.blaze3d.vertex.PoseStack pose,
            net.minecraft.client.renderer.MultiBufferSource buffers,
            int light,
            CallbackInfo ci) {
        if (ExecutionTerminationClient.suppress(entity)) ci.cancel();
    }
}

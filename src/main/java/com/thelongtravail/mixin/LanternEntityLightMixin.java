package com.thelongtravail.mixin;

import com.thelongtravail.client.LanternLightingClient;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class LanternEntityLightMixin {
    @Inject(method="getPackedLightCoords",at=@At("RETURN"),cancellable=true)
    private void travail$entityLantern(Entity e,float partial,CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(LanternLightingClient.light(Mth.lerp(partial,e.xo,e.getX()),Mth.lerp(partial,e.yo,e.getY())+e.getBbHeight()*.5,Mth.lerp(partial,e.zo,e.getZ()),cir.getReturnValueI()));
    }
}

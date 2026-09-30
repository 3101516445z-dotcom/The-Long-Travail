package com.thelongtravail.mixin;

import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Non-player speed gate; the Player override is handled alongside altitude in AltitudeSpeedMixin. */
@Mixin(LivingEntity.class)
public abstract class StiffSpeedMixin {
    @Inject(method = "getSpeed", at = @At("RETURN"), cancellable = true)
    private void travail$stiffSpeed(CallbackInfoReturnable<Float> cir) {
        // Read the active effect directly on both sides; no tick cache delays dispelling.
        if (((LivingEntity) (Object) this).hasEffect(ModRegistry.STIFF.get())) cir.setReturnValue(0.0F);
    }
}

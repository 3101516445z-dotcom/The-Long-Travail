package com.thelongtravail.mixin;

import com.thelongtravail.farreach.IcarusFlight;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.caelus.api.CaelusApi;

// 提供可选的飞行能力来源，但不覆盖 Caelus 明确禁止飞行的结果。
@Pseudo
@Mixin(targets = "top.theillusivec4.caelus.common.CaelusApiImpl", remap = false)
public abstract class IcarusCaelusApiMixin {
    @Inject(method = "canFallFly", at = @At("RETURN"), cancellable = true, remap = false)
    private void travail$icarusSource(LivingEntity entity, CallbackInfoReturnable<CaelusApi.TriState> cir) {
        if (cir.getReturnValue() == CaelusApi.TriState.DEFAULT && IcarusFlight.equipped(entity))
            cir.setReturnValue(CaelusApi.TriState.ALLOW);
    }
}

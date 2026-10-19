package com.thelongtravail.mixin;

import com.thelongtravail.farreach.BabelProtection;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetingConditions.class)
public abstract class BabelTargetingMixin {
    @Shadow @Final private boolean isCombat;

    @Inject(method = "test", at = @At("RETURN"), cancellable = true)
    private void travail$test(LivingEntity source, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (isCombat && source != null && BabelProtection.protects(source, target)) cir.setReturnValue(false);
    }
}

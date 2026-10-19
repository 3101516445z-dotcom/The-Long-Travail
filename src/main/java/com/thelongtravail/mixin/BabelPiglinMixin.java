package com.thelongtravail.mixin;

import com.thelongtravail.farreach.BabelProtection;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PiglinAi.class)
public abstract class BabelPiglinMixin {
    @Inject(method = "wasHurtBy", at = @At("HEAD"), cancellable = true)
    private static void travail$hurt(Piglin mob, LivingEntity player, CallbackInfo ci) {
        if (BabelProtection.protects(mob, player)) ci.cancel();
    }

    @Inject(method = "setAngerTarget", at = @At("HEAD"), cancellable = true)
    private static void travail$anger(AbstractPiglin mob, LivingEntity player, CallbackInfo ci) {
        if (BabelProtection.protects(mob, player)) ci.cancel();
    }
}

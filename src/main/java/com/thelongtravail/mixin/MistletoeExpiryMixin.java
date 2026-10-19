package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.flourishing.Mistletoe;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class MistletoeExpiryMixin {
    @WrapMethod(method = "tickEffects")
    private void travail$expiry(Operation<Void> original) {
        LivingEntity entity = (LivingEntity) (Object) this;
        try {
            original.call();
        } catch (RuntimeException | Error failure) {
            Mistletoe.abort(entity);
            throw failure;
        }
        Mistletoe.afterEffects(entity);
    }
}

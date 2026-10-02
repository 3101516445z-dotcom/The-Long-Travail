package com.thelongtravail.mixin;

import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 此处处理非玩家实体；Player 的重写方法由 AltitudeSpeedMixin 统一处理。 */
@Mixin(LivingEntity.class)
public abstract class StiffSpeedMixin {
    @Inject(method = "getSpeed", at = @At("RETURN"), cancellable = true)
    private void travail$stiffSpeed(CallbackInfoReturnable<Float> cir) {
        // 两端直接读取当前效果，避免 tick 缓存延迟驱散结果。
        if (((LivingEntity) (Object) this).hasEffect(ModRegistry.STIFF.get())) cir.setReturnValue(0.0F);
    }
}

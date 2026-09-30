package com.thelongtravail.mixin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(LivingEntity.class)
public interface EffectRemovalInvoker {
    @Invoker("onEffectRemoved") void travail$onEffectRemoved(MobEffectInstance instance);
}

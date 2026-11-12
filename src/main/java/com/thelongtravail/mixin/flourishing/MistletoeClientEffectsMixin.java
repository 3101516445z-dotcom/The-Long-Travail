package com.thelongtravail.mixin.flourishing;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.runtime.effects.EffectInsertion;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPacketListener.class)
public abstract class MistletoeClientEffectsMixin {
    @WrapOperation(
            method = "handleUpdateMobEffect",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;forceAddEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)V"))
    private void travail$receiveMistletoe(
            LivingEntity entity,
            MobEffectInstance effect,
            Entity source,
            Operation<Void> original) {
        original.call(entity, effect, source);
        if (effect.getEffect() != ModRegistry.MISTLETOE_EFFECT.get()
                || entity.getEffect(effect.getEffect()) == effect) return;
        // 客户端也可能拒收；仅将服务端已确认的槲寄生状态写入真实表。
        if (EffectInsertion.writeVerified(entity, effect))
            EffectInsertion.synchronize(entity, effect);
    }
}

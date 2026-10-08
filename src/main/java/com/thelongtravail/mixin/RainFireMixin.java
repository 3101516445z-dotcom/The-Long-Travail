package com.thelongtravail.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.abyss.RainState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(LivingEntity.class)
public abstract class RainFireMixin {
    @WrapOperation(method="hurt", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z"))
    private boolean travail$fireResistance(LivingEntity entity, MobEffect effect, Operation<Boolean> original) {
        return original.call(entity,effect) || effect==MobEffects.FIRE_RESISTANCE && entity instanceof Player p && RainState.fire(p);
    }
}

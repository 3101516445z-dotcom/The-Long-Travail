package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thelongtravail.data.AltitudePenalty;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class AltitudeFlightMixin {
    // 疾跑飞行和普通飞行均读取此值；不能直接修改能力对象中的速度。
    @WrapOperation(
            method = "getFlyingSpeed",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/player/Abilities;getFlyingSpeed()F"),
            require = 2,
            allow = 2)
    private float travail$flightSpeed(Abilities abilities, Operation<Float> original) {
        return original.call(abilities) * AltitudePenalty.factor((Player) (Object) this);
    }
}

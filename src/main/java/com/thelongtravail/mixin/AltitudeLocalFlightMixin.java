package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thelongtravail.data.AltitudePenalty;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class AltitudeLocalFlightMixin {
    // 本地输入会单独读取能力速度，用于跳跃和潜行时的垂直加速。
    @WrapOperation(
            method = "aiStep",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/player/Abilities;getFlyingSpeed()F"),
            require = 1,
            allow = 1)
    private float travail$verticalFlightSpeed(Abilities abilities, Operation<Float> original) {
        return original.call(abilities) * AltitudePenalty.factor((LocalPlayer) (Object) this);
    }
}

package com.thelongtravail.mixin.movement;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thelongtravail.runtime.effects.RevivalMovement;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPacketListener.class)
public abstract class RevivalClientExplosionMixin {
    @WrapOperation(
            method = "handleExplosion",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/player/LocalPlayer;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"),
            require = 1,
            allow = 1)
    private void travail$explosionImpulse(
            LocalPlayer player, Vec3 velocity, Operation<Void> original) {
        Vec3 before = player.getDeltaMovement();
        original.call(player, velocity);
        ((RevivalMovement.Impulses) player)
                .travail$externalImpulse(player.getDeltaMovement().subtract(before));
    }
}

package com.thelongtravail.mixin.movement;

import com.thelongtravail.runtime.effects.RevivalMovement;

import net.minecraft.world.entity.monster.Ghast;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(targets = "net.minecraft.world.entity.monster.Ghast$GhastMoveControl")
public abstract class RevivalGhastMixin {
    @Shadow @Final private Ghast ghast;

    @ModifyArg(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"),
            index = 0)
    private double travail$propulsion(double speed) {
        return speed * RevivalMovement.factor(ghast);
    }
}

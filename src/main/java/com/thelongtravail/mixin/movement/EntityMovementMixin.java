package com.thelongtravail.mixin.movement;

import com.thelongtravail.runtime.attributes.GroundSpeed;
import com.thelongtravail.runtime.effects.StiffState;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class EntityMovementMixin {
    @ModifyVariable(method = "moveRelative", at = @At("HEAD"), argsOnly = true)
    private float travail$limitPropulsion(float speed) {
        return (Object) this instanceof LivingEntity living
                ? speed * GroundSpeed.limit(living)
                : speed;
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 travail$stiffMovement(Vec3 movement) {
        if ((Object) this instanceof Player player && StiffState.active(player)) {
            player.setDeltaMovement(Vec3.ZERO);
            return Vec3.ZERO;
        }
        return movement;
    }
}

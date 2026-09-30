package com.thelongtravail.mixin;

import com.thelongtravail.data.AltitudePenalty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

/** Scale elytra displacement on the controlling side, without compounding physics velocity. */
@Mixin(LivingEntity.class)
public abstract class AltitudeGlidingMixin {
    @Unique private float travail$glidingFactor = 1.0F;

    // In 1.20.1 travel's three direct move calls are water, lava, then elytra.
    // Land movement uses handleRelativeFrictionAndCalculateMovement instead.
    @ModifyArg(method = "travel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            ordinal = 2), index = 1, require = 1, allow = 1)
    private Vec3 travail$scaleGlidingDisplacement(Vec3 displacement) {
        travail$glidingFactor = (Object) this instanceof Player player ? AltitudePenalty.factor(player) : 1.0F;
        // Leave deltaMovement intact: gravity, steering and rockets keep their vanilla recurrence.
        // Entity.move still resolves collisions and clears blocked velocity components normally.
        return travail$glidingFactor < 1.0F ? displacement.scale(travail$glidingFactor) : displacement;
    }

    // Vanilla wall damage is (horizontal speed lost * 10 - 3). Scale the speed loss,
    // preserving its damage threshold and sound check, using the factor before movement.
    @ModifyConstant(method = "travel", constant = @Constant(doubleValue = 10.0D),
            slice = @Slice(from = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 2),
                    to = @At(value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/LivingEntity;getFallDamageSound(I)Lnet/minecraft/sounds/SoundEvent;")),
            require = 1, allow = 1)
    private double travail$scaleGlidingImpact(double multiplier) {
        return multiplier * travail$glidingFactor;
    }
}

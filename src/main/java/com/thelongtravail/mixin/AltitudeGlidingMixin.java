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

// 在控制端缩放鞘翅位移，避免物理速度累积衰减。
@Mixin(LivingEntity.class)
public abstract class AltitudeGlidingMixin {
    @Unique private float travail$glidingFactor = 1.0F;

    // 1.20.1 的 travel 中三次直接 move 调用依次对应水、熔岩和鞘翅。
    // 陆地移动通过 handleRelativeFrictionAndCalculateMovement 处理。
    @ModifyArg(method = "travel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            ordinal = 2), index = 1, require = 1, allow = 1)
    private Vec3 travail$scaleGlidingDisplacement(Vec3 displacement) {
        travail$glidingFactor = (Object) this instanceof Player player ? AltitudePenalty.factor(player) : 1.0F;
        // 保留 deltaMovement，使重力、转向和烟花加速继续按原版递推。
        // Entity.move 仍正常处理碰撞并清除受阻方向的速度分量。
        return travail$glidingFactor < 1.0F ? displacement.scale(travail$glidingFactor) : displacement;
    }

    // 原版撞墙伤害为「水平速度损失 × 10 − 3」。使用移动前的系数缩放速度损失，
    // 保留伤害阈值及音效判断。
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

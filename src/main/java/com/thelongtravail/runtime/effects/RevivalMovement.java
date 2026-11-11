package com.thelongtravail.runtime.effects;

import com.thelongtravail.combat.EffectCombatRules;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

// 缩放实体自主推进量，保留重力、跳跃、载具运动和外部推力。
public final class RevivalMovement {
    public interface Impulses {
        void travail$externalImpulse(Vec3 change);

        Vec3 travail$glidingMovement(Vec3 movement);
    }

    public static float factor(Entity entity) {
        return entity instanceof LivingEntity living && EffectCombatRules.revival(living)
                ? .5F
                : 1F;
    }

    public static Vec3 glide(LivingEntity entity, Vec3 movement) {
        return ((Impulses) entity).travail$glidingMovement(movement);
    }

    public static Vec3 glideImpulse(LivingEntity entity, Vec3 start, Vec3 impulse) {
        if (impulse.lengthSqr() == 0) return Vec3.ZERO;
        Vec3 look = entity.getLookAngle();
        float pitch = entity.getXRot() * ((float) Math.PI / 180);
        double gravity =
                entity.getAttributeValue(net.minecraftforge.common.ForgeMod.ENTITY_GRAVITY.get());
        return glideStep(start, look, pitch, gravity)
                .subtract(glideStep(start.subtract(impulse), look, pitch, gravity));
    }

    // 按 Forge 1.20.1 LivingEntity.travel 的鞘翅运动公式分离已记录的外部推力。
    // 此处仅计算推力贡献，实际运动仍由原版 travel 执行。
    private static Vec3 glideStep(Vec3 start, Vec3 look, float pitch, double gravity) {
        double horizontalLook = look.horizontalDistance(), speed = start.horizontalDistance();
        double lift = Math.cos(pitch);
        lift = lift * lift * Math.min(1, look.length() / .4);
        Vec3 motion = start.add(0, gravity * (-1 + lift * .75), 0);
        if (motion.y < 0 && horizontalLook > 0) {
            double gain = motion.y * -.1 * lift;
            motion =
                    motion.add(
                            look.x * gain / horizontalLook, gain, look.z * gain / horizontalLook);
        }
        if (pitch < 0 && horizontalLook > 0) {
            double gain = speed * -net.minecraft.util.Mth.sin(pitch) * .04;
            motion =
                    motion.add(
                            -look.x * gain / horizontalLook,
                            gain * 3.2,
                            -look.z * gain / horizontalLook);
        }
        if (horizontalLook > 0)
            motion =
                    motion.add(
                            (look.x / horizontalLook * speed - motion.x) * .1,
                            0,
                            (look.z / horizontalLook * speed - motion.z) * .1);
        return motion.multiply((double) .99F, (double) .98F, (double) .99F);
    }

    private RevivalMovement() {}
}

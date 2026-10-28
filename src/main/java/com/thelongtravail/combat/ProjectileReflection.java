package com.thelongtravail.combat;

import com.thelongtravail.farreach.SunChaser;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "the_long_travail")
public final class ProjectileReflection {
    public static boolean reflect(Projectile projectile, LivingEntity actor) {
        var velocity = projectile.getDeltaMovement();
        if (projectile.getOwner() == actor
                || !Double.isFinite(velocity.lengthSqr())
                || velocity.lengthSqr() < 1.0E-12) return false;
        projectile.setOwner(actor);
        ((com.thelongtravail.mixin.ReflectedProjectileAccessor) projectile)
                .travail$leftOwner(false);
        var reflected = velocity.scale(-1);
        projectile.setDeltaMovement(reflected);
        if (projectile instanceof net.minecraft.world.entity.projectile.ShulkerBullet) {
            var homing = (com.thelongtravail.mixin.ReflectedShulkerAccessor) projectile;
            homing.travail$target(null);
            homing.travail$targetId(null);
        }
        projectile.setYRot((float) (Math.atan2(reflected.x, reflected.z) * 180 / Math.PI));
        projectile.setXRot(
                (float) (Math.atan2(reflected.y, reflected.horizontalDistance()) * 180 / Math.PI));
        if (projectile instanceof AbstractHurtingProjectile fireball) {
            fireball.xPower = -fireball.xPower;
            fireball.yPower = -fireball.yPower;
            fireball.zPower = -fireball.zPower;
        }
        projectile.hasImpulse = true;
        projectile.hurtMarked = true;
        return true;
    }

    @SubscribeEvent
    public static void impact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity actor
                && SunChaser.reflects(actor)
                && reflect(event.getProjectile(), actor))
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
    }

    private ProjectileReflection() {}
}

package com.thelongtravail.boundless;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.compat.ftbteams.FtbTeamsCompat;
import com.thelongtravail.equipment.TravailCurios;
import com.thelongtravail.journey.LongTravailData;
import com.thelongtravail.mixin.boundless.LorentzArrowAccessor;
import com.thelongtravail.mixin.timestop.TimeStopProjectileAccessor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "the_long_travail")
public final class ProjectileWard {
    private static final boolean IRON_SPELLS =
            net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks");
    private static final double RADIUS = 3;

    public static void scan(ServerPlayer wearer) {
        for (Projectile shot :
                wearer.level()
                        .getEntitiesOfClass(
                                Projectile.class,
                                wearer.getBoundingBox().inflate(RADIUS),
                                p -> p.distanceToSqr(wearer) <= RADIUS * RADIUS))
            intercept(wearer, shot);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void impact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof ServerPlayer wearer)) return;
        var diary = TravailCurios.stack(wearer);
        if (!diary.isEmpty()
                && LongTravailData.hasWitness(diary, TravailAspect.BOUNDLESS)
                && intercept(wearer, event.getProjectile()))
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);
    }

    private static boolean intercept(ServerPlayer wearer, Projectile shot) {
        if (shot.isRemoved()
                || shot instanceof AbstractArrow arrow
                        && ((LorentzArrowAccessor) arrow).travail$inGround()) return false;
        if (friendly(wearer, shot)) return false;
        shot.discard();
        return true;
    }

    private static boolean friendlyId(ServerPlayer wearer, UUID id) {
        if (id != null) {
            if (wearer.getUUID().equals(id)) return true;
            ServerPlayer other = wearer.server.getPlayerList().getPlayer(id);
            return (other != null && wearer.isAlliedTo(other))
                    || FtbTeamsCompat.projectileAlly(wearer, id);
        }
        return false;
    }

    private static boolean friendly(ServerPlayer wearer, Entity entity) {
        for (int depth = 0; entity != null && depth < 16; depth++) {
            if (entity instanceof Player player)
                return player == wearer
                        || wearer.isAlliedTo(player)
                        || FtbTeamsCompat.projectileAlly(wearer, player.getUUID());
            if (entity instanceof Projectile shot) {
                entity = shot.getOwner();
                if (entity == null)
                    return friendlyId(
                            wearer, ((TimeStopProjectileAccessor) shot).travail$ownerUUID());
            } else if (entity instanceof OwnableEntity pet) {
                entity = pet.getOwner();
                if (entity == null) return friendlyId(wearer, pet.getOwnerUUID());
            } else {
                if (!IRON_SPELLS) return false;
                entity = com.thelongtravail.compat.ironsspellbooks.SummonOwner.get(entity);
            }
        }
        return false;
    }

    private ProjectileWard() {}
}

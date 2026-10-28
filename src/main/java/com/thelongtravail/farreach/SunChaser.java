package com.thelongtravail.farreach;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.api.CarryingApi;
import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.combat.DamageScope;
import com.thelongtravail.combat.DirectStrikeDamage;
import com.thelongtravail.combat.SunBladeActors;
import com.thelongtravail.compat.maid.MaidAccessories;
import com.thelongtravail.config.SunBladeConfig;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "the_long_travail")
public final class SunChaser {
    private static final String UNTIL = "the_long_travail:sun_suppression_until";

    private static final class Strike extends DamageSource {
        Strike(LivingEntity target, LivingEntity actor) {
            super(DirectStrikeDamage.source(target.level(), actor, actor).typeHolder(), actor);
        }
    }

    public record Hit(boolean witness, boolean burning, double maximum, double bonus) {}

    public static boolean carrying(LivingEntity entity) {
        return entity instanceof ServerPlayer actor
                ? CarryingApi.isCarrying(actor, ModRegistry.SUN_CHASER.get())
                : MaidAccessories.equipped(entity, ModRegistry.SUN_CHASER.get());
    }

    public static boolean strike(DamageSource source) {
        return com.thelongtravail.combat.DamageIdentity.original(source) instanceof Strike;
    }

    public static boolean immune(LivingEntity target, DamageSource source) {
        return (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.LAVA))
                && carrying(target);
    }

    public static boolean lavaExempt(ServerPlayer actor) {
        return actor.isInLava() && carrying(actor);
    }

    public static boolean reflects(LivingEntity actor) {
        return carrying(actor)
                && (SunBladeActors.phase(actor, TravailAspect.FAR_REACH)
                        == TravailStateApi.AspectState.MALICE);
    }

    public static Hit snapshot(LivingEntity target, DamageSource source) {
        LivingEntity actor = SunBladeActors.attacker(source);
        if (actor == null || actor == target || !carrying(actor))
            return new Hit(false, false, 0, 0);
        boolean burning = target.isOnFire();
        double bonus =
                (SunBladeActors.phase(actor, TravailAspect.FAR_REACH)
                                == TravailStateApi.AspectState.MALICE)
                        ? (source.is(DamageTypeTags.IS_PROJECTILE)
                                        ? SunBladeConfig.value("sun_chaser.projectileBonus")
                                        : 0)
                                + (burning ? SunBladeConfig.value("sun_chaser.burningBonus") : 0)
                        : 0;
        return new Hit(
                (SunBladeActors.phase(actor, TravailAspect.FAR_REACH)
                        == TravailStateApi.AspectState.WITNESS),
                burning,
                target.getMaxHealth(),
                bonus);
    }

    public static void confirmed(LivingEntity target, DamageSource source, Hit hit) {
        if (!hit.witness
                || strike(source)
                || target.isSpectator()
                || target instanceof net.minecraft.world.entity.player.Player p && p.isCreative())
            return;
        int duration = SunBladeConfig.ticks("sun_chaser.suppressionSeconds");
        if (duration > 0)
            target.getPersistentData().putLong(UNTIL, target.level().getGameTime() + duration);
        clearCooldown(target);
    }

    public static void finish(LivingEntity target, DamageSource source, Hit hit) {
        LivingEntity actor = SunBladeActors.attacker(source);
        if (!hit.witness
                || !hit.burning
                || strike(source)
                || !target.isAlive()
                || target.isRemoved()
                || actor == null
                || !actor.isAlive()
                || actor.level() != target.level()
                || !(actor instanceof ServerPlayer)
                        && target instanceof net.minecraft.world.entity.player.Player other
                        && !MaidAccessories.canHarm(actor, other)
                || !carrying(actor)
                || !(SunBladeActors.phase(actor, TravailAspect.FAR_REACH)
                        == TravailStateApi.AspectState.WITNESS)
                || !DamageScope.claimSun(actor.getUUID())) return;
        double amount = hit.maximum * SunBladeConfig.value("sun_chaser.strikeFraction");
        if (amount > 0 && Double.isFinite(amount))
            target.hurt(new Strike(target, actor), (float) Math.min(Float.MAX_VALUE, amount));
    }

    public static boolean suppressed(LivingEntity target) {
        long until = target.getPersistentData().getLong(UNTIL);
        if (until <= target.level().getGameTime() || !target.isAlive()) {
            if (until != 0) target.getPersistentData().remove(UNTIL);
            return false;
        }
        return true;
    }

    public static void clearCooldown(LivingEntity target) {
        if (suppressed(target)) target.invulnerableTime = 0;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void heal(LivingHealEvent event) {
        if (!event.getEntity().level().isClientSide && suppressed(event.getEntity()))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer actor)
                || !carrying(actor)) return;
        maintain(actor);
    }

    public static void maintain(LivingEntity actor) {
        if (actor.level().isClientSide || !carrying(actor)) return;
        actor.clearFire();
        if (actor.isInLava() && actor.tickCount % 20 == 0)
            actor.heal((float) SunBladeConfig.value("sun_chaser.healing"));
    }

    private SunChaser() {}
}

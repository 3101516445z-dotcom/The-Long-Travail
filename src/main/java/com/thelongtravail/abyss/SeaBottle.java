package com.thelongtravail.abyss;

import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.combat.DamageClassTags;
import com.thelongtravail.combat.EffectCombatRules;
import com.thelongtravail.config.ThemeItemConfig;
import com.thelongtravail.equipment.AccessoryEquipRules;
import com.thelongtravail.equipment.TravailCurios;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.runtime.effects.EffectInsertion;
import com.thelongtravail.runtime.effects.InsertedEffectSync;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "the_long_travail")
public final class SeaBottle {
    public static final UUID SLOTS = UUID.fromString("276c6f49-e875-497c-9b86-96acfa9e04dd");
    private static final ThreadLocal<Boolean> APPLYING = ThreadLocal.withInitial(() -> false);

    public static boolean equipped(LivingEntity entity) {
        return entity instanceof Player p
                ? AccessoryEquipRules.hasEquipped(p, ModRegistry.SEA_IN_A_BOTTLE)
                : com.thelongtravail.integration.MaidIntegration.equipped(
                        entity, ModRegistry.SEA_IN_A_BOTTLE);
    }

    private static boolean phase(LivingEntity entity, boolean witness) {
        var state =
                entity instanceof Player p
                        ? TravailStateApi.state(p, com.thelongtravail.TravailAspect.ABYSS)
                        : com.thelongtravail.integration.MaidIntegration.phase(
                                entity, com.thelongtravail.TravailAspect.ABYSS);
        return state
                == (witness
                        ? TravailStateApi.AspectState.WITNESS
                        : TravailStateApi.AspectState.MALICE);
    }

    public static double bonus(DamageSource source) {
        if (!source.is(DamageClassTags.MAGIC)) return 0;
        var actor = EffectCombatRules.actor(source);
        return actor != null && equipped(actor) && phase(actor, true)
                ? ThemeItemConfig.get("sea_in_a_bottle.magicBonus")
                : 0;
    }

    public static double healingBonus(LivingEntity entity) {
        return equipped(entity) && phase(entity, false)
                ? ThemeItemConfig.get("sea_in_a_bottle.healingBonus")
                : 0;
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer p)
            TravailCurios.syncExtraSlots(
                    p,
                    equipped(p) ? (int) ThemeItemConfig.get("sea_in_a_bottle.extraSlots") : 0,
                    SLOTS);
    }

    public static boolean listens(LivingEntity recipient, MobEffectInstance incoming) {
        return !APPLYING.get()
                && incoming.getEffect().getCategory() == MobEffectCategory.HARMFUL
                && !incoming.getEffect().isInstantenous()
                && !recipient.level().isClientSide
                && equipped(recipient)
                && phase(recipient, false);
    }

    public static void reapply(
            LivingEntity recipient,
            MobEffectInstance incoming,
            Entity source,
            int oldLevel,
            boolean accepted) {
        if (!accepted
                || APPLYING.get()
                || recipient.level().isClientSide
                || !equipped(recipient)
                || !phase(recipient, false)
                || incoming.getEffect().getCategory() != MobEffectCategory.HARMFUL
                || incoming.getEffect().isInstantenous()
                || incoming.getAmplifier() <= oldLevel) return;
        if (source instanceof Projectile projectile) source = projectile.getOwner();
        if (!(source instanceof LivingEntity actor) || actor == recipient || !actor.isAlive())
            return;
        var actual = recipient.getEffect(incoming.getEffect());
        if (actual == null || actual.getAmplifier() != incoming.getAmplifier()) return;
        APPLYING.set(true);
        try {
            var copy =
                    new MobEffectInstance(
                            incoming.getEffect(),
                            incoming.getDuration(),
                            incoming.getAmplifier(),
                            incoming.isAmbient(),
                            incoming.isVisible(),
                            incoming.showIcon());
            EffectInsertion.addOrGet(
                    actor,
                    copy,
                    recipient,
                    inserted -> InsertedEffectSync.synchronize(actor, inserted));
        } finally {
            APPLYING.remove();
        }
    }

    private SeaBottle() {}
}

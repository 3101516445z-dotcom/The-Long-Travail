package com.thelongtravail.abyss;

import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.compat.maid.MaidAccessories;
import com.thelongtravail.config.AbyssItemsConfig;
import com.thelongtravail.config.ModeEffectRules;
import com.thelongtravail.data.EffectChanges;
import com.thelongtravail.farreach.IcarusEnvironment;
import com.thelongtravail.helper.AccessoryEquipRules;
import com.thelongtravail.mixin.EffectDurationAccessor;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "the_long_travail")
public final class MidnightLafite {
    public static boolean equipped(LivingEntity e) {
        if (!e.isAlive() || e.isSpectator()) return false;
        return e instanceof Player p
                ? AccessoryEquipRules.hasEquipped(p, "hands", ModRegistry.MIDNIGHT_LAFITE.get())
                : MaidAccessories.lafite(e);
    }

    public static int phase(LivingEntity e) {
        if (e instanceof Player p)
            return TravailStateApi.hasAbyssWitness(p)
                    ? 2
                    : TravailStateApi.hasAbyssMalice(p) ? 1 : 0;
        return MaidAccessories.lafitePhase(e);
    }

    public static double night(long time) {
        long t = Math.floorMod(time, 24000L);
        return t < 12000 ? 0 : Math.max(0, Math.sin(Math.PI * (t - 12000) / 12000.0));
    }

    public static double bonus(LivingEntity e) {
        if (!equipped(e)) return 0;
        long time = Math.floorMod(e.level().getDayTime(), 24000L);
        if (time < 12000) return 0;
        int phase = phase(e);
        if (phase == 2) return AbyssItemsConfig.get("witnessBonus");
        if (phase != 1) return 0;
        return night(time)
                * AbyssItemsConfig.get(
                        !IcarusEnvironment.sunny(e.level()) && !IcarusEnvironment.openSky(e)
                                ? "shelteredNonClearPeak"
                                : "malicePeak");
    }

    public static int levels(LivingEntity e, MobEffect effect) {
        if (!equipped(e)) return 0;
        return effect == MobEffects.REGENERATION
                ? AbyssItemsConfig.REGENERATION.get()
                : effect == MobEffects.SATURATION ? AbyssItemsConfig.SATURATION.get() : 0;
    }

    public static MobEffectInstance duration(LivingEntity e, MobEffectInstance incoming) {
        if (e.level().isClientSide || !equipped(e) || incoming.isInfiniteDuration())
            return incoming;
        double multiplier;
        if (incoming.getEffect() == MobEffects.POISON)
            multiplier = AbyssItemsConfig.get("poisonDurationMultiplier");
        else if (incoming.getEffect() == MobEffects.CONFUSION) {
            multiplier = AbyssItemsConfig.get("nauseaDurationMultiplier");
            if (multiplier > 1 && !ModeEffectRules.LAFITE_NAUSEA.applies(e)) return incoming;
        } else return incoming;
        if (multiplier == 1 || incoming.getDuration() <= 0) return incoming;
        var copy = copyEffect(incoming);
        ((EffectDurationAccessor) copy)
                .travail$duration(
                        (int)
                                Math.max(
                                        1,
                                        Math.min(
                                                Integer.MAX_VALUE,
                                                Math.ceil(incoming.getDuration() * multiplier))));
        return copy;
    }

    public static MobEffectInstance copyEffect(MobEffectInstance incoming) {
        var copy = new MobEffectInstance(incoming);
        var hidden = ((EffectDurationAccessor) incoming).travail$hiddenEffect();
        if (hidden != null)
            ((EffectDurationAccessor) copy).travail$hiddenEffect(copyEffect(hidden));
        return copy;
    }

    public static boolean immune(LivingEntity e, MobEffect effect) {
        return (effect == MobEffects.HUNGER || effect == MobEffects.LEVITATION)
                && equipped(e)
                && phase(e) == 2;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void applicable(MobEffectEvent.Applicable event) {
        if (!event.getEntity().level().isClientSide
                && immune(event.getEntity(), event.getEffectInstance().getEffect()))
            event.setResult(Event.Result.DENY);
    }

    public static void clear(LivingEntity e) {
        if (!equipped(e) || phase(e) != 2) return;
        for (var effect : new MobEffect[] {MobEffects.HUNGER, MobEffects.LEVITATION}) {
            if (!e.hasEffect(effect)) continue;
            if (e instanceof ServerPlayer p) EffectChanges.remove(p, effect, false);
            else e.removeEffect(effect);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer p) clear(p);
    }

    private MidnightLafite() {}
}

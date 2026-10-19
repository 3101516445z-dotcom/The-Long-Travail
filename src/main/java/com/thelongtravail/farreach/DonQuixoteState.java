package com.thelongtravail.farreach;

import com.thelongtravail.compat.maid.MaidAccessories;
import com.thelongtravail.config.FarReachRelicsConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = "the_long_travail")
public final class DonQuixoteState {
    public static final String COOLDOWN = "TravailDonQuixoteCooldown";

    private record Guard(
            UUID target,
            long until,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
            UUID owner) {}

    private static final Map<LivingEntity, Guard> GUARDS = new WeakHashMap<>();

    public static void trigger(LivingEntity p, LivingEntity target) {
        if (DonQuixoteCombat.phase(p) != 2 || cooldown(p) > 0) return;
        cooldown(p, FarReachRelicsConfig.ticks("don_quixote.cooldown"));
        GUARDS.put(
                p,
                new Guard(
                        target.getUUID(),
                        p.level().getGameTime() + FarReachRelicsConfig.ticks("don_quixote.duration"),
                        p.level().dimension(),
                        MaidAccessories.quixoteOwner(p)));
    }

    public static float reduce(LivingEntity target, DamageSource source, float amount) {
        LivingEntity p = target;
        Guard guard = GUARDS.get(p);
        if (guard == null) return amount;
        if (!DonQuixoteCombat.equipped(p)
                || DonQuixoteCombat.phase(p) != 2
                || p.level().getGameTime() >= guard.until
                || p.level().dimension() != guard.dimension
                || !Objects.equals(guard.owner, MaidAccessories.quixoteOwner(p))) {
            GUARDS.remove(p);
            return amount;
        }
        return source.getEntity() != null && source.getEntity().getUUID().equals(guard.target)
                ? (float) (amount * (1 - FarReachRelicsConfig.value("don_quixote.reduction")))
                : amount;
    }

    public static void tick(LivingEntity p) {
        int ticks = cooldown(p);
        if (ticks > 0) cooldown(p, ticks - 1);
        Guard guard = GUARDS.get(p);
        if (!DonQuixoteCombat.equipped(p)
                || DonQuixoteCombat.phase(p) != 2
                || guard != null && !Objects.equals(guard.owner, MaidAccessories.quixoteOwner(p))) GUARDS.remove(p);
    }

    public static void forget(LivingEntity e) {
        GUARDS.remove(e);
    }

    private static int cooldown(LivingEntity e) {
        return e instanceof ServerPlayer ? e.getPersistentData().getInt(COOLDOWN) : MaidAccessories.quixoteCooldown(e);
    }

    private static void cooldown(LivingEntity e, int ticks) {
        if (e instanceof ServerPlayer) e.getPersistentData().putInt(COOLDOWN, ticks);
        else MaidAccessories.quixoteCooldown(e, ticks);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.player instanceof ServerPlayer p) tick(p);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        e.getEntity().getPersistentData().putInt(COOLDOWN, e.getOriginal().getPersistentData().getInt(COOLDOWN));
        if (e.getOriginal() instanceof ServerPlayer p) GUARDS.remove(p);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            GUARDS.remove(p);
            BabelCarrying.forget(p);
        }
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) GUARDS.remove(p);
    }

    private DonQuixoteState() {}
}

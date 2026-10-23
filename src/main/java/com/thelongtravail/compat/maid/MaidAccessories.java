package com.thelongtravail.compat.maid;

import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.MinecraftServer;
import java.util.*;

public final class MaidAccessories {
    public static List<Item> items() {
        return List.of(
                ModRegistry.DEMON_KING.get(),
                ModRegistry.SOUL_BANNER.get(),
                ModRegistry.SPRING_CORNER.get(),
                ModRegistry.EVERLASTING_FLOWER.get(),
                ModRegistry.SWORD_AND_LANTERN.get(),
                ModRegistry.AZRAEL.get(),
                ModRegistry.PHOSPHOPHYLLITE.get(),
                ModRegistry.THOUSAND_YEARS.get(),
                ModRegistry.STAR_VOICE.get(),
                ModRegistry.LORENTZ_FANTASY.get(),
                ModRegistry.ICARUS.get(),
                ModRegistry.DON_QUIXOTE.get(),
                ModRegistry.MIDNIGHT_LAFITE.get(),
                ModRegistry.TOKAIDO.get(),
                ModRegistry.EKI.get());
    }

    public static LivingEntity quixoteAttacker(DamageSource source) {
        return loaded() ? MaidAccessoryRuntime.quixoteAttacker(source) : null;
    }

    public static boolean quixote(LivingEntity e) {
        return loaded() && MaidAccessoryRuntime.quixote(e);
    }

    public static int quixotePhase(LivingEntity e) {
        return loaded() ? MaidAccessoryRuntime.quixotePhase(e) : 0;
    }

    public static UUID quixoteOwner(LivingEntity e) {
        return loaded() ? MaidAccessoryRuntime.quixoteOwner(e) : null;
    }

    public static int quixoteCooldown(LivingEntity e) {
        return loaded() ? MaidAccessoryRuntime.quixoteCooldown(e, -1) : 0;
    }

    public static void quixoteCooldown(LivingEntity e, int ticks) {
        if (loaded()) MaidAccessoryRuntime.quixoteCooldown(e, ticks);
    }

    public static int phosphophyllitePhase(LivingEntity entity) {
        return loaded() ? MaidAccessoryRuntime.phosphophyllitePhase(entity) : 0;
    }

    public static ItemStack lorentzStack(LivingEntity e) {
        return loaded() ? MaidAccessoryRuntime.lorentzStack(e) : ItemStack.EMPTY;
    }

    public static int lorentzPhase(LivingEntity e) {
        return loaded() ? MaidAccessoryRuntime.lorentzPhase(e) : 0;
    }

    public static UUID lorentzOwner(LivingEntity e) {
        return loaded() ? MaidAccessoryRuntime.lorentzOwner(e) : null;
    }

    public static boolean lafite(LivingEntity entity) {
        return loaded() && MaidAccessoryRuntime.lafite(entity);
    }

    public static int lafitePhase(LivingEntity entity) {
        return loaded() ? MaidAccessoryRuntime.lafitePhase(entity) : 0;
    }

    public static ItemStack flowerStack(LivingEntity entity) {
        return loaded() ? MaidAccessoryRuntime.flowerStack(entity) : ItemStack.EMPTY;
    }

    public static int flowerPhase(LivingEntity entity) {
        return loaded() ? MaidAccessoryRuntime.flowerPhase(entity) : 0;
    }

    public static int relicPhase(LivingEntity e) {
        return loaded() ? MaidAccessoryRuntime.relicPhase(e) : 0;
    }

    public static ItemStack relicStack(LivingEntity e, boolean banner) {
        return loaded() ? MaidAccessoryRuntime.relicStack(e, banner) : ItemStack.EMPTY;
    }

    private static boolean loaded() {
        return net.minecraftforge.fml.ModList.get().isLoaded("touhou_little_maid");
    }

    public static boolean isMaid(Entity e) {
        return loaded() && MaidAccessoryRuntime.isMaid(e);
    }

    public static ItemStack springCrown(LivingEntity entity) {
        return loaded() ? MaidAccessoryRuntime.springCrown(entity) : ItemStack.EMPTY;
    }

    public static void init() {
        if (loaded()) MaidAccessoryRuntime.init();
    }

    public static double bonus(LivingEntity victim, DamageSource source) {
        return loaded() ? MaidAccessoryRuntime.bonus(victim, source) : 0;
    }

    public static double witherBonus(LivingEntity victim, DamageSource source) {
        return loaded() ? MaidAccessoryRuntime.witherBonus(victim, source) : 0;
    }

    public static boolean immune(LivingEntity victim, DamageSource source, float amount) {
        return loaded() && MaidAccessoryRuntime.immune(victim, source, amount);
    }

    public static float reduce(
            LivingEntity victim, DamageSource source, float amount, boolean poison) {
        return loaded() ? MaidAccessoryRuntime.reduce(victim, source, amount, poison) : amount;
    }

    public static void received(LivingEntity victim, DamageSource source) {
        if (loaded()) MaidAccessoryRuntime.received(victim, source);
    }

    public static void retaliate(LivingEntity victim, DamageSource source) {
        if (loaded()) MaidAccessoryRuntime.retaliate(victim, source);
    }

    public static void confirmed(LivingEntity victim, DamageSource source, boolean rose) {
        if (loaded()) MaidAccessoryRuntime.confirmed(victim, source, rose);
    }

    public static void successful(LivingEntity victim, DamageSource source) {
        if (loaded()) MaidAccessoryRuntime.successful(victim, source);
    }

    public static boolean seesUndead(LivingEntity e) {
        return loaded() && MaidAccessoryRuntime.seesUndead(e);
    }

    public static MobEffectInstance extend(
            LivingEntity target, MobEffectInstance effect, Entity source) {
        return loaded() ? MaidAccessoryRuntime.extend(target, effect, source) : effect;
    }

    public static UUID casterId(LivingEntity e) {
        return isMaid(e) ? MaidAccessoryRuntime.identity(e) : e.getUUID();
    }

    public static void starCooldown(LivingEntity e, int ticks) {
        if (isMaid(e)) MaidAccessoryRuntime.starCooldown(e, ticks);
    }

    public static boolean completeCooldown(MinecraftServer server, UUID id, int ticks) {
        return loaded() && MaidAccessoryLedger.complete(server, id, ticks);
    }

    public static boolean lantern(Entity e) {
        return loaded() && MaidAccessoryRuntime.lantern(e);
    }

    public static boolean canHarm(
            LivingEntity attacker, net.minecraft.world.entity.player.Player other) {
        if (attacker instanceof net.minecraft.server.level.ServerPlayer p)
            return p.server.isPvpAllowed() && p.canHarmPlayer(other);
        return loaded() && MaidAccessoryRuntime.canHarm(attacker, other);
    }

    private MaidAccessories() {}
}

package com.thelongtravail.compat.maid;

import com.thelongtravail.api.TravailStateApi.AspectState;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;

import java.util.*;

public final class MaidAccessories {
    public static List<Item> items() {
        return List.of(
                ModRegistry.SUN_CHASER.get(),
                ModRegistry.KAGOTSURUBE.get(),
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

    public static boolean equipped(LivingEntity entity, Item item) {
        return loaded() && MaidAccessoryRules.hasEquipped(entity, item);
    }

    public static AspectState phase(LivingEntity entity, com.thelongtravail.TravailAspect aspect) {
        return loaded() ? MaidAccessoryRules.aspectState(entity, aspect) : AspectState.INACTIVE;
    }

    public static LivingEntity damageActor(DamageSource source) {
        return loaded() ? MaidCombatCalculations.damageActor(source) : null;
    }

    public static LivingEntity quixoteAttacker(DamageSource source) {
        return loaded() ? MaidAccessoryRuntime.quixoteAttacker(source) : null;
    }

    public static boolean quixote(LivingEntity e) {
        return loaded() && MaidAccessoryRules.quixote(e);
    }

    public static int quixotePhase(LivingEntity e) {
        return loaded() ? MaidAccessoryRules.quixotePhase(e) : 0;
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
        return loaded() ? MaidAccessoryRules.phosphophyllitePhase(entity) : 0;
    }

    public static ItemStack lorentzStack(LivingEntity e) {
        return loaded() ? MaidAccessoryRules.lorentzStack(e) : ItemStack.EMPTY;
    }

    public static int lorentzPhase(LivingEntity e) {
        return loaded() ? MaidAccessoryRules.lorentzPhase(e) : 0;
    }

    public static UUID lorentzOwner(LivingEntity e) {
        return loaded() ? MaidAccessoryRuntime.lorentzOwner(e) : null;
    }

    public static boolean lafite(LivingEntity entity) {
        return loaded() && MaidAccessoryRules.lafite(entity);
    }

    public static int lafitePhase(LivingEntity entity) {
        return loaded() ? MaidAccessoryRules.lafitePhase(entity) : 0;
    }

    public static ItemStack flowerStack(LivingEntity entity) {
        return loaded() ? MaidAccessoryRules.flowerStack(entity) : ItemStack.EMPTY;
    }

    public static int flowerPhase(LivingEntity entity) {
        return loaded() ? MaidAccessoryRules.flowerPhase(entity) : 0;
    }

    public static int relicPhase(LivingEntity e) {
        return loaded() ? MaidAccessoryRules.relicPhase(e) : 0;
    }

    public static ItemStack relicStack(LivingEntity e, boolean banner) {
        return loaded() ? MaidAccessoryRules.relicStack(e, banner) : ItemStack.EMPTY;
    }

    private static boolean loaded() {
        return net.minecraftforge.fml.ModList.get().isLoaded("touhou_little_maid");
    }

    public static boolean isMaid(Entity e) {
        return loaded() && MaidAccessoryRuntime.isMaid(e);
    }

    public static ItemStack springCrown(LivingEntity entity) {
        return loaded() ? MaidAccessoryRules.springCrown(entity) : ItemStack.EMPTY;
    }

    public static void init() {
        if (loaded()) MaidAccessoryRuntime.init();
    }

    public static double bonus(LivingEntity victim, DamageSource source) {
        return loaded() ? MaidCombatCalculations.bonus(victim, source) : 0;
    }

    public static double witherBonus(LivingEntity victim, DamageSource source) {
        return loaded() ? MaidCombatCalculations.witherBonus(victim, source) : 0;
    }

    public static boolean immune(LivingEntity victim, DamageSource source, float amount) {
        return loaded() && MaidCombatCalculations.immune(victim, source, amount);
    }

    public static float reduce(
            LivingEntity victim, DamageSource source, float amount, boolean poison) {
        return loaded() ? MaidCombatCalculations.reduce(victim, source, amount, poison) : amount;
    }

    public static void received(LivingEntity victim, DamageSource source) {
        if (loaded()) MaidCombatFeedback.received(victim, source);
    }

    public static void retaliate(LivingEntity victim, DamageSource source) {
        if (loaded()) MaidCombatFeedback.retaliate(victim, source);
    }

    public static void confirmed(LivingEntity victim, DamageSource source, boolean rose) {
        if (loaded()) MaidCombatFeedback.confirmed(victim, source, rose);
    }

    public static void successful(LivingEntity victim, DamageSource source) {
        if (loaded()) MaidCombatFeedback.successful(victim, source);
    }

    public static boolean seesUndead(LivingEntity e) {
        return loaded() && MaidAccessoryRules.seesUndead(e);
    }

    public static MobEffectInstance extend(
            LivingEntity target, MobEffectInstance effect, Entity source) {
        return loaded() ? MaidCombatFeedback.extend(target, effect, source) : effect;
    }

    public static UUID casterId(LivingEntity e) {
        return isMaid(e) ? MaidAccess.identity(e) : e.getUUID();
    }

    public static void starCooldown(LivingEntity e, int ticks) {
        if (isMaid(e)) MaidAccessoryRuntime.starCooldown(e, ticks);
    }

    public static boolean completeCooldown(MinecraftServer server, UUID id, int ticks) {
        return loaded() && MaidAccessoryLedger.complete(server, id, ticks);
    }

    public static boolean lantern(Entity e) {
        return loaded() && MaidAccessoryRules.lantern(e);
    }

    public static boolean canHarm(
            LivingEntity attacker, net.minecraft.world.entity.player.Player other) {
        if (attacker instanceof net.minecraft.server.level.ServerPlayer p)
            return p.server.isPvpAllowed() && p.canHarmPlayer(other);
        return loaded() && MaidAccessoryRuntime.canHarm(attacker, other);
    }

    private MaidAccessories() {}
}

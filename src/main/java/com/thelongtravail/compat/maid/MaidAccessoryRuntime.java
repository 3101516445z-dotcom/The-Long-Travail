package com.thelongtravail.compat.maid;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidAndItemTransformEvent;
import com.github.tartaricacid.touhoulittlemaid.network.message.SyncBaubleMessage;
import com.thelongtravail.TravailAspect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.event.CurioEquipEvent;
import java.util.List;
import java.util.UUID;

/** Forge event adapter and compatibility entry points. Mutable state belongs to MaidRuntimeState. */
public final class MaidAccessoryRuntime {
    public enum Phase {
        INACTIVE,
        MALICE,
        WITNESS
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new MaidAccessoryRuntime());
        MinecraftForge.EVENT_BUS.register(new MaidBaubleSync());
    }

    public static List<Item> items() {
        return MaidAccessories.items();
    }

    public static boolean isMaid(Entity e) {
        return e instanceof EntityMaid;
    }

    public static void watch(EntityMaid m) {
        MaidRuntimeState.watch(m);
    }

    public static void equipmentChanged(EntityMaid m) {
        MaidRuntimeState.equipmentChanged(m);
    }

    public static void equipmentChanged(EntityMaid m, ItemStack changed) {
        MaidRuntimeState.equipmentChanged(m, changed);
    }

    public static ItemStack equipped(EntityMaid m, Item item) {
        return MaidAccess.equipped(m, item);
    }

    public static ItemStack lorentzStack(LivingEntity e) {
        return MaidAccessoryRules.lorentzStack(e);
    }

    public static int lorentzPhase(LivingEntity e) {
        return MaidAccessoryRules.lorentzPhase(e);
    }

    public static UUID lorentzOwner(LivingEntity e) {
        return e instanceof EntityMaid m ? m.getOwnerUUID() : null;
    }

    public static ServerPlayer owner(EntityMaid m) {
        return MaidAccess.owner(m);
    }

    public static LivingEntity quixoteAttacker(DamageSource source) {
        return attacker(source);
    }

    public static boolean quixote(LivingEntity e) {
        return MaidAccessoryRules.quixote(e);
    }

    public static int quixotePhase(LivingEntity e) {
        return MaidAccessoryRules.quixotePhase(e);
    }

    public static UUID quixoteOwner(LivingEntity e) {
        return e instanceof EntityMaid m ? m.getOwnerUUID() : null;
    }

    public static int quixoteCooldown(LivingEntity e, int ticks) {
        return MaidRuntimeState.quixoteCooldown(e, ticks);
    }

    public static ItemStack flowerStack(LivingEntity entity) {
        return MaidAccessoryRules.flowerStack(entity);
    }

    public static int flowerPhase(LivingEntity entity) {
        return MaidAccessoryRules.flowerPhase(entity);
    }

    public static boolean lafite(LivingEntity entity) {
        return MaidAccessoryRules.lafite(entity);
    }

    public static int lafitePhase(LivingEntity entity) {
        return MaidAccessoryRules.lafitePhase(entity);
    }

    public static int phosphophyllitePhase(LivingEntity entity) {
        return MaidAccessoryRules.phosphophyllitePhase(entity);
    }

    public static int relicPhase(LivingEntity e) {
        return MaidAccessoryRules.relicPhase(e);
    }

    public static ItemStack relicStack(LivingEntity e, boolean banner) {
        return MaidAccessoryRules.relicStack(e, banner);
    }

    public static Phase phase(EntityMaid m, TravailAspect aspect) {
        return MaidAccessoryRules.phase(m, aspect);
    }

    public static UUID identity(LivingEntity e) {
        return MaidAccess.identity(e);
    }

    public static void starCooldown(LivingEntity e, int ticks) {
        MaidRuntimeState.starCooldown(e, ticks);
    }

    public static boolean canHarm(LivingEntity attacker, Player other) {
        return MaidAccessoryRules.canHarm(attacker, other);
    }

    public static boolean lantern(Entity e) {
        return MaidAccessoryRules.lantern(e);
    }

    public static EntityMaid attacker(Entity source) {
        return MaidCombatCalculations.attacker(source);
    }

    private static EntityMaid attacker(DamageSource source) {
        return MaidCombatCalculations.attacker(source);
    }

    public static ItemStack springCrown(LivingEntity entity) {
        return MaidAccessoryRules.springCrown(entity);
    }

    public static double swimBonus(EntityMaid m) {
        return MaidAccessoryRules.swimBonus(m);
    }

    public static Phase eki(EntityMaid m) {
        return MaidAccessoryRules.eki(m);
    }

    public static double bonus(LivingEntity victim, DamageSource source) {
        return MaidCombatCalculations.bonus(victim, source);
    }

    public static boolean seesUndead(LivingEntity e) {
        return MaidAccessoryRules.seesUndead(e);
    }

    public static double witherBonus(LivingEntity victim, DamageSource source) {
        return MaidCombatCalculations.witherBonus(victim, source);
    }

    public static boolean immune(LivingEntity victim, DamageSource source, float amount) {
        return MaidCombatCalculations.immune(victim, source, amount);
    }

    public static float reduce(
            LivingEntity victim, DamageSource source, float amount, boolean poison) {
        return MaidCombatCalculations.reduce(victim, source, amount, poison);
    }

    public static void received(LivingEntity victim, DamageSource source) {
        MaidCombatFeedback.received(victim, source);
    }

    public static void confirmed(LivingEntity victim, DamageSource source, boolean rose) {
        MaidCombatFeedback.confirmed(victim, source, rose);
    }

    public static void successful(LivingEntity victim, DamageSource source) {
        MaidCombatFeedback.successful(victim, source);
    }

    public static void retaliate(LivingEntity victim, DamageSource source) {
        MaidCombatFeedback.retaliate(victim, source);
    }

    public static MobEffectInstance extend(
            LivingEntity target, MobEffectInstance effect, Entity source) {
        return MaidCombatFeedback.extend(target, effect, source);
    }

    public static void tick(EntityMaid m) {
        MaidPeriodicEffects.tick(m);
    }

    private static List<SyncBaubleMessage> lanternChanges(
            EntityMaid m, CompoundTag previous, CompoundTag current) {
        return MaidBaubleSync.changes(m, previous, current);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void denyCurios(CurioEquipEvent e) {
        if (e.getEntity() instanceof EntityMaid
                && "the_long_travail"
                        .equals(
                                ForgeRegistries.ITEMS
                                        .getKey(e.getStack().getItem())
                                        .getNamespace())) e.setResult(Event.Result.DENY);
    }

    @SubscribeEvent
    public void join(EntityJoinLevelEvent e) {
        if (e.getEntity() instanceof EntityMaid m) watch(m);
    }

    @SubscribeEvent
    public void leave(EntityLeaveLevelEvent e) {
        MaidRuntimeState.leave(e);
    }

    @SubscribeEvent
    public void transformOut(MaidAndItemTransformEvent.ToItem e) {
        MaidRuntimeState.transformOut(e);
    }

    @SubscribeEvent
    public void transformIn(MaidAndItemTransformEvent.ToMaid e) {
        MaidRuntimeState.transformIn(e);
    }

    @SubscribeEvent
    public void heal(LivingHealEvent e) {
        MaidCombatFeedback.heal(e);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void effect(MobEffectEvent.Applicable e) {
        MaidCombatFeedback.effect(e);
    }

    @SubscribeEvent
    public void death(LivingDeathEvent e) {
        MaidCombatFeedback.death(e);
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent e) {
        MaidRuntimeMaintenance.serverTick(e);
    }

    @SubscribeEvent
    public void stopped(net.minecraftforge.event.server.ServerStoppedEvent e) {
        MaidRuntimeState.stopped(e);
    }
}

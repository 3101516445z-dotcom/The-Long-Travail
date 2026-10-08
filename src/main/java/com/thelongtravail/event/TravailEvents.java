package com.thelongtravail.event;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.*;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.helper.TrueDamage;
import com.thelongtravail.network.TravailNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;


public class TravailEvents {
    @SubscribeEvent
    public void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            RewardDelivery.get(event.getServer()).tick(event.getServer());
            WayguideSearch.tick();
            JourneyVisits.tick();
            WayguideReturns.get(event.getServer()).tick(event.getServer());
        }
    }
    @SubscribeEvent
    public void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent event) {
        RuntimePools.reload();
    }

    @SubscribeEvent
    public void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        JourneyVisits.clear();
        WayguideSearch.clear(); // 在世界和玩家保存前交接托管物品。
    }
    @SubscribeEvent
    public void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        RequirementPools.invalidate();
        JourneyQueries.clear();
        JourneyVisits.clear();
        WayguideSearch.stopped();
        com.thelongtravail.helper.ReceivedMalice.clear();
        AutomaticEffectBudget.clear();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        TravailCommands.onRegisterCommands(event);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // 在背包常规 tick 前校正已保存的修饰符，包括旧版本缓存的修饰符。
        ItemStack equipped = TravailCurios.stack(player);
        if (equipped.isEmpty()) TravailPlayerMaintenance.removeExtraSlots(player); else TravailPlayerMaintenance.updateExtraSlots(player, equipped);
        TravailNetwork.sendTooltipConfig(player);
        TravailNetwork.sendJourney(player);
        TravailPlayerMaintenance.giveStarterItem(player);
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        PlayerJourneyData.copyTo(event.getOriginal(), event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            VisualDeprivation.resend(player);
            StiffState.resend(player);
            WayguideReturns.get(player.server).activate(player);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { resetRuntime(player, VisualDeprivation.ClearReason.RESPAWN); TravailNetwork.sendJourney(player); WayguideReturns.get(player.server).activate(player); }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { resetRuntime(player, VisualDeprivation.ClearReason.DIMENSION_CHANGE); TravailNetwork.sendJourney(player); WayguideReturns.get(player.server).activate(player); }
    }

    @SubscribeEvent
    public void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEntity() instanceof ServerPlayer player) EffectChanges.externalChange(player, event.getEffect());
    }

    // 牛奶同时清除权威状态和用于显示的效果实例。
    @SubscribeEvent
    public void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getItem().is(Items.MILK_BUCKET)) {
            VisualDeprivation.clear(player, VisualDeprivation.ClearReason.MILK);
            StiffState.clear(player);
        }
    }

    @SubscribeEvent
    public void afterPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END && event.player instanceof ServerPlayer player) EffectChanges.tick(player);
    }
    public static void onDiaryReset(ServerPlayer player, ItemStack diary) {
        resetRuntime(player, VisualDeprivation.ClearReason.DIARY_RESET);
        TravailPlayerMaintenance.updateExtraSlots(player, diary);
        TravailPlayerMaintenance.updateAltitudePenalty(player, diary);
        TravailNetwork.sendJourney(player);
    }

    private static void resetRuntime(ServerPlayer player, VisualDeprivation.ClearReason reason) {
        clearRuntimeCaches(player);
        VisualDeprivation.clear(player, reason);
        StiffState.clear(player);
    }

    private static void clearRuntimeCaches(ServerPlayer player) {
        com.thelongtravail.valley.AzraelState.forget(player);
        AltitudePenalty.forget(player);
        com.thelongtravail.helper.ReceivedMalice.forget(player);
        AutomaticEffectBudget.forget(player);
        JourneyQueries.forget(player); CleanseRotation.forget(player); EffectChanges.forget(player);
        WayguideSearch.forget(player);
    }
    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) resetRuntime(player, VisualDeprivation.ClearReason.DEATH);
    }
    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AltitudePenalty.forget(player);
            // 持久化的截止时间属于诅咒状态，不能随连接缓存一起清除。
            clearRuntimeCaches(player);
            WayguideReturns.get(player.server).suspend(player);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        VisualDeprivation.tick(player);
        StiffState.tick(player);
        ItemStack fastTravail = TravailCurios.stack(player);
        if (player.tickCount % 20 == 0) JourneyQueries.update(player, fastTravail);
        if (fastTravail.isEmpty()) {
            EffectChanges.unequipped(player);
            AltitudePenalty.forget(player);
            TravailPlayerMaintenance.restoreFlyingSpeed(player);
            if (player.tickCount % 20 == 0) TravailPlayerMaintenance.removeExtraSlots(player);
            return;
        }

        ItemStack travail = fastTravail;
        LongTravailData.initialize(travail, player);
        TravailPlayerMaintenance.updateAltitudePenalty(player, travail);
        if (player.tickCount % 20 == 0) {
            TravailPlayerMaintenance.updateExtraSlots(player, travail);
        }

        if (!LongTravailData.hasWitness(travail, TravailAspect.ABYSS) && touchesFluid(player)
                && isDue(player, TravailConfig.ABYSS_FLUID_INTERVAL_SECONDS.get(), PeriodicSchedule.FLUID)) {
            TrueDamage.hurtFluid(player, safeFloat(TravailConfig.ABYSS_FLUID_DAMAGE.get()));
        }
        if (!LongTravailData.hasWitness(travail, TravailAspect.UNDERWORLD)) TravailPlayerMaintenance.clampExperience(player);
        if (LongTravailData.hasWitness(travail, TravailAspect.BOUNDLESS)
                && Math.floorMod(player.level().getGameTime() + player.getUUID().hashCode(),
                        Math.max(20, TravailEffectActions.secondsToTicks(TravailConfig.PHANTOM_CHECK_INTERVAL_SECONDS.get()))) == 0) TravailRewards.killNearbyPhantoms(player);
        if (LongTravailData.hasWitness(travail, TravailAspect.DEEP_VALLEY)) {
            if (player.hasEffect(MobEffects.DARKNESS)) EffectChanges.remove(player, MobEffects.DARKNESS, false);
            if (player.hasEffect(MobEffects.BLINDNESS)) EffectChanges.remove(player, MobEffects.BLINDNESS, false);
        }

        if (LongTravailData.hasWitness(travail, TravailAspect.FAR_REACH)
                && isDue(player, TravailConfig.FAR_WITNESS_INTERVAL_SECONDS.get(), PeriodicSchedule.FAR_REACH)) {
            TravailEffectActions.processFarReachWitness(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        TravailPlayerMaintenance.onBreakSpeed(event);
    }

    @SubscribeEvent
    public void onHeal(LivingHealEvent event) {
        TravailEffectActions.onHeal(event);
    }

    @SubscribeEvent
    public void onEffectApplicable(MobEffectEvent.Applicable event) {
        TravailEffectActions.onEffectApplicable(event);
    }

    @SubscribeEvent
    public void onEffectAdded(MobEffectEvent.Added event) {
        TravailEffectActions.onEffectAdded(event);
    }

    @SubscribeEvent
    public void onJump(LivingEvent.LivingJumpEvent event) {
        TravailEffectActions.onJump(event);
    }

    @SubscribeEvent
    public void onProjectileCreated(EntityJoinLevelEvent event) {
        TravailEffectActions.onProjectileCreated(event);
    }

    @SubscribeEvent
    public void onExperienceChange(PlayerXpEvent.XpChange event) {
        TravailPlayerMaintenance.onExperienceChange(event);
    }

    @SubscribeEvent
    public void onExperienceLevelChange(PlayerXpEvent.LevelChange event) {
        TravailPlayerMaintenance.onExperienceLevelChange(event);
    }

    @SubscribeEvent
    public void onFishing(ItemFishedEvent event) {
        TravailRewards.onFishing(event);
    }

    public static void processBeingAttacked(ServerPlayer player) {
        TravailEffectActions.processBeingAttacked(player);
    }

    public static void processSuccessfulAttack(ServerPlayer player) {
        TravailEffectActions.processSuccessfulAttack(player);
    }

    private static boolean touchesFluid(ServerPlayer player) {
        return player.isInFluidType() || player.isInWaterOrBubble() || player.isInLava();
    }

    public static boolean isFlyingAttack(ServerPlayer player, DamageSource source) {
        return TravailEffectActions.isFlyingAttack(player, source);
    }

    private static boolean isDue(ServerPlayer player, double seconds, int task) {
        int interval = TravailEffectActions.secondsToTicks(seconds);
        return PeriodicSchedule.due(player.level().getGameTime(), player.getUUID().hashCode(), task, interval);
    }

    private static float safeFloat(double value) {
        return value >= Float.MAX_VALUE ? Float.MAX_VALUE : (float) Math.max(0.0D, value);
    }

}

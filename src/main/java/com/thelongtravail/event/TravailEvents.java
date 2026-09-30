package com.thelongtravail.event;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.*;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.helper.TrueDamage;
import com.thelongtravail.network.TravailNetwork;
import com.thelongtravail.network.TooltipConfigSync;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class TravailEvents {
    @SubscribeEvent
    public void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END)
            RewardDelivery.get(event.getServer()).tick(event.getServer());
    }
    @SubscribeEvent
    public void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent event) {
        RuntimePools.reload();
    }

    @SubscribeEvent
    public void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        RequirementPools.invalidate();
        JourneyQueries.clear();
        com.thelongtravail.helper.ReceivedMalice.clear();
        AutomaticEffectBudget.clear();
    }
    private static final String FLYING_PROJECTILE = "LongTravailFlyingProjectile";
    private static final String ATTACK_CLEANSE_COOLDOWN = "AttackCleanseCooldown";
    private static final String VALLEY_WITNESS_COOLDOWN = "ValleyWitnessCooldown";
    private static final String STARTER_ITEM_RECEIVED = "LongTravailStarterItemReceived";

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("the_long_travail")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("diagnose_effects").executes(context -> EffectChanges.diagnose(context.getSource().getPlayerOrException())))
                .then(Commands.literal("visual_deprivation")
                        .then(Commands.literal("clear")
                                .executes(context -> clearVisualDeprivation(context.getSource().getPlayerOrException()))))
                .then(Commands.literal("reverse")
                        .then(Commands.literal("clear")
                                .executes(context -> clearWitnessTesting(context.getSource().getPlayerOrException(), 0))
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 6))
                                        .executes(context -> clearWitnessTesting(
                                                context.getSource().getPlayerOrException(),
                                                IntegerArgumentType.getInteger(context, "value")))))
                        .then(Commands.argument("value", IntegerArgumentType.integer(-6, 6))
                                .executes(context -> setWitnessForTesting(
                                        context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "value"))))));
    }

    private static int clearVisualDeprivation(ServerPlayer player) {
        VisualDeprivation.clear(player, VisualDeprivation.ClearReason.COMMAND);
        player.sendSystemMessage(Component.translatable("command.the_long_travail.visual_deprivation.cleared"));
        return 1;
    }

    private static int setWitnessForTesting(ServerPlayer player, int value) {
        if (value == 0) {
            player.sendSystemMessage(Component.translatable("command.the_long_travail.reverse.zero"));
            return 0;
        }
        ItemStack travail = TravailCurios.stack(player);
        if (travail.isEmpty()) {
            player.sendSystemMessage(Component.translatable("command.the_long_travail.reverse.not_equipped"));
            return 0;
        }
        TravailAspect aspect = TravailAspect.values()[Math.abs(value) - 1];
        boolean witness = value > 0;
        LongTravailData.setWitness(travail, aspect, witness);
        String state = witness ? "witness" : "malice";
        String key = "tooltip.the_long_travail." + aspect.id() + "." + state + ".title";
        player.sendSystemMessage(Component.translatable("command.the_long_travail.reverse.success", Component.translatable(key)));
        return 1;
    }

    private static int clearWitnessTesting(ServerPlayer player, int value) {
        ItemStack travail = TravailCurios.stack(player);
        if (travail.isEmpty()) {
            player.sendSystemMessage(Component.translatable("command.the_long_travail.reverse.not_equipped"));
            return 0;
        }
        if (value == 0) {
            LongTravailData.clearForcedWitnessState(travail, null);
            player.sendSystemMessage(Component.translatable("command.the_long_travail.reverse.clear_all"));
            return 1;
        }
        TravailAspect aspect = TravailAspect.values()[value - 1];
        LongTravailData.clearForcedWitnessState(travail, aspect);
        player.sendSystemMessage(Component.translatable("command.the_long_travail.reverse.clear_one", value));
        return 1;
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Reconcile saved (including pre-fix cached) modifiers before normal inventory ticking.
        ItemStack equipped = TravailCurios.stack(player);
        if (equipped.isEmpty()) removeExtraSlots(player); else updateExtraSlots(player, equipped);
        TravailNetwork.sendTooltipConfig(player);
        TravailNetwork.sendJourney(player);
        if (!TravailConfig.GIVE_STARTER_ITEM.get()) return;

        CompoundTag persistentData = player.getPersistentData();
        CompoundTag persisted = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(STARTER_ITEM_RECEIVED)) return;

        ItemStack example = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        boolean alreadyOwnsOne = player.getInventory().contains(example) || !TravailCurios.stack(player).isEmpty();
        if (!alreadyOwnsOne) {
            ItemStack gift = example.copy();
            if (!player.addItem(gift)) player.drop(gift, false);
        }

        persisted.putBoolean(STARTER_ITEM_RECEIVED, true);
        persistentData.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        PlayerJourneyData.copyTo(event.getOriginal(), event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) VisualDeprivation.resend(player);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { resetRuntime(player, VisualDeprivation.ClearReason.RESPAWN); TravailNetwork.sendJourney(player); }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { resetRuntime(player, VisualDeprivation.ClearReason.DIMENSION_CHANGE); TravailNetwork.sendJourney(player); }
    }

    @SubscribeEvent
    public void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEntity() instanceof ServerPlayer player) EffectChanges.externalChange(player, event.getEffect());
    }

    /** Milk is a legitimate way out, so it ends the state as well as the presentation instance. */
    @SubscribeEvent
    public void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getItem().is(Items.MILK_BUCKET)) {
            VisualDeprivation.clear(player, VisualDeprivation.ClearReason.MILK);
        }
    }

    @SubscribeEvent
    public void afterPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END && event.player instanceof ServerPlayer player) EffectChanges.tick(player);
    }
    public static void onDiaryReset(ServerPlayer player, ItemStack diary) {
        resetRuntime(player, VisualDeprivation.ClearReason.DIARY_RESET);
        updateExtraSlots(player, diary);
        updateAltitudePenalty(player, diary);
        TravailNetwork.sendJourney(player);
    }

    private static void resetRuntime(ServerPlayer player, VisualDeprivation.ClearReason reason) {
        clearRuntimeCaches(player);
        VisualDeprivation.clear(player, reason);
    }

    private static void clearRuntimeCaches(ServerPlayer player) {
        AltitudePenalty.forget(player);
        com.thelongtravail.helper.ReceivedMalice.forget(player);
        AutomaticEffectBudget.forget(player);
        JourneyQueries.forget(player); CleanseRotation.forget(player); EffectChanges.forget(player);
    }
    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) resetRuntime(player, VisualDeprivation.ClearReason.DEATH);
    }
    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AltitudePenalty.forget(player);
            // The persisted deadline belongs to the curse, not to this connection's caches.
            clearRuntimeCaches(player);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        VisualDeprivation.tick(player);
        ItemStack fastTravail = TravailCurios.stack(player);
        if (player.tickCount % 20 == 0) JourneyQueries.update(player, fastTravail);
        if (fastTravail.isEmpty()) {
            EffectChanges.unequipped(player);
            AltitudePenalty.forget(player);
            restoreFlyingSpeed(player);
            if (player.tickCount % 20 == 0) removeExtraSlots(player);
            return;
        }

        ItemStack travail = fastTravail;
        LongTravailData.initialize(travail, player);
        updateAltitudePenalty(player, travail);
        if (player.tickCount % 20 == 0) {
            updateExtraSlots(player, travail);
        }

        if (!LongTravailData.hasWitness(travail, TravailAspect.ABYSS) && touchesFluid(player)
                && isDue(player, TravailConfig.ABYSS_FLUID_INTERVAL_SECONDS.get(), PeriodicSchedule.FLUID)) {
            TrueDamage.hurtFluid(player, safeFloat(TravailConfig.ABYSS_FLUID_DAMAGE.get()));
        }
        if (!LongTravailData.hasWitness(travail, TravailAspect.UNDERWORLD)) clampExperience(player);
        if (LongTravailData.hasWitness(travail, TravailAspect.BOUNDLESS)
                && Math.floorMod(player.level().getGameTime() + player.getUUID().hashCode(),
                        Math.max(20, secondsToTicks(TravailConfig.PHANTOM_CHECK_INTERVAL_SECONDS.get()))) == 0) killNearbyPhantoms(player);
        if (LongTravailData.hasWitness(travail, TravailAspect.DEEP_VALLEY)) {
            if (player.hasEffect(MobEffects.DARKNESS)) EffectChanges.remove(player, MobEffects.DARKNESS, false);
            if (player.hasEffect(MobEffects.BLINDNESS)) EffectChanges.remove(player, MobEffects.BLINDNESS, false);
        }

        if (LongTravailData.hasWitness(travail, TravailAspect.FAR_REACH)
                && isDue(player, TravailConfig.FAR_WITNESS_INTERVAL_SECONDS.get(), PeriodicSchedule.FAR_REACH)) {
            processFarReachWitness(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        // Client prediction and server block breaking must use the same penalty.
        Player player = event.getEntity();
        ItemStack travail = TravailCurios.stack(player);
        if (!travail.isEmpty() && !LongTravailData.hasWitness(travail, TravailAspect.ABYSS)) {
            double reduction = TravailConfig.ABYSS_MINING_REDUCTION.get();
            if (player.level().isClientSide)
                reduction = TooltipConfigSync.decimal("abyss.miningReduction", reduction);
            // Apply after ordinary equipment bonuses, preserving other mods' current speed.
            float multiplier = (float) (1.0D - Math.max(0.0D, Math.min(1.0D, reduction)));
            event.setNewSpeed(event.getNewSpeed() * multiplier);
        }
    }

    @SubscribeEvent
    public void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) return;
        ItemStack travail = TravailCurios.stack(player);
        if (travail.isEmpty()) return;
        if (!LongTravailData.hasWitness(travail, TravailAspect.FLOURISHING)) {
            event.setAmount(event.getAmount() * (1.0F - TravailConfig.FLOURISHING_HEALING_REDUCTION.get().floatValue()));
            return;
        }
        double bonus = FlourishingBonus.calculate(PlayerJourneyData.discoveredBiomeCount(player),
                TravailConfig.FLOURISHING_BONUS_PER_BIOME.get(), TravailConfig.MAX_HEALING_BONUS.get());
        event.setAmount(safeMultiply(event.getAmount(), 1.0F + (float) bonus));
    }

    @SubscribeEvent
    public void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MobEffect effect = event.getEffectInstance().getEffect();
        if (effect != MobEffects.DARKNESS && effect != MobEffects.BLINDNESS) return;
        ItemStack travail = TravailCurios.stack(player);
        if (!travail.isEmpty() && LongTravailData.hasWitness(travail, TravailAspect.DEEP_VALLEY)) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MobEffectInstance added = event.getEffectInstance();
        EffectChanges.externalChange(player, added.getEffect());
        if (added.getEffect().getCategory() != MobEffectCategory.BENEFICIAL) return;
        ItemStack travail = TravailCurios.stack(player);
        if (travail.isEmpty() || !LongTravailData.hasWitness(travail, TravailAspect.FAR_REACH)) return;
        ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(added.getEffect());
        if (effectId != null && RuntimePools.current().levelBonusBlacklist().contains(effectId)) return;
        int maxLevel = configuredMaxLevel(added.getEffect());
        // Added fires before the outer addEffect writes/merges this instance.
        // Upgrade that input in place; recursive addEffect would be overwritten on first gain.
        EffectUpgrade.apply(added, TravailConfig.FAR_WITNESS_LEVEL_BONUS.get(), maxLevel);
    }

    @SubscribeEvent
    public void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity().hasEffect(ModRegistry.STIFF.get())) {
            Vec3 motion = event.getEntity().getDeltaMovement();
            if (motion.y > 0.0D) event.getEntity().setDeltaMovement(motion.x, 0.0D, motion.z);
        }
    }

    @SubscribeEvent
    public void onProjectileCreated(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Projectile projectile)
                || !(projectile.getOwner() instanceof ServerPlayer player)) return;
        if (isFlying(player)) projectile.getPersistentData().putBoolean(FLYING_PROJECTILE, true);
    }

    @SubscribeEvent
    public void onExperienceChange(PlayerXpEvent.XpChange event) {
        int cap = TravailConfig.UNDERWORLD_EXPERIENCE_LEVEL_CAP.get();
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0 || player.experienceLevel < cap) return;
        ItemStack travail = TravailCurios.stack(player);
        if (!travail.isEmpty() && !LongTravailData.hasWitness(travail, TravailAspect.UNDERWORLD)) event.setAmount(0);
    }

    @SubscribeEvent
    public void onExperienceLevelChange(PlayerXpEvent.LevelChange event) {
        int cap = TravailConfig.UNDERWORLD_EXPERIENCE_LEVEL_CAP.get();
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getLevels() <= 0 || player.experienceLevel < cap) return;
        ItemStack travail = TravailCurios.stack(player);
        if (!travail.isEmpty() && !LongTravailData.hasWitness(travail, TravailAspect.UNDERWORLD)) event.setLevels(0);
    }

    @SubscribeEvent
    public void onFishing(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack travail = TravailCurios.stack(player);
        if (travail.isEmpty() || !LongTravailData.hasWitness(travail, TravailAspect.UNDERWORLD)) return;
        double entityChance = TravailConfig.FISH_ENTITY_CHANCE.get();
        double itemChance = TravailConfig.FISH_SPECIAL_CHANCE.get();
        double total = entityChance + itemChance;
        if (total > 1.0D) {
            entityChance /= total;
            itemChance /= total;
        }
        double roll = player.getRandom().nextDouble();
        if (roll < entityChance) {
            spawnFishingEntity(player, event);
        } else if (roll < entityChance + itemChance) {
            spawnFishingReward(player, event);
        }
    }

    public static void processBeingAttacked(ServerPlayer player) {
        ItemStack travail = TravailCurios.stack(player);
        if (travail.isEmpty()) return;
        RandomSource random = player.getRandom();
        if (!LongTravailData.hasWitness(travail, TravailAspect.FAR_REACH)) {
            if (random.nextDouble() < TravailConfig.FAR_ALL_CLEAR_CHANCE.get()) removeAllBeneficial(player); else removeOneBeneficial(player);
        }
        if (!LongTravailData.hasWitness(travail, TravailAspect.DEEP_VALLEY)
                && random.nextDouble() < TravailConfig.VALLEY_VISUAL_DEPRIVATION_CHANCE.get()) {
            VisualDeprivation.start(player, secondsToTicks(TravailConfig.VALLEY_VISUAL_DEPRIVATION_DURATION_SECONDS.get()));
        }
    }

    public static void processSuccessfulAttack(ServerPlayer player) {
        ItemStack travail = TravailCurios.stack(player);
        if (travail.isEmpty()) return;
        RandomSource random = player.getRandom();
        if (!LongTravailData.hasWitness(travail, TravailAspect.FAR_REACH)) {
            long now = player.level().getGameTime();
            if (now >= LongTravailData.getLong(travail, ATTACK_CLEANSE_COOLDOWN)
                    && random.nextDouble() < TravailConfig.FAR_ATTACK_CLEAR_CHANCE.get()) {
                removeOneBeneficial(player);
                LongTravailData.putLong(travail, ATTACK_CLEANSE_COOLDOWN, now + secondsToTicks(TravailConfig.FAR_ATTACK_COOLDOWN_SECONDS.get()));
            }
        }
        if (!LongTravailData.hasWitness(travail, TravailAspect.DEEP_VALLEY)) {
            if (random.nextDouble() < TravailConfig.VALLEY_STIFF_CHANCE.get()) {
                player.addEffect(new MobEffectInstance(ModRegistry.STIFF.get(), secondsToTicks(TravailConfig.VALLEY_STIFF_DURATION_SECONDS.get())));
            }
        } else {
            long now = player.level().getGameTime();
            if (now < LongTravailData.getLong(travail, VALLEY_WITNESS_COOLDOWN)) return;
            LongTravailData.putLong(travail, VALLEY_WITNESS_COOLDOWN,
                    now + secondsToTicks(TravailConfig.VALLEY_WITNESS_ATTACK_COOLDOWN_SECONDS.get()));
            if (AutomaticEffectBudget.acquire(player)) EffectChanges.add(player, new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                    secondsToTicks(TravailConfig.VALLEY_SWIFTNESS_DURATION_SECONDS.get()), TravailConfig.VALLEY_SWIFTNESS_LEVEL.get() - 1));
            for (int i = 0; i < TravailConfig.VALLEY_RANDOM_EFFECT_COUNT.get(); i++) {
                grantRandomBeneficial(player, RuntimePools.current().valley(),
                        secondsToTicks(TravailConfig.VALLEY_RANDOM_EFFECT_DURATION_SECONDS.get()));
            }
        }
    }

    private static void processFarReachWitness(ServerPlayer player) {
        int actions = TravailConfig.FAR_WITNESS_ACTION_COUNT.get();
        int removed = CleanseRotation.remove(player, actions, () -> AutomaticEffectBudget.acquire(player));
        for (int i = removed; i < actions; i++) grantRandomBeneficial(player, RuntimePools.current().far(),
                secondsToTicks(TravailConfig.FAR_WITNESS_POSITIVE_DURATION_SECONDS.get()));
    }

    private static void updateAltitudePenalty(ServerPlayer player, ItemStack travail) {
        boolean active = !LongTravailData.hasWitness(travail, TravailAspect.BOUNDLESS)
                && player.getY() > TravailConfig.BOUNDLESS_HEIGHT_THRESHOLD.get();
        double reduction = TravailConfig.BOUNDLESS_SPEED_REDUCTION.get();
        boolean penaltyActive = active && reduction > 0.0D;
        // The walking/swimming penalty is applied by AltitudeSpeedMixin when the speed is read,
        // so nothing is written to the movement speed attribute here.
        if (penaltyActive && player.getAbilities().flying) reduceFlyingSpeed(player, reduction); else restoreFlyingSpeed(player);
        // Elytra displacement is scaled by AltitudeGlidingMixin on the controlling side.
        // Do not damp the stored velocity here: that compounds and misses local prediction.
    }

    private static void reduceFlyingSpeed(ServerPlayer player, double reduction) {
        CompoundTag data = player.getPersistentData();
        float current = player.getAbilities().getFlyingSpeed();
        float baseline = data.getBoolean("LongTravailFlightSlowed")
                ? data.getFloat("LongTravailOriginalFlySpeed") : current;
        // Legacy saves have a baseline but no last-applied value.
        float last = data.contains("LongTravailLastFlySpeed") ? data.getFloat("LongTravailLastFlySpeed") : current;
        FlightSpeedAdjustment.Result adjustment = FlightSpeedAdjustment.apply(current, baseline, last, reduction);
        data.putBoolean("LongTravailFlightSlowed", true);
        data.putFloat("LongTravailOriginalFlySpeed", adjustment.baseline());
        data.putFloat("LongTravailLastFlySpeed", adjustment.applied());
        if (!FlightSpeedAdjustment.same(current, adjustment.applied())) {
            player.getAbilities().setFlyingSpeed(adjustment.applied());
            player.onUpdateAbilities();
        }
    }

    private static void restoreFlyingSpeed(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean("LongTravailFlightSlowed")) return;
        float current = player.getAbilities().getFlyingSpeed();
        float restored = FlightSpeedAdjustment.restore(current, data.getFloat("LongTravailOriginalFlySpeed"),
                data.contains("LongTravailLastFlySpeed") ? data.getFloat("LongTravailLastFlySpeed") : current);
        data.remove("LongTravailOriginalFlySpeed");
        data.remove("LongTravailLastFlySpeed");
        data.remove("LongTravailFlightSlowed");
        if (!FlightSpeedAdjustment.same(current, restored)) {
            player.getAbilities().setFlyingSpeed(restored);
            player.onUpdateAbilities();
        }
    }

    private static void updateExtraSlots(ServerPlayer player, ItemStack travail) {
        TravailCurios.syncExtraSlots(player, LongTravailData.hasWitness(travail, TravailAspect.BOUNDLESS)
                ? TravailConfig.BOUNDLESS_EXTRA_CURIO_SLOTS.get() : 0);
    }

    private static void removeExtraSlots(ServerPlayer player) {
        TravailCurios.syncExtraSlots(player, 0);
    }

    private static void killNearbyPhantoms(ServerPlayer player) {
        int range = TravailConfig.PHANTOM_RANGE.get();
        double rangeSquared = (double) range * range;
        int attempts = 0;
        RewardDelivery queue = RewardDelivery.get(player.server);
        for (Phantom phantom : player.serverLevel().getEntitiesOfClass(Phantom.class,
                new net.minecraft.world.phys.AABB(player.position(), player.position()).inflate(range),
                candidate -> candidate.isAlive() && candidate.distanceToSqr(player) <= rangeSquared)) {
            if (!phantom.isAlive()) continue;
            if (attempts++ >= TravailConfig.PHANTOM_MAX_KILLS.get()) break;
            try (var reservation = queue.reserve()) {
                if (reservation == null) { queue.warnFull(player.server); break; }
                phantom.hurt(player.damageSources().playerAttack(player), Float.MAX_VALUE);
                if (!phantom.isAlive()) queueReward(reservation, player.serverLevel(), phantom.position(), RuntimePools.current().phantom(), player.getRandom());
            }
        }
    }

    private static void clampExperience(ServerPlayer player) {
        int cap = TravailConfig.UNDERWORLD_EXPERIENCE_LEVEL_CAP.get();
        if (player.experienceLevel < cap) return;
        player.experienceLevel = cap;
        player.experienceProgress = 0.0F;
        player.totalExperience = experienceAtLevel(cap);
    }

    private static boolean touchesFluid(ServerPlayer player) {
        return player.isInFluidType() || player.isInWaterOrBubble() || player.isInLava();
    }

    public static boolean isFlyingAttack(ServerPlayer player, DamageSource source) {
        Entity direct = source.getDirectEntity();
        return direct instanceof Projectile projectile
                ? projectile.getPersistentData().getBoolean(FLYING_PROJECTILE)
                : isFlying(player);
    }

    private static boolean isFlying(ServerPlayer player) {
        return player.getAbilities().flying || player.isFallFlying();
    }

    private static void removeOneBeneficial(ServerPlayer player) {
        List<MobEffect> effects = beneficialEffects(player);
        if (!effects.isEmpty()) EffectChanges.remove(player, effects.get(player.getRandom().nextInt(effects.size())), true);
    }

    private static void removeAllBeneficial(ServerPlayer player) {
        beneficialEffects(player).forEach(effect -> EffectChanges.remove(player, effect, true));
    }

    private static List<MobEffect> beneficialEffects(ServerPlayer player) {
        return player.getActiveEffects().stream().filter(instance -> instance.getEffect().getCategory() == MobEffectCategory.BENEFICIAL)
                .map(MobEffectInstance::getEffect).distinct().toList();
    }

    private static void grantRandomBeneficial(ServerPlayer player, WeightedTable<EffectEntry> configured, int duration) {
        if (configured.entries().isEmpty() || !AutomaticEffectBudget.acquire(player)) return;
        EffectEntry selected = configured.choose(player.getRandom());
        if (selected == null) return;
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(selected.id());
        int amplifier = player.getRandom().nextInt(selected.maxLevel());
        EffectChanges.add(player, new MobEffectInstance(effect, duration, amplifier));
    }

    private static int configuredMaxLevel(MobEffect effect) {
        ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        if (id == null) return TravailConfig.FAR_WITNESS_FALLBACK_MAX_LEVEL.get();
        return RuntimePools.current().maxLevels().getOrDefault(id, TravailConfig.FAR_WITNESS_FALLBACK_MAX_LEVEL.get());
    }

    private static void spawnFishingReward(ServerPlayer player, ItemFishedEvent event) {
        Vec3 position = event.getHookEntity().position();
        spawnRewardAt(player.serverLevel(), position, RuntimePools.current().fish(), player.getRandom());
    }

    private static void spawnFishingEntity(ServerPlayer player, ItemFishedEvent event) {
        WeightedEntry selected = RuntimePools.current().entities().choose(player.getRandom());
        if (selected == null) return;
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(selected.id());
        Entity entity = type == null ? null : type.create(player.serverLevel());
        if (entity != null) {
            Vec3 pos = event.getHookEntity().position();
            entity.moveTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0F);
            player.serverLevel().addFreshEntity(entity);
        }
    }

    private static void spawnRewardAt(ServerLevel level, Vec3 position, WeightedTable<RewardEntry> configured, RandomSource random) {
        RewardDelivery queue = RewardDelivery.get(level.getServer());
        try (var reservation = queue.reserve()) {
            if (reservation == null) { queue.warnFull(level.getServer()); return; }
            queueReward(reservation, level, position, configured, random);
        }
    }

    private static void queueReward(RewardDelivery.Reservation reservation, ServerLevel level, Vec3 position,
                                    WeightedTable<RewardEntry> configured, RandomSource random) {
        RewardEntry selected = configured.choose(random);
        if (selected == null) return;
        Item item = ForgeRegistries.ITEMS.getValue(selected.id());
        if (item == null) return;
        int count = selected.minCount() + random.nextInt(selected.maxCount() - selected.minCount() + 1);
        reservation.submit(level, position, new ItemStack(item), count);
    }

    private static float safeMultiply(float amount, float multiplier) {
        double result = (double) amount * multiplier;
        return !Double.isFinite(result) || result >= Float.MAX_VALUE ? Float.MAX_VALUE : (float) Math.max(0.0D, result);
    }

    private static boolean isDue(ServerPlayer player, double seconds, int task) {
        int interval = secondsToTicks(seconds);
        return PeriodicSchedule.due(player.level().getGameTime(), player.getUUID().hashCode(), task, interval);
    }

    private static int secondsToTicks(double seconds) {
        if (seconds <= 0.0D) return 1;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, Math.round(seconds * 20.0D)));
    }

    private static float safeFloat(double value) {
        return value >= Float.MAX_VALUE ? Float.MAX_VALUE : (float) Math.max(0.0D, value);
    }

    private static int experienceAtLevel(int level) {
        if (level <= 0) return 0;
        double total;
        if (level <= 16) total = level * level + 6.0D * level;
        else if (level <= 31) total = 2.5D * level * level - 40.5D * level + 360.0D;
        else total = 4.5D * level * level - 162.5D * level + 2220.0D;
        return total >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0.0D, Math.floor(total));
    }
}

package com.thelongtravail.config;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.storage.ConfigFilesDescription;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class TravailConfig {
    public enum FluidProtection {
        ENFORCED,
        RESPECT_CANCELLATION,
        STANDARD
    }

    public enum ReceivedTrigger {
        ATTACK_ATTEMPT,
        HEALTH_LOSS
    }

    public enum RewardOverflow {
        DEFER,
        LIMIT
    }

    public static final ForgeConfigSpec.EnumValue<FluidProtection> FLUID_PROTECTION;
    public static final ForgeConfigSpec.EnumValue<ReceivedTrigger> RECEIVED_TRIGGER;
    public static final ForgeConfigSpec.IntValue RECEIVED_COOLDOWN;
    public static final ForgeConfigSpec.IntValue MAX_AUTOMATIC_EFFECT_ACTIONS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RECEIVED_EXCLUSIONS;
    public static final ForgeConfigSpec.IntValue PHANTOM_MAX_KILLS,
            REWARD_ENTITIES_PER_TICK,
            REWARD_QUEUE_CAPACITY,
            REWARD_LIMIT;
    public static final ForgeConfigSpec.EnumValue<RewardOverflow> REWARD_OVERFLOW;
    public static final ForgeConfigSpec.DoubleValue VISUAL_DARKENING;
    public static final ForgeConfigSpec.DoubleValue VISUAL_OPACITY;
    public static final ForgeConfigSpec.DoubleValue VISUAL_RADIUS;
    public static final ForgeConfigSpec.DoubleValue VISUAL_SOFTNESS;
    public static final ForgeConfigSpec.DoubleValue VISUAL_DISTANCE_START;
    public static final ForgeConfigSpec.DoubleValue VISUAL_DISTANCE_END;
    public static final ForgeConfigSpec.DoubleValue VISUAL_PULSE_PERIOD;
    public static final ForgeConfigSpec.DoubleValue VISUAL_PULSE_DEPTH;
    public static final ForgeConfigSpec.BooleanValue VISUAL_DISTANCE_ENABLED, VISUAL_PULSE_RADIUS;
    public static final ForgeConfigSpec.IntValue VISUAL_REFRESH_TICKS, VISUAL_CLEAR_TICKS;
    public static final Map<String, ForgeConfigSpec> SPECS;
    public static final ForgeConfigSpec.BooleanValue AMONG_ALL_THINGS_IMMUNE_TO_KILL;
    public static final ForgeConfigSpec.BooleanValue GIVE_STARTER_ITEM;
    public static final ForgeConfigSpec.BooleanValue PROTECT_ARCHAEOLOGY_LOOT;
    public static final ForgeConfigSpec.BooleanValue MUTE_BENEFICIAL_EFFECT_GAIN_SOUNDS;
    public static final Map<TravailAspect, ForgeConfigSpec.ConfigValue<List<? extends String>>>
            BIOME_POOLS = new EnumMap<>(TravailAspect.class);
    public static final Map<TravailAspect, ForgeConfigSpec.ConfigValue<List<? extends String>>>
            STRUCTURE_POOLS = new EnumMap<>(TravailAspect.class);
    public static final ForgeConfigSpec.IntValue BIOME_DRAW_COUNT;
    public static final ForgeConfigSpec.IntValue STRUCTURE_DRAW_COUNT;
    public static final ForgeConfigSpec.IntValue WAYGUIDE_RADIUS,
            WAYGUIDE_BIOME_SPACING,
            WAYGUIDE_HEIGHT_SPACING;
    public static final ForgeConfigSpec.IntValue WAYGUIDE_GUIDE_DURATION_SECONDS;
    public static final ForgeConfigSpec.IntValue WAYGUIDE_SAMPLES_PER_TICK,
            WAYGUIDE_MAX_SAMPLES,
            WAYGUIDE_SEARCH_MILLIS,
            WAYGUIDE_CACHE_TICKS,
            WAYGUIDE_TIMEOUT_SECONDS;
    public static final ForgeConfigSpec.IntValue MAX_HEALING_BONUS;
    public static final ForgeConfigSpec.DoubleValue FLOURISHING_HEALING_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue FLOURISHING_HEALTH_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue FLOURISHING_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue FLOURISHING_BONUS_PER_BIOME;
    public static final ForgeConfigSpec.DoubleValue ABYSS_FLUID_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue ABYSS_FLUID_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.DoubleValue ABYSS_MINING_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue ABYSS_ENVIRONMENT_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ABYSS_CANCEL_CHANCE;
    public static final ForgeConfigSpec.BooleanValue FLUID_EROSION_UNAVOIDABLE;
    public static final ForgeConfigSpec.DoubleValue FAR_ALL_CLEAR_CHANCE;
    public static final ForgeConfigSpec.DoubleValue FAR_ATTACK_CLEAR_CHANCE;
    public static final ForgeConfigSpec.DoubleValue FAR_ATTACK_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.IntValue FAR_WITNESS_LEVEL_BONUS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>>
            FAR_WITNESS_LEVEL_BLACKLIST;
    public static final ForgeConfigSpec.IntValue FAR_WITNESS_FALLBACK_MAX_LEVEL;
    public static final ForgeConfigSpec.DoubleValue FAR_WITNESS_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.IntValue FAR_WITNESS_ACTION_COUNT;
    public static final ForgeConfigSpec.DoubleValue FAR_WITNESS_POSITIVE_DURATION_SECONDS;
    public static final ForgeConfigSpec.DoubleValue VALLEY_VISUAL_DEPRIVATION_CHANCE;
    public static final ForgeConfigSpec.DoubleValue VALLEY_VISUAL_DEPRIVATION_DURATION_SECONDS;
    public static final ForgeConfigSpec.IntValue VALLEY_VISUAL_DEPRIVATION_FADE_IN_TICKS;
    public static final ForgeConfigSpec.IntValue VALLEY_VISUAL_DEPRIVATION_FADE_OUT_TICKS;
    public static final ForgeConfigSpec.DoubleValue VALLEY_STIFF_CHANCE;
    public static final ForgeConfigSpec.DoubleValue VALLEY_STIFF_DURATION_SECONDS;
    public static final ForgeConfigSpec.DoubleValue VALLEY_SWIFTNESS_DURATION_SECONDS;
    public static final ForgeConfigSpec.IntValue VALLEY_SWIFTNESS_LEVEL;
    public static final ForgeConfigSpec.IntValue VALLEY_RANDOM_EFFECT_COUNT;
    public static final ForgeConfigSpec.DoubleValue VALLEY_RANDOM_EFFECT_DURATION_SECONDS;
    public static final ForgeConfigSpec.DoubleValue VALLEY_WITNESS_ATTACK_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue UNDERWORLD_DAMAGE_REDUCTION;
    public static final ForgeConfigSpec.IntValue UNDERWORLD_EXPERIENCE_LEVEL_CAP;
    public static final ForgeConfigSpec.IntValue UNDERWORLD_ANVIL_LEVEL_CAP;
    public static final ForgeConfigSpec.IntValue UNDERWORLD_FORTUNE_BONUS;
    public static final ForgeConfigSpec.IntValue UNDERWORLD_LOOTING_BONUS;
    public static final ForgeConfigSpec.DoubleValue FISH_ENTITY_CHANCE;
    public static final ForgeConfigSpec.DoubleValue FISH_SPECIAL_CHANCE;
    public static final ForgeConfigSpec.DoubleValue BOUNDLESS_HEIGHT_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue BOUNDLESS_SPEED_REDUCTION;
    public static final ForgeConfigSpec.IntValue BOUNDLESS_EXTRA_CURIO_SLOTS;
    public static final ForgeConfigSpec.IntValue PHANTOM_RANGE;
    public static final ForgeConfigSpec.DoubleValue PHANTOM_CHECK_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>>
            FAR_REACH_POSITIVE_EFFECTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>>
            DEEP_VALLEY_POSITIVE_EFFECTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FISH_SPECIAL_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FISH_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PHANTOM_DROPS;

    static {
        Map<String, ForgeConfigSpec.Builder> builders = new java.util.LinkedHashMap<>();
        var general =
                GeneralConfigDefinition.define(
                        builders.computeIfAbsent(
                                "general.toml", key -> new ForgeConfigSpec.Builder()));
        AMONG_ALL_THINGS_IMMUNE_TO_KILL = general.amongAllThingsImmuneToKill();
        PROTECT_ARCHAEOLOGY_LOOT = general.protectArchaeologyLoot();
        MUTE_BENEFICIAL_EFFECT_GAIN_SOUNDS = general.muteBeneficialEffectGainSounds();
        GIVE_STARTER_ITEM = general.giveStarterItem();
        BIOME_DRAW_COUNT = general.biomeDrawCount();
        STRUCTURE_DRAW_COUNT = general.structureDrawCount();
        RECEIVED_TRIGGER = general.receivedTrigger();
        RECEIVED_COOLDOWN = general.receivedCooldown();
        MAX_AUTOMATIC_EFFECT_ACTIONS = general.maxAutomaticEffectActions();
        RECEIVED_EXCLUSIONS = general.receivedExclusions();
        var utility =
                UtilityItemsDefinition.define(
                        builders.computeIfAbsent(
                                "utility_items.toml", key -> new ForgeConfigSpec.Builder()));
        WAYGUIDE_GUIDE_DURATION_SECONDS = utility.wayguideGuideDurationSeconds();
        WAYGUIDE_RADIUS = utility.wayguideRadius();
        WAYGUIDE_TIMEOUT_SECONDS = utility.wayguideTimeoutSeconds();
        WAYGUIDE_BIOME_SPACING = utility.wayguideBiomeSpacing();
        WAYGUIDE_HEIGHT_SPACING = utility.wayguideHeightSpacing();
        WAYGUIDE_SAMPLES_PER_TICK = utility.wayguideSamplesPerTick();
        WAYGUIDE_SEARCH_MILLIS = utility.wayguideSearchMillis();
        WAYGUIDE_MAX_SAMPLES = utility.wayguideMaxSamples();
        WAYGUIDE_CACHE_TICKS = utility.wayguideCacheTicks();
        var rewards =
                RewardDeliveryDefinition.define(
                        builders.computeIfAbsent(
                                "general.toml", key -> new ForgeConfigSpec.Builder()));
        REWARD_ENTITIES_PER_TICK = rewards.rewardEntitiesPerTick();
        REWARD_QUEUE_CAPACITY = rewards.rewardQueueCapacity();
        REWARD_OVERFLOW = rewards.rewardOverflow();
        REWARD_LIMIT = rewards.rewardLimit();
        var flourishing =
                FlourishingDefinition.define(
                        builders.computeIfAbsent(
                                "aspects/flourishing.toml", key -> new ForgeConfigSpec.Builder()),
                        BIOME_POOLS,
                        STRUCTURE_POOLS);
        MAX_HEALING_BONUS = flourishing.maxHealingBonus();
        FLOURISHING_HEALING_REDUCTION = flourishing.flourishingHealingReduction();
        FLOURISHING_HEALTH_THRESHOLD = flourishing.flourishingHealthThreshold();
        FLOURISHING_DAMAGE_MULTIPLIER = flourishing.flourishingDamageMultiplier();
        FLOURISHING_BONUS_PER_BIOME = flourishing.flourishingBonusPerBiome();
        var abyss =
                AbyssDefinition.define(
                        builders.computeIfAbsent(
                                "aspects/abyss.toml", key -> new ForgeConfigSpec.Builder()),
                        BIOME_POOLS,
                        STRUCTURE_POOLS);
        ABYSS_FLUID_DAMAGE = abyss.abyssFluidDamage();
        ABYSS_FLUID_INTERVAL_SECONDS = abyss.abyssFluidIntervalSeconds();
        ABYSS_MINING_REDUCTION = abyss.abyssMiningReduction();
        ABYSS_ENVIRONMENT_MULTIPLIER = abyss.abyssEnvironmentMultiplier();
        ABYSS_CANCEL_CHANCE = abyss.abyssCancelChance();
        FLUID_EROSION_UNAVOIDABLE = abyss.fluidErosionUnavoidable();
        FLUID_PROTECTION = abyss.fluidProtection();
        var farReach =
                FarReachDefinition.define(
                        builders.computeIfAbsent(
                                "aspects/far_reach.toml", key -> new ForgeConfigSpec.Builder()),
                        BIOME_POOLS,
                        STRUCTURE_POOLS);
        FAR_ALL_CLEAR_CHANCE = farReach.farAllClearChance();
        FAR_ATTACK_CLEAR_CHANCE = farReach.farAttackClearChance();
        FAR_ATTACK_COOLDOWN_SECONDS = farReach.farAttackCooldownSeconds();
        FAR_WITNESS_LEVEL_BONUS = farReach.farWitnessLevelBonus();
        FAR_WITNESS_LEVEL_BLACKLIST = farReach.farWitnessLevelBlacklist();
        FAR_WITNESS_FALLBACK_MAX_LEVEL = farReach.farWitnessFallbackMaxLevel();
        FAR_WITNESS_INTERVAL_SECONDS = farReach.farWitnessIntervalSeconds();
        FAR_WITNESS_ACTION_COUNT = farReach.farWitnessActionCount();
        FAR_WITNESS_POSITIVE_DURATION_SECONDS = farReach.farWitnessPositiveDurationSeconds();
        FAR_REACH_POSITIVE_EFFECTS = farReach.farReachPositiveEffects();
        var deepValley =
                DeepValleyDefinition.define(
                        builders.computeIfAbsent(
                                "aspects/deep_valley.toml", key -> new ForgeConfigSpec.Builder()),
                        BIOME_POOLS,
                        STRUCTURE_POOLS);
        VALLEY_VISUAL_DEPRIVATION_CHANCE = deepValley.valleyVisualDeprivationChance();
        VALLEY_VISUAL_DEPRIVATION_DURATION_SECONDS =
                deepValley.valleyVisualDeprivationDurationSeconds();
        VALLEY_VISUAL_DEPRIVATION_FADE_IN_TICKS = deepValley.valleyVisualDeprivationFadeInTicks();
        VALLEY_VISUAL_DEPRIVATION_FADE_OUT_TICKS = deepValley.valleyVisualDeprivationFadeOutTicks();
        VALLEY_STIFF_CHANCE = deepValley.valleyStiffChance();
        VALLEY_STIFF_DURATION_SECONDS = deepValley.valleyStiffDurationSeconds();
        VALLEY_SWIFTNESS_DURATION_SECONDS = deepValley.valleySwiftnessDurationSeconds();
        VALLEY_SWIFTNESS_LEVEL = deepValley.valleySwiftnessLevel();
        VALLEY_RANDOM_EFFECT_COUNT = deepValley.valleyRandomEffectCount();
        VALLEY_RANDOM_EFFECT_DURATION_SECONDS = deepValley.valleyRandomEffectDurationSeconds();
        VALLEY_WITNESS_ATTACK_COOLDOWN_SECONDS = deepValley.valleyWitnessAttackCooldownSeconds();
        DEEP_VALLEY_POSITIVE_EFFECTS = deepValley.deepValleyPositiveEffects();
        var underworld =
                UnderworldDefinition.define(
                        builders.computeIfAbsent(
                                "aspects/underworld.toml", key -> new ForgeConfigSpec.Builder()),
                        BIOME_POOLS,
                        STRUCTURE_POOLS);
        UNDERWORLD_DAMAGE_REDUCTION = underworld.underworldDamageReduction();
        UNDERWORLD_EXPERIENCE_LEVEL_CAP = underworld.underworldExperienceLevelCap();
        UNDERWORLD_ANVIL_LEVEL_CAP = underworld.underworldAnvilLevelCap();
        UNDERWORLD_FORTUNE_BONUS = underworld.underworldFortuneBonus();
        UNDERWORLD_LOOTING_BONUS = underworld.underworldLootingBonus();
        FISH_ENTITY_CHANCE = underworld.fishEntityChance();
        FISH_SPECIAL_CHANCE = underworld.fishSpecialChance();
        FISH_SPECIAL_ITEMS = underworld.fishSpecialItems();
        FISH_ENTITIES = underworld.fishEntities();
        var boundless =
                BoundlessDefinition.define(
                        builders.computeIfAbsent(
                                "aspects/boundless.toml", key -> new ForgeConfigSpec.Builder()),
                        BIOME_POOLS,
                        STRUCTURE_POOLS);
        BOUNDLESS_HEIGHT_THRESHOLD = boundless.boundlessHeightThreshold();
        BOUNDLESS_SPEED_REDUCTION = boundless.boundlessSpeedReduction();
        BOUNDLESS_EXTRA_CURIO_SLOTS = boundless.boundlessExtraCurioSlots();
        PHANTOM_RANGE = boundless.phantomRange();
        PHANTOM_CHECK_INTERVAL_SECONDS = boundless.phantomCheckIntervalSeconds();
        PHANTOM_MAX_KILLS = boundless.phantomMaxKills();
        PHANTOM_DROPS = boundless.phantomDrops();
        var visual =
                VisualDeprivationDefinition.define(
                        builders.computeIfAbsent(
                                "aspects/deep_valley.toml", key -> new ForgeConfigSpec.Builder()));
        VISUAL_DARKENING = visual.visualDarkening();
        VISUAL_OPACITY = visual.visualOpacity();
        VISUAL_RADIUS = visual.visualRadius();
        VISUAL_SOFTNESS = visual.visualSoftness();
        VISUAL_DISTANCE_START = visual.visualDistanceStart();
        VISUAL_DISTANCE_END = visual.visualDistanceEnd();
        VISUAL_PULSE_PERIOD = visual.visualPulsePeriod();
        VISUAL_PULSE_DEPTH = visual.visualPulseDepth();
        VISUAL_DISTANCE_ENABLED = visual.visualDistanceEnabled();
        VISUAL_PULSE_RADIUS = visual.visualPulseRadius();
        VISUAL_REFRESH_TICKS = visual.visualRefreshTicks();
        VISUAL_CLEAR_TICKS = visual.visualClearTicks();
        CarryingConfig.define(builders.get("general.toml"));
        BoundlessItemsConfig.define(builders.get("aspects/boundless.toml"));
        RainConfig.define(builders.get("aspects/abyss.toml"));
        AbyssItemsConfig.define(builders.get("aspects/abyss.toml"));
        UnderworldItemsConfig.define(builders.get("aspects/underworld.toml"));
        AzraelConfig.define(builders.get("aspects/deep_valley.toml"));
        DeepValleyItemsConfig.define(builders.get("aspects/deep_valley.toml"));
        com.thelongtravail.config.SwordLanternConfig.define(
                builders.get("aspects/deep_valley.toml"));
        FlourishingItemsConfig.define(builders.get("aspects/flourishing.toml"));
        FarReachItemsConfig.define(builders.get("aspects/far_reach.toml"));
        ThemeItemConfig.define(builders);
        ModeEffectRules.define(builders);
        Map<String, ForgeConfigSpec> specs = new java.util.LinkedHashMap<>();
        builders.forEach(
                (file, value) -> {
                    ConfigFilesDescription.describe(value, file);
                    specs.put(file, value.build());
                });
        SPECS = java.util.Collections.unmodifiableMap(specs);
    }

    private TravailConfig() {}
}

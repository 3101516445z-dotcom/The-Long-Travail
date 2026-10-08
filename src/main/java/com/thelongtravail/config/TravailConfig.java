package com.thelongtravail.config;

import com.thelongtravail.TravailAspect;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class TravailConfig {
    public enum FluidProtection { ENFORCED, RESPECT_CANCELLATION, STANDARD }
    public enum ReceivedTrigger { ATTACK_ATTEMPT, HEALTH_LOSS }
    public enum RewardOverflow { DEFER, LIMIT }
    public static final ForgeConfigSpec.EnumValue<FluidProtection> FLUID_PROTECTION;
    public static final ForgeConfigSpec.EnumValue<ReceivedTrigger> RECEIVED_TRIGGER;
    public static final ForgeConfigSpec.IntValue RECEIVED_COOLDOWN;
    public static final ForgeConfigSpec.IntValue MAX_AUTOMATIC_EFFECT_ACTIONS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RECEIVED_EXCLUSIONS;
    public static final ForgeConfigSpec.IntValue PHANTOM_MAX_KILLS, REWARD_ENTITIES_PER_TICK, REWARD_QUEUE_CAPACITY, REWARD_LIMIT;
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
    public static final ForgeConfigSpec.BooleanValue GIVE_STARTER_ITEM;
    public static final ForgeConfigSpec.BooleanValue MUTE_BENEFICIAL_EFFECT_GAIN_SOUNDS;
    public static final Map<TravailAspect, ForgeConfigSpec.ConfigValue<List<? extends String>>> BIOME_POOLS = new EnumMap<>(TravailAspect.class);
    public static final Map<TravailAspect, ForgeConfigSpec.ConfigValue<List<? extends String>>> STRUCTURE_POOLS = new EnumMap<>(TravailAspect.class);

    public static final ForgeConfigSpec.IntValue BIOME_DRAW_COUNT;
    public static final ForgeConfigSpec.IntValue STRUCTURE_DRAW_COUNT;
    public static final ForgeConfigSpec.IntValue WAYGUIDE_RADIUS, WAYGUIDE_BIOME_SPACING, WAYGUIDE_HEIGHT_SPACING;
    public static final ForgeConfigSpec.IntValue WAYGUIDE_GUIDE_DURATION_SECONDS;
    public static final ForgeConfigSpec.IntValue WAYGUIDE_SAMPLES_PER_TICK, WAYGUIDE_MAX_SAMPLES, WAYGUIDE_SEARCH_MILLIS, WAYGUIDE_CACHE_TICKS, WAYGUIDE_TIMEOUT_SECONDS;
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
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FAR_WITNESS_LEVEL_BLACKLIST;
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

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FAR_REACH_POSITIVE_EFFECTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DEEP_VALLEY_POSITIVE_EFFECTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FISH_SPECIAL_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FISH_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PHANTOM_DROPS;

    static {
        Map<String, ForgeConfigSpec.Builder> builders = new java.util.LinkedHashMap<>();
        ForgeConfigSpec.Builder builder;

        builder = builders.computeIfAbsent("general.toml", key -> new ForgeConfigSpec.Builder());
        com.thelongtravail.loot.UnifiedLootConfig.define(builder);
        MUTE_BENEFICIAL_EFFECT_GAIN_SOUNDS = builder.comment(
                "佩戴苦旅且拥有穷遐之见证或幽谷之见证时，关闭Sounds模组在获得正面药水效果时播放的提示音，不限效果来源。\n不影响饮用、装备操作和减益效果的提示音。关闭此选项后，仍保留苦旅自身效果原有的静音规则。",
                "While wearing The Long Travail with Far Reach Witness or Deep Valley Witness, suppress the Sounds mod's notification sound for gaining beneficial potion effects, regardless of their source.\nDoes not affect drinking sounds, equipment interaction sounds, or harmful-effect notifications. Disabling this option preserves The Long Travail's existing rules for silencing its own effects.")
                .define("compatibility.muteBeneficialEffectGainSounds", true);
        GIVE_STARTER_ITEM = builder.comment(
                "玩家首次进入世界时是否赠送一个“苦旅”；每名玩家最多赠送一次。",
                "Give a player one The Long Travail item when they first enter the world; at most once per player.")
                .define("starter.giveStarterItem", true);
        BIOME_DRAW_COUNT = integer(builder, "requirements.biomeDrawCount", 3, 0, 100, "为每种恶意抽取的不同群系数量。", "Number of distinct biomes drawn for each Malice.");
        STRUCTURE_DRAW_COUNT = integer(builder, "requirements.structureDrawCount", 0, 0, 100, "为每种恶意抽取的不同结构数量。", "Number of distinct structures drawn for each Malice.");
        RECEIVED_TRIGGER = builder.comment("受击恶意的触发方式：ATTACK_ATTEMPT 表示受到攻击时触发；HEALTH_LOSS 表示实际生命值减少后才触发，不含吸收生命值的损失。", "Trigger mode for on-hit Malice: ATTACK_ATTEMPT triggers when attacked; HEALTH_LOSS triggers only after actual health loss, excluding absorption loss.")
                .defineEnum("receivedMalice.receivedMaliceTrigger", ReceivedTrigger.ATTACK_ATTEMPT);
        RECEIVED_COOLDOWN = integer(builder, "receivedMalice.receivedMaliceCooldownTicks", 0, 0, 1_000_000, "各类受击恶意共用的冷却时间，单位：游戏刻；0表示无冷却。此设置不改变伤害本身，也不影响由攻击触发的效果。", "Shared cooldown for the different on-hit Malice effects, in game ticks; 0 means no cooldown. Does not change damage itself or effects triggered by attacking.");
        MAX_AUTOMATIC_EFFECT_ACTIONS = integer(builder, "effectLimits.maxAutomaticEffectActionsPerPlayerPerSecond", 0, 0, 1_000_000,
                "每名玩家任意连续20游戏刻内，穷遐之见证与幽谷之见证共用的自动效果尝试次数上限。\n包括穷遐的净化和增益，以及幽谷攻击触发的增益；失败或被取消的尝试也计数。\n超出上限的尝试直接跳过，不补发。0表示不限次数。\n不影响受击恶意、视觉剥夺，以及幽谷的黑暗和失明免疫。",
                "Maximum automatic effect attempts shared by Far Reach Witness and Deep Valley Witness per player within any consecutive 20 game ticks.\nIncludes Far Reach cleansing and buffs, and buffs triggered by Deep Valley attacks. Failed or canceled attempts also count.\nAttempts exceeding the limit are skipped and are not retried later. 0 means unlimited attempts.\nDoes not affect on-hit Malice, Visual Deprivation, or Deep Valley's Darkness and Blindness immunity.");
        RECEIVED_EXCLUSIONS = builder.comment("不触发受击恶意的伤害类型ID；不改变伤害本身。", "Damage type IDs that do not trigger on-hit Malice; does not change damage itself.")
                .defineListAllowEmpty("receivedMalice.receivedMaliceExcludedDamageTypes", List.of(), value -> value instanceof String id && net.minecraft.resources.ResourceLocation.tryParse(id) != null);


        builder = builders.computeIfAbsent("utility_items.toml", key -> new ForgeConfigSpec.Builder());
        WAYGUIDE_GUIDE_DURATION_SECONDS = integer(builder, "items.wayguide.guideDurationSeconds", 10, 1, 3600,
                "路引找到目标后，从开始飞出到变成掉落物的持续时间，单位：秒，包含飞行和悬停；不包含搜索等待时间。修改后也会影响正在指路的路引。",
                "Duration from launching toward a found target until becoming a dropped item, in seconds, including flight and hovering but excluding search time. Changes also affect Wayguides already in flight.");
        WAYGUIDE_RADIUS = integer(builder, "items.wayguide.search.searchRadius", 5000, 16, 5000, "路引的最大水平搜索半径，单位：方块，最多5000。未找到目标不代表整个维度都没有目标。", "Maximum horizontal Wayguide search radius, in blocks, capped at 5000. Not finding a target does not mean none exists in the entire dimension.");
        WAYGUIDE_TIMEOUT_SECONDS = integer(builder, "items.wayguide.search.searchTimeoutSeconds", 30, 1, 300, "单次搜索允许的最长等待时间，从搜索请求被接受时开始计算，包含准备和排队时间，单位：现实时间的秒。系统检查到超时后，会停止搜索并返还路引。", "Maximum elapsed time for a search, measured from acceptance of the request and including preparation and queueing, in real-world seconds. When the system detects a timeout, it stops the search and returns the Wayguide.");
        WAYGUIDE_BIOME_SPACING = integer(builder, "items.wayguide.sampling.biomeSampleSpacing", 32, 4, 256, "群系的水平采样间距，单位：方块；数值越小，搜索越精细，但耗时也越长。", "Horizontal biome sample spacing, in blocks. Smaller values provide finer searches but take longer.");
        WAYGUIDE_HEIGHT_SPACING = integer(builder, "items.wayguide.sampling.heightSampleSpacing", 32, 4, 256, "群系的垂直采样间距，单位：方块；始终额外检查玩家所在高度。", "Vertical biome sample spacing, in blocks. The player's height is always checked in addition.");
        WAYGUIDE_SAMPLES_PER_TICK = integer(builder, "items.wayguide.budget.samplesPerTick", 2048, 1, 65536, "全服每游戏刻最多进行的路引搜索采样次数，由所有玩家共用。", "Maximum Wayguide search samples per game tick across the server, shared by all players.");
        WAYGUIDE_SEARCH_MILLIS = integer(builder, "items.wayguide.budget.searchMillisPerTick", 2, 1, 50, "每游戏刻用于常规搜索处理的目标耗时，单位：毫秒，包含准备、检查、采样和收尾。此值不是严格上限，单次操作及必要的状态清理可能使实际耗时超过此值。", "Target time spent on normal search processing per game tick, in milliseconds, including preparation, checks, sampling, and completion. This is not a strict limit; individual operations and necessary state cleanup may exceed it.");
        WAYGUIDE_MAX_SAMPLES = integer(builder, "items.wayguide.budget.maxSamplesPerSearch", 8000000, 1, 32000000, "单次路引搜索的采样次数上限；达到上限后停止搜索并返还路引。", "Maximum samples per Wayguide search. Reaching the limit stops the search and returns the Wayguide.");
        WAYGUIDE_CACHE_TICKS = integer(builder, "items.wayguide.cache.cacheTicks", 1200, 0, 72000, "搜索结果的缓存时间，单位：游戏刻；0表示不缓存。移动超过128格、切换维度或苦旅进度变化后，缓存失效。", "Search-result cache duration, in game ticks; 0 disables caching. Moving more than 128 blocks, changing dimensions, or changing Long Travail progress invalidates the cache.");


        builder = builders.computeIfAbsent("general.toml", key -> new ForgeConfigSpec.Builder());
        REWARD_ENTITIES_PER_TICK = integer(builder, "rewardDelivery.rewardMaxItemEntitiesPerTick", 32, 1, 1024, "全服每游戏刻最多生成的额外奖励物品实体数；不限制原版掉落。", "Maximum extra-reward item entities spawned per game tick across the server. Does not limit vanilla drops.");
        REWARD_QUEUE_CAPACITY = integer(builder, "rewardDelivery.rewardQueueCapacity", 1024, 1, 16384, "待发奖励的记录数量上限。达到上限时，暂停自动击杀幻翼，并跳过钓鱼的额外物品奖励。调低上限不会删除已有记录。", "Maximum pending reward records. At the limit, automatic phantom kills pause and extra fishing item rewards are skipped. Lowering the limit does not delete existing records.");
        REWARD_OVERFLOW = builder.comment("DEFER：分批发放全部奖励。\nLIMIT：每次最多发放 rewardMaxEntitiesPerReward 堆物品，超出部分不发放。\n两种方式均受全服每游戏刻发放上限限制。",
                "DEFER: deliver all rewards in batches.\nLIMIT: deliver at most rewardMaxEntitiesPerReward item stacks per reward; discard the excess.\nBoth modes are subject to the server-wide per-tick delivery limit.")
                .defineEnum("rewardDelivery.rewardOverflowPolicy", RewardOverflow.DEFER);
        REWARD_LIMIT = integer(builder, "rewardDelivery.rewardMaxEntitiesPerReward", 64, 1, 4096, "LIMIT模式下，每次奖励最多发放的物品堆数。", "Maximum item stacks delivered per reward in LIMIT mode.");


        builder = builders.computeIfAbsent("aspects/flourishing.toml", key -> new ForgeConfigSpec.Builder());
        MAX_HEALING_BONUS = integer(builder, "travail.witness.maxHealingBonus", 0, 0, 1_000_000, "繁茂之见证的治疗量累计加成上限；100表示额外增加10000%，0表示无上限。", "Maximum accumulated healing bonus from Flourishing Witness; 100 means an additional 10000%, and 0 means no limit.");
        FLOURISHING_HEALING_REDUCTION = decimal(builder, "travail.malice.healingReduction", 0.80D, 0.0D, 1.0D, "受到的治疗量降低的比例；0.8表示削减80%。", "Fractional reduction in healing received; 0.8 means an 80% reduction.");
        FLOURISHING_HEALTH_THRESHOLD = decimal(builder, "travail.malice.doubleDamageHealthThreshold", 0.50D, 0.0D, 1.0D, "当前生命值占最大生命值的比例高于此值时，应用 incomingDamageMultiplier 指定的受伤倍率。", "When current health as a fraction of maximum health is above this value, apply the incomingDamageMultiplier damage multiplier.");
        FLOURISHING_DAMAGE_MULTIPLIER = decimal(builder, "travail.malice.incomingDamageMultiplier", 2.0D, 0.0D, 1_000_000.0D, "生命值比例高于 doubleDamageHealthThreshold 时，受到的伤害倍率。", "Incoming damage multiplier while the health fraction is above doubleDamageHealthThreshold.");
        FLOURISHING_BONUS_PER_BIOME = decimal(builder, "travail.witness.healingBonusPerBiome", 0.05D, 0.0D, 1_000_000.0D, "每发现一个群系增加的治疗量加成；0.05表示额外增加5%。", "Healing bonus added per biome discovered; 0.05 means an additional 5%.");
        defineRequirements(builder, TravailAspect.FLOURISHING,
                List.of("minecraft:plains|@biome.minecraft.plains|3", "minecraft:forest|@biome.minecraft.forest|3", "minecraft:taiga|@biome.minecraft.taiga|3", "minecraft:swamp|@biome.minecraft.swamp|1", "minecraft:jungle|@biome.minecraft.jungle|1", "minecraft:savanna|@biome.minecraft.savanna|1"),
                List.of());


        builder = builders.computeIfAbsent("aspects/abyss.toml", key -> new ForgeConfigSpec.Builder());
        ABYSS_FLUID_DAMAGE = decimal(builder, "travail.malice.fluidTrueDamage", 1.0D, 0.0D, 1_000_000.0D, "每次流体伤害造成的真实伤害值。", "True damage dealt by each fluid damage trigger.");
        ABYSS_FLUID_INTERVAL_SECONDS = decimal(builder, "travail.malice.fluidDamageIntervalSeconds", 1.0D, 0.0D, 1_000_000.0D, "流体伤害的触发间隔，单位：秒；0表示每游戏刻触发。间隔过短可能增加游戏负担，多人时更明显。", "Interval between fluid damage triggers, in seconds; 0 triggers every game tick. Short intervals may increase processing load, especially with multiple players.");
        ABYSS_MINING_REDUCTION = decimal(builder, "travail.malice.miningSpeedReduction", 0.50D, 0.0D, 1.0D, "挖掘速度削减比例；0.5表示削减50%。", "Fractional mining speed reduction; 0.5 means a 50% reduction.");
        ABYSS_ENVIRONMENT_MULTIPLIER = decimal(builder, "travail.malice.environmentDamageMultiplier", 10.0D, 0.0D, 1_000_000.0D, "窒息、挤压、溺水与冻结伤害倍率；见证免疫同一组伤害。", "Damage multiplier for suffocation, entity cramming, drowning, and freezing. Witness grants immunity to the same damage types.");
        ABYSS_CANCEL_CHANCE = decimal(builder, "travail.witness.fluidDamageCancelChance", 0.50D, 0.0D, 1.0D, "归墟之见证在流体中取消伤害的概率。", "Abyss Witness chance to negate damage while in fluid.");
        FLUID_EROSION_UNAVOIDABLE = builder.comment(
                "是否防止流体真实伤害在攻击、受伤和伤害结算事件中被取消或降为零，并保留苦旅自身倍率计算后的伤害下限。\nPrevent fluid true damage from being canceled or reduced to zero in attack, hurt, and damage-calculation events, and preserve a damage floor after The Long Travail's own multipliers are applied.",
                "仍遵守苦旅自身的免伤和输出限制，不绕过伤害事件之前的无敌判定、图腾或死亡事件。关闭后仍保留流体伤害原有的护甲等穿透规则。",
                "Still follows The Long Travail's own damage immunity and output limits. Does not bypass invulnerability checks before damage events, totems, or death events. Disabling this preserves fluid damage's existing armor and other bypass rules.",
                "Respects Travail immunity/output restrictions; does not bypass earlier entity checks, totems or death events. Damage-type bypass tags remain when disabled.")
                .define("travail.malice.fluidErosionUnavoidable", true);
        FLUID_PROTECTION = builder.comment("ENFORCED：防止事件取消伤害，并保留伤害下限。\nRESPECT_CANCELLATION：允许事件取消伤害；伤害未被取消时，保留下限。\nSTANDARD：允许事件取消伤害或降低伤害。\nENFORCED: prevent events from canceling damage and preserve a damage floor.\nRESPECT_CANCELLATION: allow events to cancel damage; preserve the floor if damage is not canceled.\nSTANDARD: allow events to cancel or reduce damage.",
                "fluidErosionUnavoidable=false 时，始终使用 STANDARD。所有模式均保留流体伤害原有的护甲和吸收生命值穿透规则。\nWhen fluidErosionUnavoidable=false, STANDARD is always used. All modes retain fluid damage's existing armor and absorption bypass rules.",
                "ENFORCED prevents event cancellation and enforces the damage floor. RESPECT_CANCELLATION allows cancellation, but retains the floor if not cancelled. STANDARD allows cancellation or damage reduction.",
                "When fluidErosionUnavoidable=false, STANDARD always applies. All modes retain the original armor and absorption bypass rules.")
                .defineEnum("travail.malice.fluidErosionProtectionMode", FluidProtection.ENFORCED);
        defineRequirements(builder, TravailAspect.ABYSS,
                List.of("minecraft:ocean|@biome.minecraft.ocean|3", "minecraft:deep_ocean|@biome.the_long_travail.warm_deep_ocean|3", "minecraft:warm_ocean|@biome.minecraft.warm_ocean|3", "minecraft:lukewarm_ocean|@biome.minecraft.lukewarm_ocean|2", "minecraft:deep_lukewarm_ocean|@biome.minecraft.deep_lukewarm_ocean|2", "minecraft:cold_ocean|@biome.minecraft.cold_ocean|1", "minecraft:deep_cold_ocean|@biome.minecraft.deep_cold_ocean|1", "minecraft:frozen_ocean|@biome.minecraft.frozen_ocean|1", "minecraft:deep_frozen_ocean|@biome.minecraft.deep_frozen_ocean|1"),
                List.of());


        builder = builders.computeIfAbsent("aspects/far_reach.toml", key -> new ForgeConfigSpec.Builder());
        FAR_ALL_CLEAR_CHANCE = decimal(builder, "travail.malice.clearAllBeneficialChanceOnHit", 0.10D, 0.0D, 1.0D, "受击时清除全部增益效果的概率。", "Chance to remove all beneficial effects when hit.");
        FAR_ATTACK_CLEAR_CHANCE = decimal(builder, "travail.malice.clearBeneficialChanceOnAttack", 0.50D, 0.0D, 1.0D, "攻击后清除一个增益效果的概率。", "Chance to remove one beneficial effect after attacking.");
        FAR_ATTACK_COOLDOWN_SECONDS = decimal(builder, "travail.malice.attackClearCooldownSeconds", 5.0D, 0.0D, 1_000_000.0D, "攻击触发增益效果清除的冷却时间，单位：秒；0表示无冷却。", "Cooldown for beneficial-effect removal triggered by attacking, in seconds; 0 means no cooldown.");
        FAR_WITNESS_LEVEL_BONUS = integer(builder, "travail.witness.beneficialLevelBonus", 1, 0, 255, "新获得增益效果时增加的等级数。", "Levels added to newly gained beneficial effects.");
        FAR_WITNESS_LEVEL_BLACKLIST = builder.comment("不接受穷遐之见证等级加成的效果ID列表，例如 minecraft:night_vision。\n仅跳过等级提升，不阻止获得效果，也不改变随机效果池。空列表表示不排除任何效果。",
                "Effect IDs excluded from Far Reach Witness's level bonus, such as minecraft:night_vision.\nOnly skips the level increase. Does not prevent gaining the effect or change random effect pools. An empty list excludes no effects.")
                .defineListAllowEmpty("travail.witness.beneficialLevelBonusBlacklist", List.of("minecraft:night_vision", "minecraft:water_breathing"), value -> value instanceof String);
        FAR_WITNESS_FALLBACK_MAX_LEVEL = integer(builder, "travail.witness.unlistedEffectMaxLevel", 5, 1, 256, "既未列入穷遐效果池、也未列入幽谷效果池的增益效果等级上限。", "Maximum level for beneficial effects absent from both the Far Reach and Deep Valley effect pools.");
        FAR_WITNESS_INTERVAL_SECONDS = decimal(builder, "travail.witness.witnessIntervalSeconds", 5.0D, 0.0D, 1_000_000.0D, "净化或给予增益效果的触发间隔，单位：秒；0表示每游戏刻触发。间隔过短且每轮次数较多时，可能增加游戏负担。", "Interval between cleansing or granting beneficial effects, in seconds; 0 triggers every game tick. Short intervals combined with many attempts per cycle may increase processing load.");
        FAR_WITNESS_ACTION_COUNT = integer(builder, "travail.witness.cleanseOrGrantCount", 2, 0, 64, "每轮尝试清除减益效果的次数；剩余次数用于给予增益效果。次数过多且触发间隔过短时，可能增加游戏负担。", "Attempts to remove harmful effects per cycle; remaining attempts grant beneficial effects. Many attempts combined with short trigger intervals may increase processing load.");
        FAR_WITNESS_POSITIVE_DURATION_SECONDS = decimal(builder, "travail.witness.grantedEffectDurationSeconds", 5.0D, 0.0D, 1_000_000.0D, "随机给予的增益效果持续时间，单位：秒。", "Duration of randomly granted beneficial effects, in seconds.");
        defineRequirements(builder, TravailAspect.FAR_REACH,
                List.of("minecraft:snowy_plains|@biome.minecraft.snowy_plains|3", "minecraft:desert|@biome.minecraft.desert|3", "minecraft:badlands|@biome.minecraft.badlands|3", "minecraft:windswept_hills|@biome.minecraft.windswept_hills|1", "minecraft:snowy_taiga|@biome.minecraft.snowy_taiga|1", "minecraft:ice_spikes|@biome.minecraft.ice_spikes|1"),
                List.of());
        FAR_REACH_POSITIVE_EFFECTS = builder.comment("格式：效果ID|最高等级|权重", "Format: effect_id|max_level|weight").defineListAllowEmpty("travail.witness.positiveEffects", defaultEffects(), TravailConfig::validEffectEntry);


        builder = builders.computeIfAbsent("aspects/deep_valley.toml", key -> new ForgeConfigSpec.Builder());
        VALLEY_VISUAL_DEPRIVATION_CHANCE = decimal(builder, "travail.malice.visualDeprivationChanceOnHit", 0.50D, 0.0D, 1.0D, "受击时获得视觉剥夺的概率。", "Chance to gain Visual Deprivation when hit.");
        VALLEY_VISUAL_DEPRIVATION_DURATION_SECONDS = decimal(builder, "travail.visualDeprivation.visualDeprivationDurationSeconds", 5.0D, 0.0D, 1_000_000.0D, "视觉剥夺的持续时间，单位：秒。", "Visual Deprivation duration, in seconds.");
        VALLEY_VISUAL_DEPRIVATION_FADE_IN_TICKS = integer(builder, "travail.visualDeprivation.visualDeprivationFadeInTicks", 30, 0, 100, "视觉剥夺的淡入时间，单位：游戏刻；淡入与淡出时间之和超过总持续时间时，按比例缩短。", "Visual Deprivation fade-in duration, in game ticks. If fade-in and fade-out together exceed the total duration, both are shortened proportionally.");
        VALLEY_VISUAL_DEPRIVATION_FADE_OUT_TICKS = integer(builder, "travail.visualDeprivation.visualDeprivationFadeOutTicks", 30, 0, 100, "视觉剥夺的淡出时间，单位：游戏刻；淡入与淡出时间之和超过总持续时间时，按比例缩短。", "Visual Deprivation fade-out duration, in game ticks. If fade-in and fade-out together exceed the total duration, both are shortened proportionally.");
        VALLEY_STIFF_CHANCE = decimal(builder, "travail.malice.stiffChanceOnAttack", 0.20D, 0.0D, 1.0D, "攻击后获得僵硬效果的概率。", "Chance to gain Stiff after attacking.");
        VALLEY_STIFF_DURATION_SECONDS = decimal(builder, "travail.malice.stiffDurationSeconds", 3.0D, 0.0D, 1_000_000.0D, "僵硬效果的持续时间，单位：秒。", "Stiff duration, in seconds.");
        VALLEY_SWIFTNESS_DURATION_SECONDS = decimal(builder, "travail.witness.swiftnessDurationSeconds", 3.0D, 0.0D, 1_000_000.0D, "幽谷之见证给予的迅捷效果持续时间，单位：秒。", "Duration of Swiftness granted by Deep Valley Witness, in seconds.");
        VALLEY_SWIFTNESS_LEVEL = integer(builder, "travail.witness.swiftnessLevel", 1, 1, 256, "幽谷之见证给予的迅捷效果等级。", "Level of Swiftness granted by Deep Valley Witness.");
        VALLEY_RANDOM_EFFECT_COUNT = integer(builder, "travail.witness.randomEffectCount", 1, 0, 64, "每次成功攻击给予的随机增益效果数量。数量过多且增益冷却过短时，可能增加游戏负担。", "Number of random beneficial effects granted per successful attack. Large counts combined with short buff cooldowns may increase processing load.");
        VALLEY_RANDOM_EFFECT_DURATION_SECONDS = decimal(builder, "travail.witness.randomEffectDurationSeconds", 3.0D, 0.0D, 1_000_000.0D, "随机增益效果的持续时间，单位：秒。", "Duration of random beneficial effects, in seconds.");
        VALLEY_WITNESS_ATTACK_COOLDOWN_SECONDS = decimal(builder, "travail.witness.witnessAttackCooldownSeconds", 3.0D, 0.0D, 1_000_000.0D, "幽谷之见证通过造成伤害触发增益的冷却时间，单位：秒；0表示无冷却。", "Cooldown for buffs triggered by dealing damage with Deep Valley Witness, in seconds; 0 means no cooldown.");
        defineRequirements(builder, TravailAspect.DEEP_VALLEY,
                List.of("minecraft:dripstone_caves|@biome.minecraft.dripstone_caves|3", "minecraft:lush_caves|@biome.minecraft.lush_caves|3", "minecraft:deep_dark|@biome.minecraft.deep_dark|3", "minecraft:dark_forest|@biome.minecraft.dark_forest|1", "minecraft:mushroom_fields|@biome.minecraft.mushroom_fields|1", "minecraft:stony_shore|@biome.minecraft.stony_shore|1"),
                List.of());
        DEEP_VALLEY_POSITIVE_EFFECTS = builder.comment("格式：效果ID|最高等级|权重", "Format: effect_id|max_level|weight").defineListAllowEmpty("travail.witness.positiveEffects", defaultEffects(), TravailConfig::validEffectEntry);


        builder = builders.computeIfAbsent("aspects/underworld.toml", key -> new ForgeConfigSpec.Builder());
        UNDERWORLD_DAMAGE_REDUCTION = decimal(builder, "travail.malice.outgoingDamageReduction", 0.50D, 0.0D, 1.0D, "造成伤害的削减比例；0.5表示削减50%。", "Fractional reduction in damage dealt; 0.5 means a 50% reduction.");
        UNDERWORLD_EXPERIENCE_LEVEL_CAP = integer(builder, "travail.malice.experienceLevelCap", 60, 0, 1_000_000, "冥府之恶意生效时的经验等级上限。", "Maximum experience level while Underworld Malice is active.");
        UNDERWORLD_ANVIL_LEVEL_CAP = integer(builder, "travail.witness.anvilLevelCostCap", 20, 0, 1_000_000, "冥府之见证生效时的铁砧经验等级消耗上限。", "Maximum anvil cost in experience levels while Underworld Witness is active.");
        UNDERWORLD_FORTUNE_BONUS = integer(builder, "travail.witness.fortuneBonus", 6, 0, 1_000_000, "冥府之见证提供的时运等级加成。", "Fortune level bonus granted by Underworld Witness.");
        UNDERWORLD_LOOTING_BONUS = integer(builder, "travail.witness.lootingBonus", 6, 0, 1_000_000, "冥府之见证提供的抢夺等级加成。", "Looting level bonus granted by Underworld Witness.");
        FISH_ENTITY_CHANCE = decimal(builder, "travail.witness.fishing.fishingEntityChance", 0.30D, 0.0D, 1.0D, "钓出生物的概率。与钓出特殊物品的概率之和超过100%时，两项按原比例缩放至合计100%。", "Chance to fish up a creature. If this and the special-item chance total more than 100%, both are scaled proportionally to total 100%.");
        FISH_SPECIAL_CHANCE = decimal(builder, "travail.witness.fishing.fishingSpecialItemChance", 0.50D, 0.0D, 1.0D, "钓出特殊物品的概率。与钓出生物的概率之和超过100%时，两项按原比例缩放至合计100%。", "Chance to fish up a special item. If this and the creature chance total more than 100%, both are scaled proportionally to total 100%.");
        defineRequirements(builder, TravailAspect.UNDERWORLD,
                List.of("minecraft:nether_wastes|@biome.minecraft.nether_wastes|3", "minecraft:soul_sand_valley|@biome.minecraft.soul_sand_valley|3", "minecraft:warped_forest|@biome.minecraft.warped_forest|3", "minecraft:basalt_deltas|@biome.minecraft.basalt_deltas|1", "minecraft:crimson_forest|@biome.minecraft.crimson_forest|1", "minecraft:wooded_badlands|@biome.minecraft.wooded_badlands|1"),
                List.of());
        FISH_SPECIAL_ITEMS = builder.comment("格式：物品ID|权重|最小数量|最大数量", "Format: item_id|weight|min_count|max_count", "不可堆叠的物品每件都会生成一个掉落实体；一次发放过多可能造成卡顿。", "Each non-stackable item creates a separate dropped item entity. Delivering too many at once may cause lag.").defineListAllowEmpty("travail.witness.fishing.fishingSpecialItems", defaultRewardItems(), TravailConfig::validRewardEntry);
        FISH_ENTITIES = builder.comment("格式：实体ID|权重", "Format: entity_id|weight").defineListAllowEmpty("travail.witness.fishing.fishingEntities", List.of(
                "minecraft:pig|10", "minecraft:cow|10", "minecraft:sheep|10", "minecraft:pufferfish|5", "minecraft:wandering_trader|5",
                "minecraft:cat|5", "minecraft:fox|1", "minecraft:axolotl|1", "minecraft:turtle|1"), TravailConfig::validWeightedEntry);


        builder = builders.computeIfAbsent("aspects/boundless.toml", key -> new ForgeConfigSpec.Builder());
        BOUNDLESS_HEIGHT_THRESHOLD = decimal(builder, "travail.malice.heightThreshold", 100.0D, 0.0D, 1_000_000.0D, "玩家Y坐标高于此值时，降低移动速度。\nReduce movement speed when the player's Y coordinate is above this value.", "Y-coordinate threshold above which the speed penalty applies.");
        BOUNDLESS_SPEED_REDUCTION = decimal(builder, "travail.malice.speedReduction", 0.80D, 0.0D, 1.0D, "高空速度削减比例；0.8表示削减80%。", "Fractional movement speed reduction at high altitude; 0.8 means an 80% reduction.");
        BOUNDLESS_EXTRA_CURIO_SLOTS = integer(builder, "travail.witness.extraGenericCurioSlots", 1, 0, 64, "无垠之见证提供的通用Curios饰品槽数量。", "Number of generic Curios accessory slots granted by Boundless Witness.");
        PHANTOM_RANGE = integer(builder, "travail.witness.phantom.phantomKillRange", 32, 0, 128, "自动击杀幻翼的检测半径，单位：方块。范围过大或幻翼过多时，可能增加游戏负担。", "Detection radius for automatic phantom kills, in blocks. Large radii or large numbers of phantoms may increase processing load.");
        PHANTOM_CHECK_INTERVAL_SECONDS = decimal(builder, "travail.witness.phantom.phantomCheckIntervalSeconds", 1.0D, 1.0D, 1_000_000.0D, "幻翼检测间隔，单位：秒，最短1秒。", "Phantom detection interval, in seconds; minimum 1 second.");
        PHANTOM_MAX_KILLS = integer(builder, "travail.witness.phantom.phantomMaxKillsPerScan", 16, 1, 1024, "每名玩家每轮最多尝试击杀的幻翼数，包括伤害被取消的尝试。", "Maximum phantom kill attempts per player per scan, including attempts whose damage is canceled.");
        defineRequirements(builder, TravailAspect.BOUNDLESS,
                List.of("minecraft:end_midlands|@biome.minecraft.end_midlands|3", "minecraft:end_highlands|@biome.minecraft.end_highlands|3", "minecraft:end_barrens|@biome.minecraft.end_barrens|3", "minecraft:the_end|@biome.minecraft.the_end|1", "minecraft:small_end_islands|@biome.minecraft.small_end_islands|1", "minecraft:stony_peaks|@biome.minecraft.stony_peaks|1"),
                List.of());
        PHANTOM_DROPS = builder.comment("格式：物品ID|权重|最小数量|最大数量", "Format: item_id|weight|min_count|max_count", "不可堆叠的物品每件都会生成一个掉落实体；多只幻翼同时产生大量奖励时可能造成卡顿。", "Each non-stackable item creates a separate dropped item entity. Large rewards from multiple phantoms at once may cause lag.").defineListAllowEmpty("travail.witness.phantom.phantomDrops", defaultRewardItems(), TravailConfig::validRewardEntry);


        builder = builders.computeIfAbsent("aspects/deep_valley.toml", key -> new ForgeConfigSpec.Builder());
        VISUAL_DARKENING = builder.comment("世界画面的整体变暗比例；0表示不变暗，1表示全黑。", "Overall world darkening fraction; 0 leaves the view unchanged, and 1 makes it completely black.").defineInRange("travail.visualDeprivation.worldDarkening", 0.65D, 0.0D, 1.0D);
        VISUAL_OPACITY = builder.comment("屏幕外围的遮蔽强度；0表示关闭外围遮蔽。", "Strength of the peripheral screen mask; 0 disables it.").defineInRange("travail.visualDeprivation.peripheralOpacity", 0.98D, 0.0D, 1.0D);
        VISUAL_RADIUS = builder.comment("屏幕中央清晰区域的椭圆半径；1表示横向和纵向均延伸至屏幕边缘，不改变实际视野角度（FOV）。", "Elliptical radius of the clear central area. 1 reaches the screen edges horizontally and vertically. Does not change the actual field of view (FOV).").defineInRange("travail.visualDeprivation.clearRadius", 0.45D, 0.0D, 2.0D);
        VISUAL_SOFTNESS = builder.comment("外围遮蔽边缘的过渡宽度。", "Transition width at the edge of the peripheral mask.").defineInRange("travail.visualDeprivation.edgeSoftness", 0.45D, 0.01D, 2.0D);
        VISUAL_DISTANCE_START = builder.comment("距离雾的起始距离，单位：方块；自动限制为不超过结束距离。", "Distance at which the distance veil starts, in blocks, automatically capped at its end distance.").defineInRange("travail.visualDeprivation.distanceVeilStart", 1.25D, 0.0D, 1024.0D);
        VISUAL_DISTANCE_END = builder.comment("距离雾的结束距离，单位：方块；使用光影包时，显示效果可能不同。", "Distance at which the distance veil ends, in blocks. Shader packs may change its appearance.").defineInRange("travail.visualDeprivation.distanceVeilEnd", 5.0D, 0.1D, 4096.0D);
        VISUAL_PULSE_PERIOD = builder.comment("视觉强度的波动周期，单位：秒。", "Period of visual intensity fluctuations, in seconds.").defineInRange("travail.visualDeprivation.pulsePeriodSeconds", 4.0D, 0.1D, 120.0D);
        VISUAL_PULSE_DEPTH = builder.comment("视觉强度的波动幅度；0表示不波动，0.2表示在80%至100%强度之间波动。", "Amplitude of visual intensity fluctuations; 0 disables fluctuations, and 0.2 varies intensity between 80% and 100%.").defineInRange("travail.visualDeprivation.pulseDepth", 0.0D, 0.0D, 1.0D);
        VISUAL_DISTANCE_ENABLED = builder.comment("启用距离雾；关闭后仍保留屏幕遮蔽。", "Enable the distance veil. Disabling it preserves screen masking.").define("travail.visualDeprivation.distanceVeilEnabled", true);
        VISUAL_PULSE_RADIUS = builder.comment("视觉强度减弱时，同步扩大屏幕中央的清晰区域。", "Expand the clear central area as visual intensity decreases.").define("travail.visualDeprivation.pulseAffectsRadius", false);
        VISUAL_REFRESH_TICKS = builder.comment("淡出期间再次触发时，恢复遮蔽强度所需的游戏刻数。", "Game ticks needed to restore mask intensity when the effect is triggered again during fade-out.").defineInRange("travail.visualDeprivation.refreshBlendTicks", 5, 0, 100);
        VISUAL_CLEAR_TICKS = builder.comment("通过饮用牛奶等方式主动解除视觉剥夺后，遮蔽画面逐渐消退所需的时间，单位：游戏刻。视觉剥夺状态本身会立即解除。", "Time for the visual mask to fade after Visual Deprivation is actively removed, for example by drinking milk, in game ticks. The status effect itself is removed immediately.").defineInRange("travail.visualDeprivation.clearFadeOutTicks", 4, 0, 100);


        BoundlessItemsConfig.define(builders.get("aspects/boundless.toml"));
        RainConfig.define(builders.get("aspects/abyss.toml"));
        UnderworldItemsConfig.define(builders.get("aspects/underworld.toml"));
        AzraelConfig.define(builders.get("aspects/deep_valley.toml"));
        com.thelongtravail.valley.SwordLanternConfig.define(builders.get("aspects/deep_valley.toml"));
        FlourishingItemsConfig.define(builders.get("aspects/flourishing.toml"));
        FarReachItemsConfig.define(builders.get("aspects/far_reach.toml"));
        Map<String, ForgeConfigSpec> specs = new java.util.LinkedHashMap<>();
        builders.forEach((file, value) -> {
            ConfigFilesDescription.describe(value, file);
            specs.put(file, value.build());
        });
        SPECS = java.util.Collections.unmodifiableMap(specs);
    }

    private static void defineRequirements(ForgeConfigSpec.Builder builder, TravailAspect aspect, List<String> biomes, List<String> structures) {
        BIOME_POOLS.put(aspect, builder.comment(
                "格式：群系ID|显示名称|权重。权重填正整数，数值越大，抽中概率越高。",
                "Format: biome_id|display_name|weight. Weights must be positive integers; larger values increase the chance of selection.",
                "显示名称可以填写自定义文字，也可以用谜语作为探索提示。例如，填写 minecraft:plains|高峰出云|3 后，实际目标仍为平原，但苦旅页面会将其显示为“高峰出云”。",
                "Display names may be custom text or riddles used as exploration hints. For example, minecraft:plains|Peaks Above the Clouds|3 still targets plains, but the diary displays it as Peaks Above the Clouds.",
                "若要让名称随游戏语言切换，请填写 @翻译键，例如：minecraft:plains|@biome.minecraft.plains|3。翻译键 biome.minecraft.plains 须有对应译文，找不到译文时将显示群系ID。",
                "To localize the name, use @translation_key, for example minecraft:plains|@biome.minecraft.plains|3. The key biome.minecraft.plains must have a translation; if none is found, the biome ID is displayed.")
                .defineListAllowEmpty("travail.requirements.biomes", biomes, TravailConfig::validRequirementEntry));
        STRUCTURE_POOLS.put(aspect, builder.comment(
                "格式：结构ID|显示名称|权重。权重填正整数，数值越大，抽中概率越高。",
                "Format: structure_id|display_name|weight. Weights must be positive integers; larger values increase the chance of selection.",
                "显示名称可以填写自定义文字，也可以用谜语作为探索提示。例如，填写 example:stronghold|末路之门|3 后，实际目标仍为 example:stronghold，但苦旅页面会将其显示为“末路之门”。",
                "Display names may be custom text or riddles used as exploration hints. For example, example:stronghold|Gate at the Journey's End|3 still targets example:stronghold, but the diary displays it as Gate at the Journey's End.",
                "若要让名称随游戏语言切换，请填写 @翻译键，例如：example:stronghold|@structure.example.stronghold|3。翻译键 structure.example.stronghold 需要在模组或资源包的语言文件中提供对应译文，找不到译文时将显示结构ID。",
                "To localize the name, use @translation_key, for example example:stronghold|@structure.example.stronghold|3. Provide a translation for structure.example.stronghold in a mod or resource-pack language file; if none is found, the structure ID is displayed.")
                .defineListAllowEmpty("travail.requirements.structures", structures, TravailConfig::validRequirementEntry));
    }

    private static List<String> defaultEffects() {
        return List.of("minecraft:jump_boost|5|5", "minecraft:regeneration|5|3", "minecraft:resistance|4|2", "minecraft:haste|3|2", "minecraft:strength|3|2");
    }

    private static List<String> defaultRewardItems() {
        return List.of("minecraft:apple|15|1|5", "minecraft:emerald|15|1|5", "minecraft:iron_ingot|15|1|3", "minecraft:gold_ingot|10|1|3", "minecraft:diamond|6|1|3",
                "minecraft:enchanted_golden_apple|3|1|1", "minecraft:netherite_ingot|2|1|1", "minecraft:nether_star|1|1|1", "minecraft:dragon_egg|1|1|1");
    }

    private static ForgeConfigSpec.IntValue integer(ForgeConfigSpec.Builder builder, String key, int value, int min, int max,
                                                     String chineseComment, String englishComment) {
        return builder.comment(chineseComment, englishComment).defineInRange(key, value, min, max);
    }

    private static ForgeConfigSpec.DoubleValue decimal(ForgeConfigSpec.Builder builder, String key, double value, double min, double max,
                                                        String chineseComment, String englishComment) {
        return builder.comment(chineseComment, englishComment).defineInRange(key, value, min, max);
    }

    private static boolean validRequirementEntry(Object value) {
        return value instanceof String string && string.split("\\|", -1).length >= 2;
    }

    private static boolean validWeightedEntry(Object value) {
        return value instanceof String string && string.split("\\|", -1).length >= 2;
    }

    private static boolean validEffectEntry(Object value) {
        return value instanceof String string && string.split("\\|", -1).length >= 3;
    }

    private static boolean validRewardEntry(Object value) {
        return value instanceof String string && string.split("\\|", -1).length >= 4;
    }

    private TravailConfig() {}
}

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
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue GIVE_STARTER_ITEM;
    public static final ForgeConfigSpec.BooleanValue MUTE_BENEFICIAL_EFFECT_GAIN_SOUNDS;
    public static final Map<TravailAspect, ForgeConfigSpec.ConfigValue<List<? extends String>>> BIOME_POOLS = new EnumMap<>(TravailAspect.class);
    public static final Map<TravailAspect, ForgeConfigSpec.ConfigValue<List<? extends String>>> STRUCTURE_POOLS = new EnumMap<>(TravailAspect.class);

    public static final ForgeConfigSpec.IntValue BIOME_DRAW_COUNT;
    public static final ForgeConfigSpec.IntValue STRUCTURE_DRAW_COUNT;
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
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("general");
        MUTE_BENEFICIAL_EFFECT_GAIN_SOUNDS = builder.comment(
                "佩戴苦旅且拥有穷遐或幽谷见证时，静默任意来源正面药水效果的获得提示音（Sounds兼容）；不屏蔽饮用、装备操作音或负面效果提示。关闭后保留原有苦旅效果精确静音规则。",
                "Mute Sounds beneficial-effect gain notifications from any source while wearing a diary with Far Reach or Deep Valley Witness. Disabling preserves existing precise Travail sound rules.")
                .define("muteBeneficialEffectGainSounds", true);
        GIVE_STARTER_ITEM = builder.comment(
                "玩家首次进入世界时是否赠送一个“苦旅”；每名玩家最多赠送一次。",
                "Whether to give one The Long Travail when a player first joins a world; granted at most once per player.")
                .define("giveStarterItem", true);
        BIOME_DRAW_COUNT = integer(builder, "biomeDrawCount", 3, 0, 100, "每种恶意抽取的不同群系数量。", "Unique biomes drawn for each aspect.");
        STRUCTURE_DRAW_COUNT = integer(builder, "structureDrawCount", 0, 0, 100, "每种恶意抽取的不同结构数量。", "Unique structures drawn for each aspect.");
        RECEIVED_TRIGGER = builder.comment("受击恶意触发时机：ATTACK_ATTEMPT 保持受攻击即触发；HEALTH_LOSS 仅实际生命值下降后触发，不含吸收生命。")
                .defineEnum("receivedMaliceTrigger", ReceivedTrigger.ATTACK_ATTEMPT);
        RECEIVED_COOLDOWN = integer(builder, "receivedMaliceCooldownTicks", 0, 0, 1_000_000, "受击恶意共享冷却刻数；0不限制，不影响伤害和攻击侧效果。", "Shared received-malice cooldown; zero disables it.");
        MAX_AUTOMATIC_EFFECT_ACTIONS = integer(builder, "maxAutomaticEffectActionsPerPlayerPerSecond", 0, 0, 1_000_000,
                "每名玩家任意连续20刻内，远望见证净化/增益与幽谷见证攻击增益的共享尝试上限；失败或被取消也计数，超额跳过，不补发。0不限制；需要限流时可从100开始调整。不影响受击恶意、视觉剥夺及幽谷的黑暗/失明免疫。",
                "Shared rolling-20-tick attempt cap for Far Reach cleanse/grants and Deep Valley witness buffs; 0 disables. Failed attempts count; excess is skipped.");
        RECEIVED_EXCLUSIONS = builder.comment("不触发受击恶意的伤害类型ID；不改变伤害本身。")
                .defineListAllowEmpty("receivedMaliceExcludedDamageTypes", List.of(), value -> value instanceof String id && net.minecraft.resources.ResourceLocation.tryParse(id) != null);
        builder.pop();

        builder.push("rewards");
        REWARD_ENTITIES_PER_TICK = integer(builder, "rewardMaxItemEntitiesPerTick", 32, 1, 1024, "全服每刻本模组额外奖励掉落实体生成预算；不限制原版掉落。", "Global bonus item entity budget per tick.");
        REWARD_QUEUE_CAPACITY = integer(builder, "rewardQueueCapacity", 1024, 1, 16384, "待发奖励记录上限；队列满时暂停自动击杀，钓鱼额外物品奖励跳过。已有记录不会因调低上限而删除。", "Pending reward record limit; full queue pauses phantom kills and skips fishing bonuses.");
        REWARD_OVERFLOW = builder.comment("DEFER 分批发放全部奖励；LIMIT 将每次奖励限制为 rewardMaxEntitiesPerReward 个堆叠，其余不发放。两者都遵守全服预算。")
                .defineEnum("rewardOverflowPolicy", RewardOverflow.DEFER);
        REWARD_LIMIT = integer(builder, "rewardMaxEntitiesPerReward", 64, 1, 4096, "LIMIT模式每次奖励的最大堆叠数。", "Maximum stacks per reward in LIMIT mode.");
        builder.pop();

        builder.push("flourishing");
        MAX_HEALING_BONUS = integer(builder, "maxHealingBonus", 0, 0, 1_000_000, "繁茂之见证的最大叠加恢复倍率；例如100代表+10000%，0代表无上限。", "Maximum additive healing multiplier for Flourishing Witness; for example, 100 means +10000%, and 0 means unlimited.");
        FLOURISHING_HEALING_REDUCTION = decimal(builder, "healingReduction", 0.80D, 0.0D, 1.0D, "恢复效果削减比例；0.8代表削减80%。", "Healing reduction; 0.8 means healing is reduced by 80%.");
        FLOURISHING_HEALTH_THRESHOLD = decimal(builder, "doubleDamageHealthThreshold", 0.50D, 0.0D, 1.0D, "触发受到伤害倍率的生命值比例阈值。", "Health fraction above which incoming damage is multiplied.");
        FLOURISHING_DAMAGE_MULTIPLIER = decimal(builder, "incomingDamageMultiplier", 2.0D, 0.0D, 1_000_000.0D, "高于生命值阈值时受到的伤害倍率。", "Incoming damage multiplier above the health threshold.");
        FLOURISHING_BONUS_PER_BIOME = decimal(builder, "healingBonusPerBiome", 0.05D, 0.0D, 1_000_000.0D, "每发现一个群系增加的恢复倍率；0.05代表+5%。", "Additive healing multiplier per discovered biome; 0.05 means +5%.");
        defineRequirements(builder, TravailAspect.FLOURISHING,
                List.of("minecraft:plains|平原|3", "minecraft:forest|森林|3", "minecraft:taiga|针叶林|3", "minecraft:swamp|沼泽|1", "minecraft:jungle|丛林|1", "minecraft:savanna|热带草原|1"),
                List.of());
        builder.pop();

        builder.push("abyss");
        ABYSS_FLUID_DAMAGE = decimal(builder, "fluidTrueDamage", 1.0D, 0.0D, 1_000_000.0D, "每次流体伤害造成的真实伤害值。", "True damage dealt on every fluid interval.");
        ABYSS_FLUID_INTERVAL_SECONDS = decimal(builder, "fluidDamageIntervalSeconds", 1.0D, 0.0D, 1_000_000.0D, "流体伤害间隔秒数；0代表每游戏刻触发。性能提示：极短间隔会增加伤害事件与诅咒联动开销，多人时更明显。", "Seconds between fluid damage; 0 means every tick. Performance: short intervals increase damage events and curse interactions, especially with many players.");
        ABYSS_MINING_REDUCTION = decimal(builder, "miningSpeedReduction", 0.50D, 0.0D, 1.0D, "挖掘速度削减比例；0.5代表削减50%。", "Mining speed reduction; 0.5 means mining speed is reduced by 50%.");
        ABYSS_ENVIRONMENT_MULTIPLIER = decimal(builder, "environmentDamageMultiplier", 10.0D, 0.0D, 1_000_000.0D, "窒息、挤压、溺水与冻结伤害倍率；见证免疫同一组伤害。", "Multiplier for in-wall, cramming, drowning and freezing damage; the witness negates the same set.");
        ABYSS_CANCEL_CHANCE = decimal(builder, "fluidDamageCancelChance", 0.50D, 0.0D, 1.0D, "归墟之见证在流体中取消伤害的概率。", "Abyss Witness chance to cancel damage while in fluid.");
        FLUID_EROSION_UNAVOIDABLE = builder.comment(
                "流体真实伤害是否穿透 LivingAttackEvent、LivingHurtEvent、LivingDamageEvent 的取消及伤害归零，并保护苦旅自身倍率计算后的伤害下限。",
                "始终尊重苦旅自身免伤和输出限制；不绕过更早的实体无敌检查、图腾或死亡事件。关闭后仍保留伤害类型原有的护甲等穿透标签。",
                "Protect fluid true damage against cancellation/zeroing at Attack, Hurt and Damage event boundaries, including Travail's own multipliers.",
                "Respects Travail immunity/output restrictions; does not bypass earlier entity checks, totems or death events. Damage-type bypass tags remain when disabled.")
                .define("fluidErosionUnavoidable", true);
        FLUID_PROTECTION = builder.comment("ENFORCED 保持强制伤害；RESPECT_CANCELLATION 尊重事件取消但保护未取消伤害的下限；STANDARD 接受取消与减伤。",
                "旧开关 fluidErosionUnavoidable=false 时始终采用 STANDARD；所有模式保留流体伤害原有穿甲/吸收穿透规则。")
                .defineEnum("fluidErosionProtectionMode", FluidProtection.ENFORCED);
        defineRequirements(builder, TravailAspect.ABYSS,
                List.of("minecraft:deep_ocean|暖水深海（使用深海实现）|3", "minecraft:warm_ocean|暖水海洋|3", "minecraft:lukewarm_ocean|温水海洋|2", "minecraft:deep_lukewarm_ocean|温水深海|2", "minecraft:cold_ocean|冷水海洋|1", "minecraft:deep_cold_ocean|冷水深海|1"),
                List.of());
        builder.pop();

        builder.push("farReach");
        FAR_ALL_CLEAR_CHANCE = decimal(builder, "clearAllBeneficialChanceOnHit", 0.10D, 0.0D, 1.0D, "受击时清除全部正面效果的概率。", "Chance to clear all beneficial effects when hit.");
        FAR_ATTACK_CLEAR_CHANCE = decimal(builder, "clearBeneficialChanceOnAttack", 0.50D, 0.0D, 1.0D, "攻击后清除一个正面效果的概率。", "Chance to clear one beneficial effect after attacking.");
        FAR_ATTACK_COOLDOWN_SECONDS = decimal(builder, "attackClearCooldownSeconds", 5.0D, 0.0D, 1_000_000.0D, "攻击触发清除的冷却秒数；0代表无冷却。", "Attack-side clear cooldown in seconds; 0 disables the cooldown.");
        FAR_WITNESS_LEVEL_BONUS = integer(builder, "beneficialLevelBonus", 1, 0, 255, "新获得正面效果时增加的等级数。", "Levels added to newly received beneficial effects.");
        FAR_WITNESS_LEVEL_BLACKLIST = builder.comment("穷遐之见证等级提升黑名单，填写完整效果ID，例如 minecraft:night_vision。只跳过等级提升，不阻止获得效果，也不改变随机效果池。空列表代表不排除任何效果。",
                "Effect IDs excluded from the Far Reach Witness level bonus; does not prevent granting effects. Example: [\"minecraft:night_vision\", \"minecraft:water_breathing\"].")
                .defineListAllowEmpty("beneficialLevelBonusBlacklist", List.of("minecraft:night_vision", "minecraft:water_breathing"), value -> value instanceof String);
        FAR_WITNESS_FALLBACK_MAX_LEVEL = integer(builder, "unlistedEffectMaxLevel", 5, 1, 256, "两个正面效果池均未列出的效果所采用的最高等级。", "Maximum level for beneficial effects absent from both pools.");
        FAR_WITNESS_INTERVAL_SECONDS = decimal(builder, "witnessIntervalSeconds", 5.0D, 0.0D, 1_000_000.0D, "清除负面效果或给予正面效果的循环间隔秒数；0代表每游戏刻触发。性能提示：极短间隔与较大动作数同时使用会显著增加效果事件开销。", "Seconds between cleanse/grant cycles; 0 means every tick. Performance: short intervals combined with high action counts increase effect-event processing.");
        FAR_WITNESS_ACTION_COUNT = integer(builder, "cleanseOrGrantCount", 2, 0, 64, "每轮清除负面效果的次数；未使用的次数会用于给予正面效果。性能提示：64次配合每刻触发时，每名玩家每秒最多尝试1280次给予效果，此外还有净化尝试开销（按20 TPS计算）。", "Cleanse actions per cycle; unused actions grant positive effects. Performance: 64 actions every tick allow up to 1280 effect-add attempts per player per second at 20 TPS, in addition to removal attempts.");
        FAR_WITNESS_POSITIVE_DURATION_SECONDS = decimal(builder, "grantedEffectDurationSeconds", 5.0D, 0.0D, 1_000_000.0D, "随机给予的正面效果持续秒数。", "Duration in seconds of randomly granted beneficial effects.");
        defineRequirements(builder, TravailAspect.FAR_REACH,
                List.of("minecraft:snowy_plains|雪原|3", "minecraft:desert|沙漠|3", "minecraft:badlands|恶地|3", "minecraft:windswept_hills|风袭丘陵|1", "minecraft:snowy_taiga|积雪针叶林|1", "minecraft:ice_spikes|冰刺之地|1"),
                List.of());
        FAR_REACH_POSITIVE_EFFECTS = builder.comment("格式：效果ID|最高等级|权重", "Format: effect_id|max_level|weight").defineListAllowEmpty("positiveEffects", defaultEffects(), TravailConfig::validEffectEntry);
        builder.pop();

        builder.push("deepValley");
        VALLEY_VISUAL_DEPRIVATION_CHANCE = decimal(builder, "visualDeprivationChanceOnHit", 0.50D, 0.0D, 1.0D, "受击时获得视觉剥夺的概率。", "Chance to gain Visual Deprivation when hit.");
        VALLEY_VISUAL_DEPRIVATION_DURATION_SECONDS = decimal(builder, "visualDeprivationDurationSeconds", 5.0D, 0.0D, 1_000_000.0D, "视觉剥夺持续秒数。", "Visual Deprivation duration in seconds.");
        VALLEY_VISUAL_DEPRIVATION_FADE_IN_TICKS = integer(builder, "visualDeprivationFadeInTicks", 30, 0, 100, "视觉剥夺渐入刻数；两段渐变超过持续时间时按比例压缩。", "Fade-in ticks; both fades are proportionally shortened when necessary.");
        VALLEY_VISUAL_DEPRIVATION_FADE_OUT_TICKS = integer(builder, "visualDeprivationFadeOutTicks", 30, 0, 100, "视觉剥夺渐出刻数；两段渐变超过持续时间时按比例压缩。", "Fade-out ticks; both fades are proportionally shortened when necessary.");
        VALLEY_STIFF_CHANCE = decimal(builder, "stiffChanceOnAttack", 0.20D, 0.0D, 1.0D, "攻击后获得僵硬效果的概率。", "Chance to gain Stiff after attacking.");
        VALLEY_STIFF_DURATION_SECONDS = decimal(builder, "stiffDurationSeconds", 3.0D, 0.0D, 1_000_000.0D, "僵硬效果持续秒数。", "Stiff duration in seconds.");
        VALLEY_SWIFTNESS_DURATION_SECONDS = decimal(builder, "swiftnessDurationSeconds", 3.0D, 0.0D, 1_000_000.0D, "幽谷之见证给予的迅捷效果持续秒数。", "Duration in seconds of Swiftness granted by Deep Valley Witness.");
        VALLEY_SWIFTNESS_LEVEL = integer(builder, "swiftnessLevel", 1, 1, 256, "幽谷之见证给予的迅捷效果等级。", "Swiftness level granted by Deep Valley Witness.");
        VALLEY_RANDOM_EFFECT_COUNT = integer(builder, "randomEffectCount", 1, 0, 64, "每次成功攻击给予的随机正面效果数量。性能提示：数量较大且攻击冷却很短时，会增加效果事件与同步开销。", "Random beneficial effects granted per successful attack. Performance: high counts combined with short cooldowns increase effect events and synchronization work.");
        VALLEY_RANDOM_EFFECT_DURATION_SECONDS = decimal(builder, "randomEffectDurationSeconds", 3.0D, 0.0D, 1_000_000.0D, "随机正面效果持续秒数。", "Random beneficial effect duration in seconds.");
        VALLEY_WITNESS_ATTACK_COOLDOWN_SECONDS = decimal(builder, "witnessAttackCooldownSeconds", 3.0D, 0.0D, 1_000_000.0D, "幽谷之见证由造成伤害触发增益的冷却秒数；0代表无冷却。", "Cooldown in seconds for Deep Valley Witness buffs triggered by dealing damage; 0 means no cooldown.");
        defineRequirements(builder, TravailAspect.DEEP_VALLEY,
                List.of("minecraft:dripstone_caves|溶洞|3", "minecraft:lush_caves|繁茂洞穴|3", "minecraft:deep_dark|深暗之域|3", "minecraft:dark_forest|黑森林|1", "minecraft:mushroom_fields|蘑菇岛|1", "minecraft:stony_shore|石岸|1"),
                List.of());
        DEEP_VALLEY_POSITIVE_EFFECTS = builder.comment("格式：效果ID|最高等级|权重", "Format: effect_id|max_level|weight").defineListAllowEmpty("positiveEffects", defaultEffects(), TravailConfig::validEffectEntry);
        builder.pop();

        builder.push("underworld");
        UNDERWORLD_DAMAGE_REDUCTION = decimal(builder, "outgoingDamageReduction", 0.50D, 0.0D, 1.0D, "造成伤害的削减比例；0.5代表削减50%。", "Outgoing damage reduction; 0.5 means outgoing damage is reduced by 50%.");
        UNDERWORLD_EXPERIENCE_LEVEL_CAP = integer(builder, "experienceLevelCap", 60, 0, 1_000_000, "冥府之恶意生效时的经验等级上限。", "Maximum experience level while the malice is active.");
        UNDERWORLD_ANVIL_LEVEL_CAP = integer(builder, "anvilLevelCostCap", 20, 0, 1_000_000, "冥府之见证生效时的铁砧经验等级消耗上限。", "Maximum anvil level cost while the witness is active.");
        UNDERWORLD_FORTUNE_BONUS = integer(builder, "fortuneBonus", 6, 0, 1_000_000, "冥府之见证提供的时运等级加成。", "Fortune level bonus granted by the witness.");
        UNDERWORLD_LOOTING_BONUS = integer(builder, "lootingBonus", 6, 0, 1_000_000, "冥府之见证提供的抢夺等级加成。", "Looting level bonus granted by the witness.");
        FISH_ENTITY_CHANCE = decimal(builder, "fishingEntityChance", 0.30D, 0.0D, 1.0D, "钓出生物的概率；与特殊物品概率之和大于1时按比例归一化。", "Chance to fish an entity; normalized proportionally when combined with the special-item chance above 1.");
        FISH_SPECIAL_CHANCE = decimal(builder, "fishingSpecialItemChance", 0.50D, 0.0D, 1.0D, "钓出特殊物品的概率；与生物概率之和大于1时按比例归一化。", "Chance to fish a special item; normalized proportionally when combined with the entity chance above 1.");
        defineRequirements(builder, TravailAspect.UNDERWORLD,
                List.of("minecraft:nether_wastes|下界荒地|3", "minecraft:soul_sand_valley|灵魂沙峡谷|3", "minecraft:warped_forest|诡异森林|3", "minecraft:basalt_deltas|玄武岩三角洲|1", "minecraft:crimson_forest|绯红森林|1", "minecraft:wooded_badlands|繁茂恶地|1"),
                List.of());
        FISH_SPECIAL_ITEMS = builder.comment("格式：物品ID|权重|最小数量|最大数量", "Format: item_id|weight|min_count|max_count", "性能提示：大量不可堆叠物品每件占一个掉落实体，4096件可生成4096个实体；集中触发可能造成卡顿。", "Performance: each non-stackable item requires one dropped entity; 4096 items can create 4096 entities. Bursts may cause lag.").defineListAllowEmpty("fishingSpecialItems", defaultRewardItems(), TravailConfig::validRewardEntry);
        FISH_ENTITIES = builder.comment("格式：实体ID|权重", "Format: entity_id|weight").defineListAllowEmpty("fishingEntities", List.of(
                "minecraft:pig|10", "minecraft:cow|10", "minecraft:sheep|10", "minecraft:pufferfish|5", "minecraft:wandering_trader|5",
                "minecraft:cat|5", "minecraft:fox|1", "minecraft:axolotl|1", "minecraft:turtle|1"), TravailConfig::validWeightedEntry);
        builder.pop();

        builder.push("boundless");
        BOUNDLESS_HEIGHT_THRESHOLD = decimal(builder, "heightThreshold", 100.0D, 0.0D, 1_000_000.0D, "速度惩罚生效的Y坐标阈值。", "Y-coordinate threshold above which the speed penalty applies.");
        BOUNDLESS_SPEED_REDUCTION = decimal(builder, "speedReduction", 0.80D, 0.0D, 1.0D, "高空速度削减比例；0.8代表削减80%。", "High-altitude speed reduction; 0.8 means speed is reduced by 80%.");
        BOUNDLESS_EXTRA_CURIO_SLOTS = integer(builder, "extraGenericCurioSlots", 1, 0, 64, "无垠之见证提供的通用Curios饰品槽数量。", "Generic Curios slots granted by Boundless Witness.");
        PHANTOM_RANGE = integer(builder, "phantomKillRange", 32, 0, 128, "自动处死幻翼的检测半径。性能提示：较大半径会增加附近实体查询开销；幻翼密集时还会集中产生掉落。", "Detection radius for automatically killing phantoms. Performance: larger radii increase entity-query work; dense phantom groups can create bursts of drops.");
        PHANTOM_CHECK_INTERVAL_SECONDS = decimal(builder, "phantomCheckIntervalSeconds", 1.0D, 1.0D, 1_000_000.0D, "幻翼检测间隔秒数，最少1秒。", "Seconds between phantom scans; minimum 1 second.");
        PHANTOM_MAX_KILLS = integer(builder, "phantomMaxKillsPerScan", 16, 1, 1024, "每名玩家每轮最多尝试处死的幻翼数，包括伤害被取消的尝试。", "Maximum phantom kill attempts per player scan.");
        defineRequirements(builder, TravailAspect.BOUNDLESS,
                List.of("minecraft:end_midlands|末地内陆|3", "minecraft:end_highlands|末地高地|3", "minecraft:end_barrens|末地荒地|3", "minecraft:the_end|末地|1", "minecraft:small_end_islands|末地小型岛屿|1", "minecraft:stony_peaks|裸岩山峰|1"),
                List.of());
        PHANTOM_DROPS = builder.comment("格式：物品ID|权重|最小数量|最大数量", "Format: item_id|weight|min_count|max_count", "性能提示：大量不可堆叠物品每件占一个掉落实体，4096件可生成4096个实体；多只幻翼同时死亡会进一步放大负载。", "Performance: each non-stackable item requires one dropped entity; 4096 items can create 4096 entities. Simultaneous phantom deaths multiply the load.").defineListAllowEmpty("phantomDrops", defaultRewardItems(), TravailConfig::validRewardEntry);
        builder.pop();

        builder.push("visualDeprivation");
        VISUAL_DARKENING = builder.comment("世界整体变暗比例，0不变暗，1全黑。").defineInRange("worldDarkening", 0.65D, 0.0D, 1.0D);
        VISUAL_OPACITY = builder.comment("外围附加遮蔽比例，0关闭外围遮蔽。").defineInRange("peripheralOpacity", 0.98D, 0.0D, 1.0D);
        VISUAL_RADIUS = builder.comment("中心清晰椭圆半径，1到达屏幕横纵边缘；不改变真实FOV。").defineInRange("clearRadius", 0.45D, 0.0D, 2.0D);
        VISUAL_SOFTNESS = builder.comment("外围遮蔽的柔和过渡宽度。").defineInRange("edgeSoftness", 0.45D, 0.01D, 2.0D);
        VISUAL_DISTANCE_START = builder.comment("距离雾开始的方块距离，自动限制在结束距离以内。").defineInRange("distanceVeilStart", 1.25D, 0.0D, 1024.0D);
        VISUAL_DISTANCE_END = builder.comment("距离雾结束的方块距离，光影包可能采用自己的雾算法。").defineInRange("distanceVeilEnd", 5.0D, 0.1D, 4096.0D);
        VISUAL_PULSE_PERIOD = builder.comment("独立波动周期，单位秒。").defineInRange("pulsePeriodSeconds", 4.0D, 0.1D, 120.0D);
        VISUAL_PULSE_DEPTH = builder.comment("波动深度，0不波动，0.2在80%到100%强度之间波动。").defineInRange("pulseDepth", 0.0D, 0.0D, 1.0D);
        VISUAL_DISTANCE_ENABLED = builder.comment("启用距离雾适配；关闭后保留独立屏幕遮蔽。").define("distanceVeilEnabled", true);
        VISUAL_PULSE_RADIUS = builder.comment("波动变浅时同步扩大中心清晰区域。").define("pulseAffectsRadius", false);
        VISUAL_REFRESH_TICKS = builder.comment("渐出中再次触发时的回升刻数。").defineInRange("refreshBlendTicks", 5, 0, 100);
        VISUAL_CLEAR_TICKS = builder.comment("牛奶等主动解除后的视觉退场刻数，逻辑状态立即结束。").defineInRange("clearFadeOutTicks", 4, 0, 100);
        builder.pop();

        SPEC = builder.build();
    }

    private static void defineRequirements(ForgeConfigSpec.Builder builder, TravailAspect aspect, List<String> biomes, List<String> structures) {
        BIOME_POOLS.put(aspect, builder.comment("格式：群系ID|显示名称|权重", "Format: biome_id|display_name|weight").defineListAllowEmpty("biomes", biomes, TravailConfig::validRequirementEntry));
        STRUCTURE_POOLS.put(aspect, builder.comment("格式：结构ID|显示名称|权重", "Format: structure_id|display_name|weight").defineListAllowEmpty("structures", structures, TravailConfig::validRequirementEntry));
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

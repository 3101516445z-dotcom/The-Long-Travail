package com.thelongtravail.config;

import com.thelongtravail.config.defaults.FishingEntityCompatDefaults;
import com.thelongtravail.config.defaults.RewardCompatDefaults;

import static com.thelongtravail.config.TravailDefinitionSupport.decimal;
import static com.thelongtravail.config.TravailDefinitionSupport.defineRequirements;
import static com.thelongtravail.config.TravailDefinitionSupport.integer;
import static com.thelongtravail.config.TravailPoolDefaults.defaultFishingEntities;
import static com.thelongtravail.config.TravailPoolDefaults.defaultFishingItems;

import com.thelongtravail.TravailAspect;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Map;

// 冥府主题的要求池与奖励。
record UnderworldDefinition(
        ForgeConfigSpec.DoubleValue underworldDamageReduction,
        ForgeConfigSpec.IntValue underworldExperienceLevelCap,
        ForgeConfigSpec.IntValue underworldAnvilLevelCap,
        ForgeConfigSpec.IntValue underworldFortuneBonus,
        ForgeConfigSpec.IntValue underworldLootingBonus,
        ForgeConfigSpec.DoubleValue fishEntityChance,
        ForgeConfigSpec.DoubleValue fishSpecialChance,
        ForgeConfigSpec.ConfigValue<List<? extends String>> fishSpecialItems,
        ForgeConfigSpec.ConfigValue<List<? extends String>> fishEntities) {
    static UnderworldDefinition define(
            ForgeConfigSpec.Builder builder,
            Map<TravailAspect, ForgeConfigSpec.ConfigValue<List<? extends String>>> biomePools,
            Map<TravailAspect, ForgeConfigSpec.ConfigValue<List<? extends String>>>
                    structurePools) {
        ForgeConfigSpec.DoubleValue underworldDamageReduction =
                decimal(
                        builder,
                        "travail.malice.outgoingDamageReduction",
                        0.50D,
                        0.0D,
                        1.0D,
                        "造成伤害的削减比例；0.5表示削减50%。",
                        "Fractional reduction in damage dealt; 0.5 means a 50% reduction.");
        ForgeConfigSpec.IntValue underworldExperienceLevelCap =
                integer(
                        builder,
                        "travail.malice.experienceLevelCap",
                        60,
                        0,
                        1_000_000,
                        "冥府之恶意生效时的经验等级上限。",
                        "Maximum experience level while Underworld Malice is active.");
        ForgeConfigSpec.IntValue underworldAnvilLevelCap =
                integer(
                        builder,
                        "travail.witness.anvilLevelCostCap",
                        20,
                        0,
                        1_000_000,
                        "冥府之见证生效时的铁砧经验等级消耗上限。",
                        "Maximum anvil cost in experience levels while Underworld Witness is"
                                + " active.");
        ForgeConfigSpec.IntValue underworldFortuneBonus =
                integer(
                        builder,
                        "travail.witness.fortuneBonus",
                        6,
                        0,
                        1_000_000,
                        "冥府之见证提供的时运等级加成。",
                        "Fortune level bonus granted by Underworld Witness.");
        ForgeConfigSpec.IntValue underworldLootingBonus =
                integer(
                        builder,
                        "travail.witness.lootingBonus",
                        6,
                        0,
                        1_000_000,
                        "冥府之见证提供的抢夺等级加成。",
                        "Looting level bonus granted by Underworld Witness.");
        ForgeConfigSpec.DoubleValue fishEntityChance =
                decimal(
                        builder,
                        "travail.witness.fishing.fishingEntityChance",
                        0.30D,
                        0.0D,
                        1.0D,
                        "钓出生物的概率。与钓出特殊物品的概率之和超过100%时，两项按原比例缩放至合计100%。",
                        "Chance to fish up a creature. If this and the special-item chance total"
                                + " more than 100%, both are scaled proportionally to total 100%.");
        ForgeConfigSpec.DoubleValue fishSpecialChance =
                decimal(
                        builder,
                        "travail.witness.fishing.fishingSpecialItemChance",
                        0.50D,
                        0.0D,
                        1.0D,
                        "钓出特殊物品的概率。与钓出生物的概率之和超过100%时，两项按原比例缩放至合计100%。",
                        "Chance to fish up a special item. If this and the creature chance total"
                                + " more than 100%, both are scaled proportionally to total 100%.");
        defineRequirements(
                builder,
                biomePools,
                structurePools,
                TravailAspect.UNDERWORLD,
                List.of(
                        "minecraft:nether_wastes|@biome.minecraft.nether_wastes|3",
                        "minecraft:soul_sand_valley|@biome.minecraft.soul_sand_valley|3",
                        "minecraft:warped_forest|@biome.minecraft.warped_forest|3",
                        "minecraft:basalt_deltas|@biome.minecraft.basalt_deltas|1",
                        "minecraft:crimson_forest|@biome.minecraft.crimson_forest|1",
                        "minecraft:wooded_badlands|@biome.minecraft.wooded_badlands|1"),
                List.of(
                        "minecraft:fortress|@structure.minecraft.fortress|1",
                        "minecraft:bastion_remnant|@structure.minecraft.bastion_remnant|1"));
        RewardCompatDefaults.define(builder, TravailAspect.UNDERWORLD);
        ForgeConfigSpec.ConfigValue<List<? extends String>> fishSpecialItems =
                builder.comment(
                                "格式：物品ID|权重|最小数量|最大数量",
                                "Format: item_id|weight|min_count|max_count",
                                "不可堆叠的物品每件都会生成一个掉落实体；一次发放过多可能造成卡顿。",
                                "Each non-stackable item creates a separate dropped item entity."
                                        + " Delivering too many at once may cause lag.")
                        .defineListAllowEmpty(
                                "travail.witness.fishing.fishingSpecialItems",
                                defaultFishingItems(),
                                TravailDefinitionSupport::validRewardEntry);
        FishingEntityCompatDefaults.define(builder);
        ForgeConfigSpec.ConfigValue<List<? extends String>> fishEntities =
                builder.comment("格式：实体ID|权重", "Format: entity_id|weight")
                        .defineListAllowEmpty(
                                "travail.witness.fishing.fishingEntities",
                                defaultFishingEntities(),
                                TravailDefinitionSupport::validWeightedEntry);
        return new UnderworldDefinition(
                underworldDamageReduction,
                underworldExperienceLevelCap,
                underworldAnvilLevelCap,
                underworldFortuneBonus,
                underworldLootingBonus,
                fishEntityChance,
                fishSpecialChance,
                fishSpecialItems,
                fishEntities);
    }
}

package com.thelongtravail.config;

import static com.thelongtravail.config.TravailDefinitionSupport.decimal;
import static com.thelongtravail.config.TravailDefinitionSupport.defineRequirements;
import static com.thelongtravail.config.TravailDefinitionSupport.integer;

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
        ForgeConfigSpec.IntValue underworldExtraCurioSlots) {
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
        ForgeConfigSpec.IntValue underworldExtraCurioSlots =
                integer(
                        builder,
                        "travail.witness.extraGenericCurioSlots",
                        1,
                        0,
                        64,
                        "冥府之见证提供的通用Curios饰品槽数量。",
                        "Number of generic Curios accessory slots granted by Underworld Witness.");
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
        return new UnderworldDefinition(
                underworldDamageReduction,
                underworldExperienceLevelCap,
                underworldAnvilLevelCap,
                underworldFortuneBonus,
                underworldLootingBonus,
                underworldExtraCurioSlots);
    }
}

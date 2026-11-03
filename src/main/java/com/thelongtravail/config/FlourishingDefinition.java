package com.thelongtravail.config;

import static com.thelongtravail.config.TravailDefinitionSupport.decimal;
import static com.thelongtravail.config.TravailDefinitionSupport.defineRequirements;
import static com.thelongtravail.config.TravailDefinitionSupport.integer;

import com.thelongtravail.TravailAspect;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Map;

// 繁茂主题的要求池与规则。
record FlourishingDefinition(
        ForgeConfigSpec.IntValue maxHealingBonus,
        ForgeConfigSpec.DoubleValue flourishingHealingReduction,
        ForgeConfigSpec.DoubleValue flourishingHealthThreshold,
        ForgeConfigSpec.DoubleValue flourishingDamageMultiplier,
        ForgeConfigSpec.DoubleValue flourishingBonusPerBiome) {
    static FlourishingDefinition define(
            ForgeConfigSpec.Builder builder,
            Map<TravailAspect, ForgeConfigSpec.ConfigValue<List<? extends String>>> biomePools,
            Map<TravailAspect, ForgeConfigSpec.ConfigValue<List<? extends String>>>
                    structurePools) {
        ForgeConfigSpec.IntValue maxHealingBonus =
                integer(
                        builder,
                        "travail.witness.maxHealingBonus",
                        0,
                        0,
                        1_000_000,
                        "繁茂之见证的治疗量累计加成上限；100表示额外增加10000%，0表示无上限。",
                        "Maximum accumulated healing bonus from Flourishing Witness; 100 means an"
                                + " additional 10000%, and 0 means no limit.");
        ForgeConfigSpec.DoubleValue flourishingHealingReduction =
                decimal(
                        builder,
                        "travail.malice.healingReduction",
                        0.50D,
                        0.0D,
                        1.0D,
                        "受到的治疗量降低的比例；0.5表示削减50%。",
                        "Fractional reduction in healing received; 0.5 means a 50% reduction.");
        ForgeConfigSpec.DoubleValue flourishingHealthThreshold =
                decimal(
                        builder,
                        "travail.malice.doubleDamageHealthThreshold",
                        0.50D,
                        0.0D,
                        1.0D,
                        "当前生命值占最大生命值的比例高于此值时，应用 incomingDamageMultiplier 指定的受伤倍率。",
                        "When current health as a fraction of maximum health is above this value,"
                                + " apply the incomingDamageMultiplier damage multiplier.");
        ForgeConfigSpec.DoubleValue flourishingDamageMultiplier =
                decimal(
                        builder,
                        "travail.malice.incomingDamageMultiplier",
                        2.0D,
                        0.0D,
                        1_000_000.0D,
                        "生命值比例高于 doubleDamageHealthThreshold 时，受到的伤害倍率。",
                        "Incoming damage multiplier while the health fraction is above"
                                + " doubleDamageHealthThreshold.");
        ForgeConfigSpec.DoubleValue flourishingBonusPerBiome =
                decimal(
                        builder,
                        "travail.witness.healingBonusPerBiome",
                        0.05D,
                        0.0D,
                        1_000_000.0D,
                        "每发现一个群系增加的治疗量加成；0.05表示额外增加5%。",
                        "Healing bonus added per biome discovered; 0.05 means an additional 5%.");
        defineRequirements(
                builder,
                biomePools,
                structurePools,
                TravailAspect.FLOURISHING,
                List.of(
                        "minecraft:plains|@biome.minecraft.plains|3",
                        "minecraft:forest|@biome.minecraft.forest|3",
                        "minecraft:taiga|@biome.minecraft.taiga|3",
                        "minecraft:swamp|@biome.minecraft.swamp|1",
                        "minecraft:jungle|@biome.minecraft.jungle|1",
                        "minecraft:savanna|@biome.minecraft.savanna|1"),
                List.of(
                        "minecraft:village_plains|@structure.minecraft.village_plains|1",
                        "minecraft:mansion|@structure.minecraft.mansion|1"));
        return new FlourishingDefinition(
                maxHealingBonus,
                flourishingHealingReduction,
                flourishingHealthThreshold,
                flourishingDamageMultiplier,
                flourishingBonusPerBiome);
    }
}

package com.thelongtravail.config;

import static com.thelongtravail.config.TravailDefinitionSupport.decimal;
import static com.thelongtravail.config.TravailDefinitionSupport.defineRequirements;
import static com.thelongtravail.config.TravailDefinitionSupport.integer;
import static com.thelongtravail.config.TravailPoolDefaults.defaultFarReachEffects;

import com.thelongtravail.TravailAspect;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Map;

// 穷遐主题的要求池与规则。
record FarReachDefinition(
        ForgeConfigSpec.DoubleValue farAllClearChance,
        ForgeConfigSpec.DoubleValue farAttackClearChance,
        ForgeConfigSpec.DoubleValue farAttackCooldownSeconds,
        ForgeConfigSpec.IntValue farWitnessLevelBonus,
        ForgeConfigSpec.ConfigValue<List<? extends String>> farWitnessLevelBlacklist,
        ForgeConfigSpec.IntValue farWitnessFallbackMaxLevel,
        ForgeConfigSpec.DoubleValue farWitnessIntervalSeconds,
        ForgeConfigSpec.IntValue farWitnessActionCount,
        ForgeConfigSpec.DoubleValue farWitnessPositiveDurationSeconds,
        ForgeConfigSpec.ConfigValue<List<? extends String>> farReachPositiveEffects) {
    static FarReachDefinition define(
            ForgeConfigSpec.Builder builder,
            Map<TravailAspect, ForgeConfigSpec.ConfigValue<List<? extends String>>> biomePools,
            Map<TravailAspect, ForgeConfigSpec.ConfigValue<List<? extends String>>>
                    structurePools) {
        ForgeConfigSpec.DoubleValue farAllClearChance =
                decimal(
                        builder,
                        "travail.malice.clearAllBeneficialChanceOnHit",
                        0.10D,
                        0.0D,
                        1.0D,
                        "受击时清除全部增益效果的概率。",
                        "Chance to remove all beneficial effects when hit.");
        ForgeConfigSpec.DoubleValue farAttackClearChance =
                decimal(
                        builder,
                        "travail.malice.clearBeneficialChanceOnAttack",
                        0.50D,
                        0.0D,
                        1.0D,
                        "攻击后清除一个增益效果的概率。",
                        "Chance to remove one beneficial effect after attacking.");
        ForgeConfigSpec.DoubleValue farAttackCooldownSeconds =
                decimal(
                        builder,
                        "travail.malice.attackClearCooldownSeconds",
                        5.0D,
                        0.0D,
                        1_000_000.0D,
                        "攻击触发增益效果清除的冷却时间，单位：秒；0表示无冷却。",
                        "Cooldown for beneficial-effect removal triggered by attacking, in seconds;"
                                + " 0 means no cooldown.");
        ForgeConfigSpec.IntValue farWitnessLevelBonus =
                integer(
                        builder,
                        "travail.witness.beneficialLevelBonus",
                        1,
                        0,
                        255,
                        "新获得增益效果时增加的等级数。",
                        "Levels added to newly gained beneficial effects.");
        ForgeConfigSpec.ConfigValue<List<? extends String>> farWitnessLevelBlacklist =
                builder.comment(
                                "不接受穷遐之见证等级加成的效果ID列表，例如 minecraft:night_vision。\n"
                                        + "仅跳过等级提升，不阻止获得效果，也不改变随机效果池。空列表表示不排除任何效果。",
                                "Effect IDs excluded from Far Reach Witness's level bonus, such as"
                                        + " minecraft:night_vision.\n"
                                        + "Only skips the level increase. Does not prevent gaining the"
                                        + " effect or change random effect pools. An empty list"
                                        + " excludes no effects.")
                        .defineListAllowEmpty(
                                "travail.witness.beneficialLevelBonusBlacklist",
                                List.of("minecraft:night_vision", "minecraft:water_breathing"),
                                value -> value instanceof String);
        ForgeConfigSpec.IntValue farWitnessFallbackMaxLevel =
                integer(
                        builder,
                        "travail.witness.unlistedEffectMaxLevel",
                        5,
                        1,
                        256,
                        "既未列入穷遐效果池、也未列入幽谷效果池的增益效果等级上限。",
                        "Maximum level for beneficial effects absent from both the Far Reach and"
                                + " Deep Valley effect pools.");
        ForgeConfigSpec.DoubleValue farWitnessIntervalSeconds =
                decimal(
                        builder,
                        "travail.witness.witnessIntervalSeconds",
                        5.0D,
                        0.0D,
                        1_000_000.0D,
                        "净化或给予增益效果的触发间隔，单位：秒；0表示每游戏刻触发。间隔过短且每轮次数较多时，可能增加游戏负担。",
                        "Interval between cleansing or granting beneficial effects, in seconds; 0"
                                + " triggers every game tick. Short intervals combined with many"
                                + " attempts per cycle may increase processing load.");
        ForgeConfigSpec.IntValue farWitnessActionCount =
                integer(
                        builder,
                        "travail.witness.cleanseOrGrantCount",
                        2,
                        0,
                        64,
                        "每轮尝试清除减益效果的次数；剩余次数用于给予增益效果。次数过多且触发间隔过短时，可能增加游戏负担。",
                        "Attempts to remove harmful effects per cycle; remaining attempts grant"
                                + " beneficial effects. Many attempts combined with short trigger"
                                + " intervals may increase processing load.");
        ForgeConfigSpec.DoubleValue farWitnessPositiveDurationSeconds =
                decimal(
                        builder,
                        "travail.witness.grantedEffectDurationSeconds",
                        5.0D,
                        0.0D,
                        1_000_000.0D,
                        "随机给予的增益效果持续时间，单位：秒。",
                        "Duration of randomly granted beneficial effects, in seconds.");
        defineRequirements(
                builder,
                biomePools,
                structurePools,
                TravailAspect.FAR_REACH,
                List.of(
                        "minecraft:snowy_plains|@biome.minecraft.snowy_plains|3",
                        "minecraft:desert|@biome.minecraft.desert|3",
                        "minecraft:badlands|@biome.minecraft.badlands|3",
                        "minecraft:windswept_hills|@biome.minecraft.windswept_hills|1",
                        "minecraft:snowy_taiga|@biome.minecraft.snowy_taiga|1",
                        "minecraft:ice_spikes|@biome.minecraft.ice_spikes|1"),
                List.of(
                        "minecraft:pillager_outpost|@structure.minecraft.pillager_outpost|1",
                        "minecraft:village_desert|@structure.minecraft.village_desert|1"));
        EffectCompatDefaults.define(builder, TravailAspect.FAR_REACH);
        ForgeConfigSpec.ConfigValue<List<? extends String>> farReachPositiveEffects =
                builder.comment("格式：效果ID|最高等级|权重", "Format: effect_id|max_level|weight")
                        .defineListAllowEmpty(
                                "travail.witness.positiveEffects",
                                defaultFarReachEffects(),
                                TravailDefinitionSupport::validEffectEntry);
        return new FarReachDefinition(
                farAllClearChance,
                farAttackClearChance,
                farAttackCooldownSeconds,
                farWitnessLevelBonus,
                farWitnessLevelBlacklist,
                farWitnessFallbackMaxLevel,
                farWitnessIntervalSeconds,
                farWitnessActionCount,
                farWitnessPositiveDurationSeconds,
                farReachPositiveEffects);
    }
}

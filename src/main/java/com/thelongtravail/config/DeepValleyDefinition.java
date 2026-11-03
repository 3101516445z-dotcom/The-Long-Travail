package com.thelongtravail.config;

import static com.thelongtravail.config.TravailDefinitionSupport.decimal;
import static com.thelongtravail.config.TravailDefinitionSupport.defineRequirements;
import static com.thelongtravail.config.TravailDefinitionSupport.integer;
import static com.thelongtravail.config.TravailPoolDefaults.defaultDeepValleyEffects;

import com.thelongtravail.TravailAspect;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Map;

// 幽谷主题的要求池与规则。
record DeepValleyDefinition(
        ForgeConfigSpec.DoubleValue valleyVisualDeprivationChance,
        ForgeConfigSpec.DoubleValue valleyVisualDeprivationDurationSeconds,
        ForgeConfigSpec.IntValue valleyVisualDeprivationFadeInTicks,
        ForgeConfigSpec.IntValue valleyVisualDeprivationFadeOutTicks,
        ForgeConfigSpec.DoubleValue valleyStiffChance,
        ForgeConfigSpec.DoubleValue valleyStiffDurationSeconds,
        ForgeConfigSpec.DoubleValue valleySwiftnessDurationSeconds,
        ForgeConfigSpec.IntValue valleySwiftnessLevel,
        ForgeConfigSpec.IntValue valleyRandomEffectCount,
        ForgeConfigSpec.DoubleValue valleyRandomEffectDurationSeconds,
        ForgeConfigSpec.DoubleValue valleyWitnessAttackCooldownSeconds,
        ForgeConfigSpec.ConfigValue<List<? extends String>> deepValleyPositiveEffects) {
    static DeepValleyDefinition define(
            ForgeConfigSpec.Builder builder,
            Map<TravailAspect, ForgeConfigSpec.ConfigValue<List<? extends String>>> biomePools,
            Map<TravailAspect, ForgeConfigSpec.ConfigValue<List<? extends String>>>
                    structurePools) {
        ForgeConfigSpec.DoubleValue valleyVisualDeprivationChance =
                decimal(
                        builder,
                        "travail.malice.visualDeprivationChanceOnHit",
                        0.50D,
                        0.0D,
                        1.0D,
                        "受击时获得视觉剥夺的概率。",
                        "Chance to gain Visual Deprivation when hit.");
        ForgeConfigSpec.DoubleValue valleyVisualDeprivationDurationSeconds =
                decimal(
                        builder,
                        "travail.visualDeprivation.visualDeprivationDurationSeconds",
                        5.0D,
                        0.0D,
                        1_000_000.0D,
                        "视觉剥夺的持续时间，单位：秒。",
                        "Visual Deprivation duration, in seconds.");
        ForgeConfigSpec.IntValue valleyVisualDeprivationFadeInTicks =
                integer(
                        builder,
                        "travail.visualDeprivation.visualDeprivationFadeInTicks",
                        30,
                        0,
                        100,
                        "视觉剥夺的淡入时间，单位：游戏刻；淡入与淡出时间之和超过总持续时间时，按比例缩短。",
                        "Visual Deprivation fade-in duration, in game ticks. If fade-in and"
                                + " fade-out together exceed the total duration, both are shortened"
                                + " proportionally.");
        ForgeConfigSpec.IntValue valleyVisualDeprivationFadeOutTicks =
                integer(
                        builder,
                        "travail.visualDeprivation.visualDeprivationFadeOutTicks",
                        30,
                        0,
                        100,
                        "视觉剥夺的淡出时间，单位：游戏刻；淡入与淡出时间之和超过总持续时间时，按比例缩短。",
                        "Visual Deprivation fade-out duration, in game ticks. If fade-in and"
                                + " fade-out together exceed the total duration, both are shortened"
                                + " proportionally.");
        ForgeConfigSpec.DoubleValue valleyStiffChance =
                decimal(
                        builder,
                        "travail.malice.stiffChanceOnAttack",
                        0.20D,
                        0.0D,
                        1.0D,
                        "攻击后获得僵硬效果的概率。",
                        "Chance to gain Stiff after attacking.");
        ForgeConfigSpec.DoubleValue valleyStiffDurationSeconds =
                decimal(
                        builder,
                        "travail.malice.stiffDurationSeconds",
                        1.5D,
                        0.0D,
                        1_000_000.0D,
                        "僵硬效果的持续时间，单位：秒。",
                        "Stiff duration, in seconds.");
        ForgeConfigSpec.DoubleValue valleySwiftnessDurationSeconds =
                decimal(
                        builder,
                        "travail.witness.swiftnessDurationSeconds",
                        3.0D,
                        0.0D,
                        1_000_000.0D,
                        "幽谷之见证给予的迅捷效果持续时间，单位：秒。",
                        "Duration of Swiftness granted by Deep Valley Witness, in seconds.");
        ForgeConfigSpec.IntValue valleySwiftnessLevel =
                integer(
                        builder,
                        "travail.witness.swiftnessLevel",
                        1,
                        1,
                        256,
                        "幽谷之见证给予的迅捷效果等级。",
                        "Level of Swiftness granted by Deep Valley Witness.");
        ForgeConfigSpec.IntValue valleyRandomEffectCount =
                integer(
                        builder,
                        "travail.witness.randomEffectCount",
                        1,
                        0,
                        64,
                        "每次成功攻击给予的随机增益效果数量。数量过多且增益冷却过短时，可能增加游戏负担。",
                        "Number of random beneficial effects granted per successful attack. Large"
                                + " counts combined with short buff cooldowns may increase processing"
                                + " load.");
        ForgeConfigSpec.DoubleValue valleyRandomEffectDurationSeconds =
                decimal(
                        builder,
                        "travail.witness.randomEffectDurationSeconds",
                        3.0D,
                        0.0D,
                        1_000_000.0D,
                        "随机增益效果的持续时间，单位：秒。",
                        "Duration of random beneficial effects, in seconds.");
        ForgeConfigSpec.DoubleValue valleyWitnessAttackCooldownSeconds =
                decimal(
                        builder,
                        "travail.witness.witnessAttackCooldownSeconds",
                        3.0D,
                        0.0D,
                        1_000_000.0D,
                        "幽谷之见证通过造成伤害触发增益的冷却时间，单位：秒；0表示无冷却。",
                        "Cooldown for buffs triggered by dealing damage with Deep Valley Witness,"
                                + " in seconds; 0 means no cooldown.");
        defineRequirements(
                builder,
                biomePools,
                structurePools,
                TravailAspect.DEEP_VALLEY,
                List.of(
                        "minecraft:dripstone_caves|@biome.minecraft.dripstone_caves|3",
                        "minecraft:lush_caves|@biome.minecraft.lush_caves|3",
                        "minecraft:deep_dark|@biome.minecraft.deep_dark|3",
                        "minecraft:dark_forest|@biome.minecraft.dark_forest|1",
                        "minecraft:mushroom_fields|@biome.minecraft.mushroom_fields|1",
                        "minecraft:stony_shore|@biome.minecraft.stony_shore|1"),
                List.of(
                        "minecraft:mineshaft|@structure.minecraft.mineshaft|1",
                        "minecraft:ancient_city|@structure.minecraft.ancient_city|1"));
        EffectCompatDefaults.define(builder, TravailAspect.DEEP_VALLEY);
        ForgeConfigSpec.ConfigValue<List<? extends String>> deepValleyPositiveEffects =
                builder.comment("格式：效果ID|最高等级|权重", "Format: effect_id|max_level|weight")
                        .defineListAllowEmpty(
                                "travail.witness.positiveEffects",
                                defaultDeepValleyEffects(),
                                TravailDefinitionSupport::validEffectEntry);
        return new DeepValleyDefinition(
                valleyVisualDeprivationChance,
                valleyVisualDeprivationDurationSeconds,
                valleyVisualDeprivationFadeInTicks,
                valleyVisualDeprivationFadeOutTicks,
                valleyStiffChance,
                valleyStiffDurationSeconds,
                valleySwiftnessDurationSeconds,
                valleySwiftnessLevel,
                valleyRandomEffectCount,
                valleyRandomEffectDurationSeconds,
                valleyWitnessAttackCooldownSeconds,
                deepValleyPositiveEffects);
    }
}

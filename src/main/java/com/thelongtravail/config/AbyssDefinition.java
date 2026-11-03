package com.thelongtravail.config;

import static com.thelongtravail.config.TravailDefinitionSupport.decimal;
import static com.thelongtravail.config.TravailDefinitionSupport.defineRequirements;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig.FluidProtection;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Map;

// 归墟主题的要求池与规则。
record AbyssDefinition(
        ForgeConfigSpec.DoubleValue abyssFluidDamage,
        ForgeConfigSpec.DoubleValue abyssFluidIntervalSeconds,
        ForgeConfigSpec.DoubleValue abyssMiningReduction,
        ForgeConfigSpec.DoubleValue abyssEnvironmentMultiplier,
        ForgeConfigSpec.DoubleValue abyssCancelChance,
        ForgeConfigSpec.BooleanValue fluidErosionUnavoidable,
        ForgeConfigSpec.EnumValue<FluidProtection> fluidProtection) {
    static AbyssDefinition define(
            ForgeConfigSpec.Builder builder,
            Map<TravailAspect, ForgeConfigSpec.ConfigValue<List<? extends String>>> biomePools,
            Map<TravailAspect, ForgeConfigSpec.ConfigValue<List<? extends String>>>
                    structurePools) {
        ForgeConfigSpec.DoubleValue abyssFluidDamage =
                decimal(
                        builder,
                        "travail.malice.fluidTrueDamage",
                        1.0D,
                        0.0D,
                        1_000_000.0D,
                        "每次流体侵蚀造成的真实伤害值。",
                        "True damage dealt by each fluid erosion trigger.");
        ForgeConfigSpec.DoubleValue abyssFluidIntervalSeconds =
                decimal(
                        builder,
                        "travail.malice.fluidDamageIntervalSeconds",
                        1.0D,
                        0.0D,
                        1_000_000.0D,
                        "流体伤害的触发间隔，单位：秒；0表示每游戏刻触发。间隔过短可能增加游戏负担，多人时更明显。",
                        "Interval between fluid damage triggers, in seconds; 0 triggers every game"
                                + " tick. Short intervals may increase processing load, especially with"
                                + " multiple players.");
        ForgeConfigSpec.DoubleValue abyssMiningReduction =
                decimal(
                        builder,
                        "travail.malice.miningSpeedReduction",
                        0.50D,
                        0.0D,
                        1.0D,
                        "挖掘速度削减比例；0.5表示削减50%。",
                        "Fractional mining speed reduction; 0.5 means a 50% reduction.");
        ForgeConfigSpec.DoubleValue abyssEnvironmentMultiplier =
                decimal(
                        builder,
                        "travail.malice.environmentDamageMultiplier",
                        10.0D,
                        0.0D,
                        1_000_000.0D,
                        "窒息、挤压、溺水与冻结伤害倍率；见证免疫同一组伤害。",
                        "Damage multiplier for suffocation, entity cramming, drowning, and"
                                + " freezing. Witness grants immunity to the same damage types.");
        ForgeConfigSpec.DoubleValue abyssCancelChance =
                decimal(
                        builder,
                        "travail.witness.fluidDamageCancelChance",
                        0.50D,
                        0.0D,
                        1.0D,
                        "归墟之见证生效时，身处流体中取消伤害的概率。",
                        "Chance for Abyss Witness to negate damage while the player is in fluid.");
        ForgeConfigSpec.BooleanValue fluidErosionUnavoidable =
                builder.comment(
                                "是否防止流体真实伤害在攻击、受伤和伤害结算事件中被取消或降为零，并保留苦旅自身倍率计算后的伤害下限。\n"
                                        + "Prevent fluid true damage from being canceled or reduced to"
                                        + " zero in attack, hurt, and damage-calculation events, and"
                                        + " preserve a damage floor after The Long Travail's own"
                                        + " multipliers are applied.",
                                "仍遵守苦旅自身的免伤和输出限制，不绕过伤害事件之前的无敌判定、图腾或死亡事件。关闭后仍保留流体伤害原有的护甲等穿透规则。",
                                "Still follows The Long Travail's own damage immunity and output"
                                        + " limits. Does not bypass invulnerability checks before"
                                        + " damage events, totems, or death events. Disabling this"
                                        + " preserves fluid damage's existing armor and other bypass"
                                        + " rules.")
                        .define("travail.malice.fluidErosionUnavoidable", true);
        ForgeConfigSpec.EnumValue<FluidProtection> fluidProtection =
                builder.comment(
                                "ENFORCED：防止事件取消伤害，并保留伤害下限。\n"
                                        + "RESPECT_CANCELLATION：允许事件取消伤害；伤害未被取消时，保留下限。\n"
                                        + "STANDARD：允许事件取消伤害或降低伤害。\n"
                                        + "ENFORCED: prevent events from canceling damage and preserve"
                                        + " a damage floor.\n"
                                        + "RESPECT_CANCELLATION: allow events to cancel damage;"
                                        + " preserve the floor if damage is not canceled.\n"
                                        + "STANDARD: allow events to cancel or reduce damage.",
                                "fluidErosionUnavoidable=false 时，始终使用 STANDARD。\n"
                                        + "所有模式均保留流体伤害原有的护甲和吸收生命值穿透规则。\n"
                                        + "When fluidErosionUnavoidable=false, STANDARD is always"
                                        + " used.\n"
                                        + "All modes retain fluid damage's existing armor and"
                                        + " absorption bypass rules.")
                        .defineEnum(
                                "travail.malice.fluidErosionProtectionMode",
                                FluidProtection.ENFORCED);
        defineRequirements(
                builder,
                biomePools,
                structurePools,
                TravailAspect.ABYSS,
                List.of(
                        "minecraft:ocean|@biome.minecraft.ocean|3",
                        "minecraft:deep_ocean|@biome.the_long_travail.warm_deep_ocean|3",
                        "minecraft:warm_ocean|@biome.minecraft.warm_ocean|3",
                        "minecraft:lukewarm_ocean|@biome.minecraft.lukewarm_ocean|2",
                        "minecraft:deep_lukewarm_ocean|@biome.minecraft.deep_lukewarm_ocean|2",
                        "minecraft:cold_ocean|@biome.minecraft.cold_ocean|1",
                        "minecraft:deep_cold_ocean|@biome.minecraft.deep_cold_ocean|1",
                        "minecraft:frozen_ocean|@biome.minecraft.frozen_ocean|1",
                        "minecraft:deep_frozen_ocean|@biome.minecraft.deep_frozen_ocean|1"),
                List.of(
                        "minecraft:monument|@structure.minecraft.monument|1",
                        "minecraft:ruined_portal_ocean|@structure.minecraft.ruined_portal_ocean|1"));
        return new AbyssDefinition(
                abyssFluidDamage,
                abyssFluidIntervalSeconds,
                abyssMiningReduction,
                abyssEnvironmentMultiplier,
                abyssCancelChance,
                fluidErosionUnavoidable,
                fluidProtection);
    }
}

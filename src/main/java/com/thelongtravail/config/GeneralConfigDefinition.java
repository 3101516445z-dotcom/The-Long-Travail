package com.thelongtravail.config;

import static com.thelongtravail.config.TravailDefinitionSupport.integer;

import com.thelongtravail.config.TravailConfig.ReceivedTrigger;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

// 通用规则与探索抽取数量。
record GeneralConfigDefinition(
        ForgeConfigSpec.BooleanValue amongAllThingsImmuneToKill,
        ForgeConfigSpec.BooleanValue protectArchaeologyLoot,
        ForgeConfigSpec.BooleanValue muteBeneficialEffectGainSounds,
        ForgeConfigSpec.BooleanValue giveStarterItem,
        ForgeConfigSpec.IntValue biomeDrawCount,
        ForgeConfigSpec.IntValue structureDrawCount,
        ForgeConfigSpec.EnumValue<ReceivedTrigger> receivedTrigger,
        ForgeConfigSpec.IntValue receivedCooldown,
        ForgeConfigSpec.IntValue maxAutomaticEffectActions,
        ForgeConfigSpec.ConfigValue<List<? extends String>> receivedExclusions) {
    static GeneralConfigDefinition define(ForgeConfigSpec.Builder builder) {
        com.thelongtravail.loot.UnifiedLootConfig.define(builder);
        JourneyDetectionConfig.define(builder);
        ForgeConfigSpec.BooleanValue amongAllThingsImmuneToKill =
                builder.comment(
                                "“我于万物之中”是否免疫标准 kill 伤害。\n"
                                        + "Whether Among All Things blocks standard kill damage.")
                        .define("effects.amongAllThings.immuneToKill", true);
        ForgeConfigSpec.BooleanValue protectArchaeologyLoot =
                builder.comment(
                                "是否阻止全局战利品修改器改动考古战利品；开启后仅按战利品表抽取。\n"
                                        + "Prevent global loot modifiers from changing archaeology loot;"
                                        + " when enabled, use only the loot table.")
                        .define("compatibility.protectArchaeologyLoot", true);
        ForgeConfigSpec.BooleanValue muteBeneficialEffectGainSounds =
                builder.comment(
                                "佩戴苦旅且处于穷遐或幽谷见证状态时，是否关闭 Sounds 的正面效果获得提示音。\n"
                                        + "Suppress Sounds notifications for gaining beneficial effects"
                                        + " while wearing The Long Travail with Far Reach or Deep Valley Witness.")
                        .define("compatibility.muteBeneficialEffectGainSounds", true);
        ForgeConfigSpec.BooleanValue giveStarterItem =
                builder.comment(
                                "玩家首次进入世界时，是否赠送一本苦旅；每名玩家最多领取一次。",
                                "Give each player a copy of The Long Travail when they first enter"
                                        + " the world, at most once per player.")
                        .define("starter.giveStarterItem", true);
        ForgeConfigSpec.IntValue biomeDrawCount =
                integer(
                        builder,
                        "requirements.biomeDrawCount",
                        3,
                        0,
                        100,
                        "为每种恶意抽取的不同群系数量。",
                        "Number of distinct biomes drawn for each Malice.");
        ForgeConfigSpec.IntValue structureDrawCount =
                integer(
                        builder,
                        "requirements.structureDrawCount",
                        2,
                        0,
                        100,
                        "为每种恶意抽取的不同结构数量。",
                        "Number of distinct structures drawn for each Malice.");
        ForgeConfigSpec.EnumValue<ReceivedTrigger> receivedTrigger =
                builder.comment(
                                "受击恶意的触发方式：ATTACK_ATTEMPT 表示受到攻击时触发；\n"
                                        + "HEALTH_LOSS 表示实际生命值减少后才触发，不含吸收生命值的损失。",
                                "Trigger mode for on-hit Malice: ATTACK_ATTEMPT triggers when"
                                        + " attacked.\n"
                                        + "HEALTH_LOSS triggers only after actual health loss,"
                                        + " excluding absorption loss.")
                        .defineEnum(
                                "receivedMalice.receivedMaliceTrigger",
                                ReceivedTrigger.ATTACK_ATTEMPT);
        ForgeConfigSpec.IntValue receivedCooldown =
                integer(
                        builder,
                        "receivedMalice.receivedMaliceCooldownTicks",
                        0,
                        0,
                        1_000_000,
                        "各类受击恶意共用的冷却时间，单位：游戏刻；0表示无冷却。此设置不改变伤害本身，也不影响由攻击触发的效果。",
                        "Shared cooldown for the different on-hit Malice effects, in game ticks; 0"
                                + " means no cooldown. Does not change damage itself or effects"
                                + " triggered by attacking.");
        ForgeConfigSpec.IntValue maxAutomaticEffectActions =
                integer(
                        builder,
                        "effectLimits.maxAutomaticEffectActionsPerPlayerPerSecond",
                        0,
                        0,
                        1_000_000,
                        "每名玩家连续 20 游戏刻内，穷遐与幽谷共用的自动效果尝试次数上限；失败尝试也计数，0 不限。超限直接跳过。",
                        "Shared Far Reach/Deep Valley automatic effect-attempt limit per player"
                                + " over 20 consecutive game ticks; failed attempts count, 0 is"
                                + " unlimited. Excess attempts are skipped.");
        ForgeConfigSpec.ConfigValue<List<? extends String>> receivedExclusions =
                builder.comment(
                                "不触发受击恶意的伤害类型ID；不改变伤害本身。",
                                "Damage type IDs that do not trigger on-hit Malice; does not change"
                                        + " damage itself.")
                        .defineListAllowEmpty(
                                "receivedMalice.receivedMaliceExcludedDamageTypes",
                                List.of(),
                                value ->
                                        value instanceof String id
                                                && net.minecraft.resources.ResourceLocation
                                                                .tryParse(id)
                                                        != null);
        return new GeneralConfigDefinition(
                amongAllThingsImmuneToKill,
                protectArchaeologyLoot,
                muteBeneficialEffectGainSounds,
                giveStarterItem,
                biomeDrawCount,
                structureDrawCount,
                receivedTrigger,
                receivedCooldown,
                maxAutomaticEffectActions,
                receivedExclusions);
    }
}

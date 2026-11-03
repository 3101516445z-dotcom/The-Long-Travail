package com.thelongtravail.config;

import static com.thelongtravail.config.TravailDefinitionSupport.decimal;
import static com.thelongtravail.config.TravailDefinitionSupport.defineRequirements;
import static com.thelongtravail.config.TravailDefinitionSupport.integer;
import static com.thelongtravail.config.TravailPoolDefaults.defaultPhantomDrops;

import com.thelongtravail.TravailAspect;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Map;

// 无垠主题的要求池与幻影奖励。
record BoundlessDefinition(
        ForgeConfigSpec.DoubleValue boundlessHeightThreshold,
        ForgeConfigSpec.DoubleValue boundlessSpeedReduction,
        ForgeConfigSpec.IntValue boundlessExtraCurioSlots,
        ForgeConfigSpec.IntValue phantomRange,
        ForgeConfigSpec.DoubleValue phantomCheckIntervalSeconds,
        ForgeConfigSpec.IntValue phantomMaxKills,
        ForgeConfigSpec.ConfigValue<List<? extends String>> phantomDrops) {
    static BoundlessDefinition define(
            ForgeConfigSpec.Builder builder,
            Map<TravailAspect, ForgeConfigSpec.ConfigValue<List<? extends String>>> biomePools,
            Map<TravailAspect, ForgeConfigSpec.ConfigValue<List<? extends String>>>
                    structurePools) {
        ForgeConfigSpec.DoubleValue boundlessHeightThreshold =
                decimal(
                        builder,
                        "travail.malice.heightThreshold",
                        100.0D,
                        0.0D,
                        1_000_000.0D,
                        "玩家的 Y 坐标高于此值时，降低移动速度。",
                        "Reduce movement speed when the player's Y coordinate exceeds this"
                                + " threshold.");
        ForgeConfigSpec.DoubleValue boundlessSpeedReduction =
                decimal(
                        builder,
                        "travail.malice.speedReduction",
                        0.50D,
                        0.0D,
                        1.0D,
                        "高空速度削减比例；0.5表示削减50%。",
                        "Fractional movement speed reduction at high altitude; 0.5 means a 50%"
                                + " reduction.");
        ForgeConfigSpec.IntValue boundlessExtraCurioSlots =
                integer(
                        builder,
                        "travail.witness.extraGenericCurioSlots",
                        1,
                        0,
                        64,
                        "无垠之见证提供的通用Curios饰品槽数量。",
                        "Number of generic Curios accessory slots granted by Boundless Witness.");
        ForgeConfigSpec.IntValue phantomRange =
                integer(
                        builder,
                        "travail.witness.phantom.phantomKillRange",
                        32,
                        0,
                        128,
                        "自动击杀幻翼的检测半径，单位：方块。范围过大或幻翼过多时，可能增加游戏负担。",
                        "Detection radius for automatic phantom kills, in blocks. Large radii or"
                                + " large numbers of phantoms may increase processing load.");
        ForgeConfigSpec.DoubleValue phantomCheckIntervalSeconds =
                decimal(
                        builder,
                        "travail.witness.phantom.phantomCheckIntervalSeconds",
                        1.0D,
                        1.0D,
                        1_000_000.0D,
                        "幻翼检测间隔，单位：秒，最短1秒。",
                        "Phantom detection interval, in seconds; minimum 1 second.");
        ForgeConfigSpec.IntValue phantomMaxKills =
                integer(
                        builder,
                        "travail.witness.phantom.phantomMaxKillsPerScan",
                        16,
                        1,
                        1024,
                        "每名玩家每轮最多尝试击杀的幻翼数，包括伤害被取消的尝试。",
                        "Maximum phantom kill attempts per player per scan, including attempts"
                                + " whose damage is canceled.");
        defineRequirements(
                builder,
                biomePools,
                structurePools,
                TravailAspect.BOUNDLESS,
                List.of(
                        "minecraft:end_midlands|@biome.minecraft.end_midlands|3",
                        "minecraft:end_highlands|@biome.minecraft.end_highlands|3",
                        "minecraft:end_barrens|@biome.minecraft.end_barrens|3",
                        "minecraft:the_end|@biome.minecraft.the_end|1",
                        "minecraft:small_end_islands|@biome.minecraft.small_end_islands|1",
                        "minecraft:stony_peaks|@biome.minecraft.stony_peaks|1"),
                List.of(
                        "minecraft:stronghold|@structure.minecraft.stronghold|1",
                        "minecraft:end_city|@structure.minecraft.end_city|1"));
        RewardCompatDefaults.define(builder, TravailAspect.BOUNDLESS);
        ForgeConfigSpec.ConfigValue<List<? extends String>> phantomDrops =
                builder.comment(
                                "格式：物品ID|权重|最小数量|最大数量",
                                "Format: item_id|weight|min_count|max_count",
                                "不可堆叠的物品每件都会生成一个掉落实体；多只幻翼同时产生大量奖励时可能造成卡顿。",
                                "Each non-stackable item creates a separate dropped item entity."
                                        + " Large rewards from multiple phantoms at once may cause"
                                        + " lag.")
                        .defineListAllowEmpty(
                                "travail.witness.phantom.phantomDrops",
                                defaultPhantomDrops(),
                                TravailDefinitionSupport::validRewardEntry);
        return new BoundlessDefinition(
                boundlessHeightThreshold,
                boundlessSpeedReduction,
                boundlessExtraCurioSlots,
                phantomRange,
                phantomCheckIntervalSeconds,
                phantomMaxKills,
                phantomDrops);
    }
}

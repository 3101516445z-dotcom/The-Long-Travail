package com.thelongtravail.config;

import static com.thelongtravail.config.TravailDefinitionSupport.integer;

import net.minecraftforge.common.ForgeConfigSpec;

// 工具物品的搜索与引导参数。
record UtilityItemsDefinition(
        ForgeConfigSpec.IntValue wayguideGuideDurationSeconds,
        ForgeConfigSpec.IntValue wayguideRadius,
        ForgeConfigSpec.IntValue wayguideTimeoutSeconds,
        ForgeConfigSpec.IntValue wayguideBiomeSpacing,
        ForgeConfigSpec.IntValue wayguideHeightSpacing,
        ForgeConfigSpec.IntValue wayguideSamplesPerTick,
        ForgeConfigSpec.IntValue wayguideSearchMillis,
        ForgeConfigSpec.IntValue wayguideMaxSamples,
        ForgeConfigSpec.IntValue wayguideCacheTicks) {
    static UtilityItemsDefinition define(ForgeConfigSpec.Builder builder) {
        ForgeConfigSpec.IntValue wayguideGuideDurationSeconds =
                integer(
                        builder,
                        "items.wayguide.guideDurationSeconds",
                        10,
                        1,
                        3600,
                        "路引找到目标后的引导时长，单位：秒。\n从开始飞出到变成掉落物为止，包含飞行和悬停，不包含搜索等待。\n修改后也会影响正在指路的路引。",
                        "Duration of guidance after the Wayguide finds a target, in seconds.\n"
                                + "Measured from launch until becoming a dropped item, including flight"
                                + " and hovering but excluding search time.\n"
                                + "Changes also affect Wayguides already in flight.");
        ForgeConfigSpec.IntValue wayguideRadius =
                integer(
                        builder,
                        "items.wayguide.search.searchRadius",
                        5000,
                        16,
                        30000000,
                        "路引的最大水平搜索半径，单位：格。",
                        "Maximum horizontal Wayguide search radius, in blocks.");
        ForgeConfigSpec.IntValue wayguideTimeoutSeconds =
                integer(
                        builder,
                        "items.wayguide.search.searchTimeoutSeconds",
                        30,
                        1,
                        300,
                        "单次搜索的最长等待时间，单位：现实秒，包含准备和排队；超时后停止搜索并返还路引。",
                        "Maximum wait per search, in real-world seconds, including preparation and queueing; a timeout stops the search and returns the Wayguide.");
        ForgeConfigSpec.IntValue wayguideBiomeSpacing =
                integer(
                        builder,
                        "items.wayguide.sampling.biomeSampleSpacing",
                        32,
                        4,
                        256,
                        "群系的水平采样间距，单位：方块；数值越小，搜索越精细，但耗时也越长。",
                        "Horizontal biome sample spacing, in blocks. Smaller values provide finer"
                                + " searches but take longer.");
        ForgeConfigSpec.IntValue wayguideHeightSpacing =
                integer(
                        builder,
                        "items.wayguide.sampling.heightSampleSpacing",
                        32,
                        4,
                        256,
                        "群系采样的垂直间距，单位：方块；无论间距如何，都会额外检查玩家所在高度。",
                        "Vertical spacing between biome samples, in blocks. The player's height is"
                                + " always checked as well.");
        ForgeConfigSpec.IntValue wayguideSamplesPerTick =
                integer(
                        builder,
                        "items.wayguide.budget.samplesPerTick",
                        2048,
                        1,
                        65536,
                        "全服每游戏刻最多进行的路引搜索采样次数，由所有玩家共用。",
                        "Maximum Wayguide search samples per game tick across the server, shared by"
                                + " all players.");
        ForgeConfigSpec.IntValue wayguideSearchMillis =
                integer(
                        builder,
                        "items.wayguide.budget.searchMillisPerTick",
                        2,
                        1,
                        50,
                        "每游戏刻用于搜索的时间预算，单位：毫秒；单次处理可能使实际耗时超过预算。",
                        "Target search processing time per game tick, in milliseconds; individual operations may exceed this budget.");
        ForgeConfigSpec.IntValue wayguideMaxSamples =
                integer(
                        builder,
                        "items.wayguide.budget.maxSamplesPerSearch",
                        8000000,
                        1,
                        32000000,
                        "单次路引搜索的采样次数上限；达到上限后停止搜索并返还路引。",
                        "Maximum samples per Wayguide search. Reaching the limit stops the search"
                                + " and returns the Wayguide.");
        ForgeConfigSpec.IntValue wayguideCacheTicks =
                integer(
                        builder,
                        "items.wayguide.cache.cacheTicks",
                        1200,
                        0,
                        72000,
                        "搜索结果的最长保留时间，单位：游戏刻；0 表示不保留。",
                        "Maximum search-result cache duration, in game ticks; 0 disables caching.");
        return new UtilityItemsDefinition(
                wayguideGuideDurationSeconds,
                wayguideRadius,
                wayguideTimeoutSeconds,
                wayguideBiomeSpacing,
                wayguideHeightSpacing,
                wayguideSamplesPerTick,
                wayguideSearchMillis,
                wayguideMaxSamples,
                wayguideCacheTicks);
    }
}

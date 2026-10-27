package com.thelongtravail.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;

public final class JourneyDetectionConfig {
    public static ForgeConfigSpec.IntValue HORIZONTAL, VERTICAL, BIOME_RADIUS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> PIECE_ONLY;

    static void define(ForgeConfigSpec.Builder b) {
        HORIZONTAL =
                b.comment(
                                "结构到访判定的水平容差，单位：方块；仅补查已加载的邻近区块。",
                                "Horizontal tolerance for structure visit checks, in blocks. Additional "
                                        + "checks use only neighboring chunks that are already loaded.")
                        .defineInRange("journeyDetection.structureHorizontalTolerance", 4, 0, 16);
        VERTICAL =
                b.comment(
                                "结构到访判定的垂直容差，单位：方块。",
                                "Vertical tolerance for structure visit checks, in blocks.")
                        .defineInRange("journeyDetection.structureVerticalTolerance", 4, 0, 16);
        BIOME_RADIUS =
                b.comment(
                                "向四个水平方向采样群系的距离，单位：方块；0 表示只检查当前位置。",
                                "Distance to sample biomes in the four cardinal directions, in blocks; 0 "
                                        + "checks only the current position.")
                        .defineInRange("journeyDetection.biomeSampleRadius", 4, 0, 8);
        PIECE_ONLY =
                b.comment(
                                "列表中的结构按各部件的范围加容差判定到访；其他结构按整体范围加容差判定。",
                                "For listed structures, visit checks use individual piece bounds plus "
                                        + "tolerance. Other structures use the overall bounds plus tolerance.")
                        .defineListAllowEmpty(
                                "journeyDetection.pieceOnlyStructures",
                                List.of(),
                                v ->
                                        v instanceof String s
                                                && net.minecraft.resources.ResourceLocation
                                                                .tryParse(s)
                                                        != null);
    }

    private JourneyDetectionConfig() {}
}

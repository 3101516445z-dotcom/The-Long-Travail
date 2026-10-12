package com.thelongtravail.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;

public final class JourneyDetectionConfig {
    public static ForgeConfigSpec.IntValue HORIZONTAL, VERTICAL, BIOME_RADIUS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> PIECE_ONLY;

    static void define(ForgeConfigSpec.Builder b) {
        HORIZONTAL =
                b.comment(
                                "结构水平到访容差，单位：方块。只补查已加载的邻近区块。",
                                "Horizontal structure visit tolerance in blocks; neighboring checks use loaded chunks only.")
                        .defineInRange("journeyDetection.structureHorizontalTolerance", 4, 0, 16);
        VERTICAL =
                b.comment("结构垂直到访容差，单位：方块。", "Vertical structure visit tolerance in blocks.")
                        .defineInRange("journeyDetection.structureVerticalTolerance", 4, 0, 16);
        BIOME_RADIUS =
                b.comment(
                                "群系四方向采样距离，单位：方块；0仅检查当前位置。",
                                "Cardinal biome sample distance in blocks; 0 checks only the current position.")
                        .defineInRange("journeyDetection.biomeSampleRadius", 4, 0, 8);
        PIECE_ONLY =
                b.comment(
                                "这些结构按部件范围加容差判定；其他结构按整体范围加容差判定。",
                                "These structure IDs use piece bounds plus tolerance; others use whole structure bounds plus tolerance.")
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

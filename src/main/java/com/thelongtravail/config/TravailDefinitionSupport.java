package com.thelongtravail.config;

import com.thelongtravail.TravailAspect;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Map;

// 定义期的范围、条目格式与主题要求池装配。
final class TravailDefinitionSupport {
    static void defineRequirements(
            ForgeConfigSpec.Builder builder,
            Map<TravailAspect, ForgeConfigSpec.ConfigValue<List<? extends String>>> biomePools,
            Map<TravailAspect, ForgeConfigSpec.ConfigValue<List<? extends String>>> structurePools,
            TravailAspect aspect,
            List<String> biomes,
            List<String> structures) {
        BiomeCompatDefaults.define(builder, aspect);
        StructureCompatDefaults.define(builder, aspect);
        biomePools.put(
                aspect,
                builder.comment(
                                "格式：群系ID|显示名称|正整数权重。例：minecraft:plains|平原|1。\n"
                                        + "Format: biome_id|display_name|positive_integer_weight."
                                        + " Example: minecraft:plains|Plains|1.")
                        .defineListAllowEmpty(
                                "travail.requirements.biomes",
                                biomes,
                                TravailDefinitionSupport::validRequirementEntry));
        structurePools.put(
                aspect,
                builder.comment(
                                "格式：结构ID|显示名称|正整数权重。例：minecraft:stronghold|要塞|1。\n"
                                        + "Format: structure_id|display_name|positive_integer_weight."
                                        + " Example: minecraft:stronghold|Stronghold|1.")
                        .defineListAllowEmpty(
                                "travail.requirements.structures",
                                structures,
                                TravailDefinitionSupport::validRequirementEntry));
    }

    static ForgeConfigSpec.IntValue integer(
            ForgeConfigSpec.Builder builder,
            String key,
            int value,
            int min,
            int max,
            String chineseComment,
            String englishComment) {
        return builder.comment(chineseComment, englishComment).defineInRange(key, value, min, max);
    }

    static ForgeConfigSpec.DoubleValue decimal(
            ForgeConfigSpec.Builder builder,
            String key,
            double value,
            double min,
            double max,
            String chineseComment,
            String englishComment) {
        return builder.comment(chineseComment, englishComment).defineInRange(key, value, min, max);
    }

    static boolean validRequirementEntry(Object value) {
        return value instanceof String string && string.split("\\|", -1).length >= 2;
    }

    static boolean validWeightedEntry(Object value) {
        return value instanceof String string && string.split("\\|", -1).length >= 2;
    }

    static boolean validEffectEntry(Object value) {
        return value instanceof String string && string.split("\\|", -1).length >= 3;
    }

    static boolean validRewardEntry(Object value) {
        return value instanceof String string && string.split("\\|", -1).length >= 4;
    }

    private TravailDefinitionSupport() {}
}

package com.thelongtravail.data;

import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

// 只迁移完全匹配的旧默认显示名，不覆盖整合包的自定义名称。
public final class RequirementNames {
    private static final Map<String, String> LEGACY_BIOMES = Map.ofEntries(
            Map.entry("minecraft:plains", "平原"),
            Map.entry("minecraft:forest", "森林"),
            Map.entry("minecraft:taiga", "针叶林"),
            Map.entry("minecraft:swamp", "沼泽"),
            Map.entry("minecraft:jungle", "丛林"),
            Map.entry("minecraft:savanna", "热带草原"),
            Map.entry("minecraft:deep_ocean", "暖水深海（使用深海实现）"),
            Map.entry("minecraft:warm_ocean", "暖水海洋"),
            Map.entry("minecraft:lukewarm_ocean", "温水海洋"),
            Map.entry("minecraft:deep_lukewarm_ocean", "温水深海"),
            Map.entry("minecraft:cold_ocean", "冷水海洋"),
            Map.entry("minecraft:deep_cold_ocean", "冷水深海"),
            Map.entry("minecraft:snowy_plains", "雪原"),
            Map.entry("minecraft:desert", "沙漠"),
            Map.entry("minecraft:badlands", "恶地"),
            Map.entry("minecraft:windswept_hills", "风袭丘陵"),
            Map.entry("minecraft:snowy_taiga", "积雪针叶林"),
            Map.entry("minecraft:ice_spikes", "冰刺之地"),
            Map.entry("minecraft:dripstone_caves", "溶洞"),
            Map.entry("minecraft:lush_caves", "繁茂洞穴"),
            Map.entry("minecraft:deep_dark", "深暗之域"),
            Map.entry("minecraft:dark_forest", "黑森林"),
            Map.entry("minecraft:mushroom_fields", "蘑菇岛"),
            Map.entry("minecraft:stony_shore", "石岸"),
            Map.entry("minecraft:nether_wastes", "下界荒地"),
            Map.entry("minecraft:soul_sand_valley", "灵魂沙峡谷"),
            Map.entry("minecraft:warped_forest", "诡异森林"),
            Map.entry("minecraft:basalt_deltas", "玄武岩三角洲"),
            Map.entry("minecraft:crimson_forest", "绯红森林"),
            Map.entry("minecraft:wooded_badlands", "繁茂恶地"),
            Map.entry("minecraft:end_midlands", "末地内陆"),
            Map.entry("minecraft:end_highlands", "末地高地"),
            Map.entry("minecraft:end_barrens", "末地荒地"),
            Map.entry("minecraft:the_end", "末地"),
            Map.entry("minecraft:small_end_islands", "末地小型岛屿"),
            Map.entry("minecraft:stony_peaks", "裸岩山峰"));

    public static String resolve(boolean structures, ResourceLocation id, String configured) {
        String key = translationKey(structures, id.toString(), configured);
        return key == null ? configured : Component.translatableWithFallback(key, id.toString()).getString();
    }

    public static String translationKey(boolean structures, String id, String configured) {
        if (configured.startsWith("@") && configured.length() > 1) return configured.substring(1);
        if (!structures && configured.equals(LEGACY_BIOMES.get(id))) {
            return id.equals("minecraft:deep_ocean") ? "biome.the_long_travail.warm_deep_ocean"
                    : "biome." + id.replace(':', '.');
        }
        return null;
    }

    private RequirementNames() {}
}

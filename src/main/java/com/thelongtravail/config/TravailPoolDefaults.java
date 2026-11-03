package com.thelongtravail.config;

import java.util.List;

// 各主题内置池条目；联动默认值由对应 CompatDefaults 装配。
final class TravailPoolDefaults {
    static List<String> defaultFarReachEffects() {
        return List.of(
                "minecraft:jump_boost|5|5",
                "minecraft:regeneration|5|3",
                "minecraft:resistance|4|2",
                "minecraft:haste|3|2",
                "minecraft:strength|3|2");
    }

    static List<String> defaultDeepValleyEffects() {
        return List.of(
                "minecraft:jump_boost|5|5",
                "minecraft:regeneration|5|3",
                "minecraft:resistance|4|2",
                "minecraft:haste|3|2",
                "minecraft:strength|3|2");
    }

    static List<String> defaultFishingEntities() {
        return List.of(
                "minecraft:pig|100",
                "minecraft:cow|100",
                "minecraft:sheep|100",
                "minecraft:villager|50",
                "minecraft:wandering_trader|50",
                "minecraft:cat|50",
                "minecraft:fox|25",
                "minecraft:axolotl|25",
                "minecraft:turtle|25");
    }

    static List<String> defaultFishingItems() {
        return List.of(
                "minecraft:apple|6|1|5",
                "minecraft:emerald|6|1|5",
                "minecraft:iron_ingot|6|1|3",
                "minecraft:gold_ingot|6|1|3",
                "minecraft:diamond|6|1|3",
                "minecraft:nautilus_shell|6|1|3",
                "minecraft:enchanted_golden_apple|3|1|1",
                "minecraft:netherite_ingot|2|1|1",
                "minecraft:nether_star|1|1|1",
                "minecraft:dragon_egg|1|1|1",
                "minecraft:netherite_scrap|3|1|3",
                "minecraft:echo_shard|3|1|3");
    }

    static List<String> defaultPhantomDrops() {
        return List.of(
                "minecraft:apple|6|1|5",
                "minecraft:emerald|6|1|5",
                "minecraft:iron_ingot|6|1|3",
                "minecraft:gold_ingot|6|1|3",
                "minecraft:diamond|6|1|3",
                "minecraft:nautilus_shell|6|1|3",
                "minecraft:enchanted_golden_apple|3|1|1",
                "minecraft:netherite_ingot|2|1|1",
                "minecraft:nether_star|1|1|1",
                "minecraft:dragon_egg|1|1|1",
                "minecraft:netherite_scrap|3|1|3",
                "minecraft:echo_shard|3|1|3");
    }

    private TravailPoolDefaults() {}
}

package travail.smoke;

import java.util.List;

// 旧版获取规则仅作为回归测试基准，正式运行时使用 UnifiedLootConfig。
public final class LegacyLootBaseline {
    public static final List<String> RING = List.of(
            "minecraft:chests/ancient_city|0.03",
            "minecraft:chests/nether_bridge|0.02");
    public static final List<String> BOOK = List.of(
            "minecraft:chests/desert_pyramid|0.02",
            "minecraft:chests/stronghold_library|0.03");
    public static final List<String> GOLD = List.of(
            "minecraft:chests/nether_bridge|0.025",
            "minecraft:chests/bastion_bridge|0.025",
            "minecraft:chests/bastion_hoglin_stable|0.025",
            "minecraft:chests/bastion_other|0.025",
            "minecraft:chests/bastion_treasure|0.025");
    public static final List<String> ICARUS = List.of(
            "minecraft:chests/end_city_treasure|0.05",
            "minecraft:chests/desert_pyramid|0.03");
    public static final List<String> AFFECTION = List.of(
            "minecraft:chests/village/village_armorer|0.02|1|1",
            "minecraft:chests/village/village_butcher|0.02|1|1",
            "minecraft:chests/village/village_cartographer|0.02|1|1",
            "minecraft:chests/village/village_desert_house|0.02|1|1",
            "minecraft:chests/village/village_fisher|0.02|1|1",
            "minecraft:chests/village/village_fletcher|0.02|1|1",
            "minecraft:chests/village/village_mason|0.02|1|1",
            "minecraft:chests/village/village_plains_house|0.02|1|1",
            "minecraft:chests/village/village_savanna_house|0.02|1|1",
            "minecraft:chests/village/village_shepherd|0.02|1|1",
            "minecraft:chests/village/village_snowy_house|0.02|1|1",
            "minecraft:chests/village/village_taiga_house|0.02|1|1",
            "minecraft:chests/village/village_tannery|0.02|1|1",
            "minecraft:chests/village/village_temple|0.02|1|1",
            "minecraft:chests/village/village_toolsmith|0.02|1|1",
            "minecraft:chests/village/village_weaponsmith|0.02|1|1",
            "minecraft:chests/jungle_temple|0.05|1|1");
    public static final List<String> AZRAEL = List.of(
            "minecraft:chests/ancient_city|0.025|1|1",
            "minecraft:chests/abandoned_mineshaft|0.025|1|1",
            "minecraft:chests/stronghold_corridor|0.025|1|1",
            "minecraft:chests/stronghold_crossing|0.025|1|1",
            "minecraft:chests/stronghold_library|0.025|1|1");
    public static final List<String> RAIN = List.of(
            "minecraft:chests/buried_treasure|0.15|1|1",
            "minecraft:gameplay/fishing|0.05|1|1");
    public static final List<String> BOUNDLESS = List.of(
            "minecraft:chests/stronghold_corridor|0.1|0.02",
            "minecraft:chests/stronghold_crossing|0.1|0.02",
            "minecraft:chests/stronghold_library|0.1|0.02",
            "minecraft:chests/end_city_treasure|0.1|0.05",
            "minecraft:chests/village/village_armorer|0.02|0",
            "minecraft:chests/village/village_butcher|0.02|0",
            "minecraft:chests/village/village_cartographer|0.02|0",
            "minecraft:chests/village/village_desert_house|0.02|0",
            "minecraft:chests/village/village_fisher|0.02|0",
            "minecraft:chests/village/village_fletcher|0.02|0",
            "minecraft:chests/village/village_mason|0.02|0",
            "minecraft:chests/village/village_plains_house|0.02|0",
            "minecraft:chests/village/village_savanna_house|0.02|0",
            "minecraft:chests/village/village_shepherd|0.02|0",
            "minecraft:chests/village/village_snowy_house|0.02|0",
            "minecraft:chests/village/village_taiga_house|0.02|0",
            "minecraft:chests/village/village_tannery|0.02|0",
            "minecraft:chests/village/village_temple|0.02|0",
            "minecraft:chests/village/village_toolsmith|0.02|0",
            "minecraft:chests/village/village_weaponsmith|0.02|0");
    public static final int EKI_WEIGHT = 1, TOKAIDO_WEIGHT = 1;
    private LegacyLootBaseline() {}
}

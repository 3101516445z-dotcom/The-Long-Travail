package com.thelongtravail.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class UnderworldItemsConfig {
    public static ForgeConfigSpec.DoubleValue WITHER_BONUS, KILL_THRESHOLD;
    public static ForgeConfigSpec.IntValue PROTECTION, SOUL_CAPACITY;

    public static final java.util.Map<String, ForgeConfigSpec.DoubleValue> RELICS =
            new java.util.LinkedHashMap<>();

    private static void relic(ForgeConfigSpec.Builder b, String key, double value, double max) {
        String description =
                switch (key) {
                    case "king.burningBonus" ->
                            "冥府之恶意下，对燃烧目标造成的伤害加成比例；0.15 表示增加 15%。\n"
                                + "Under Underworld Malice, fractional damage bonus against burning"
                                + " targets; 0.15 means +15%.";
                    case "king.lifesteal" ->
                            "冥府之恶意下，按目标实际损失生命值恢复自身生命的比例；0.05 表示 5%。见切反伤不触发。\n"
                                + "Under Underworld Malice, heal for this fraction of the target's"
                                + " actual health loss; 0.05 means 5%. Insight retaliation does not"
                                + " trigger this healing.";
                    case "king.undeadReduction" ->
                            "冥府之恶意下，来自亡灵生物的伤害减免比例；0.2 表示减免 20%。绕过无敌的伤害除外。\n"
                                    + "Under Underworld Malice, fractional reduction of damage from"
                                    + " undead attackers; 0.2 means 20%. Damage bypassing"
                                    + " invulnerability is excluded.";
                    case "king.overlord" ->
                            "冥府之见证下，增加的霸主属性值；直接加值，0.1 表示增加 0.1。\n"
                                + "Under Underworld Witness, flat Overlord attribute increase; 0.1"
                                + " adds 0.1.";
                    case "king.environmentBonus" ->
                            "冥府之见证下，位于下界标签群系或非露天位置时的伤害加成比例；0.1 表示增加 10%。\n"
                                    + "Under Underworld Witness, fractional damage bonus in a"
                                    + " Nether-tagged biome or without open sky; 0.1 means +10%.";
                    case "banner.maliceCriticalPerSoul" ->
                            "冥府之恶意下，每点有效魂数增加的暴击率；0.00002 表示增加 0.002 个百分点。\n"
                                + "Under Underworld Malice, critical-chance increase per effective"
                                + " soul; 0.00002 adds 0.002 percentage points.";
                    case "banner.witnessCriticalPerSoul" ->
                            "冥府之见证下，每点有效魂数增加的暴击率；0.00001 表示增加 0.001 个百分点。\n"
                                + "Under Underworld Witness, critical-chance increase per effective"
                                + " soul; 0.00001 adds 0.001 percentage points.";
                    case "banner.insightPerSoul" ->
                            "冥府之见证下，每点有效魂数增加的见切属性值；总加值受 insightCap 限制。\n"
                                + "Under Underworld Witness, flat Insight increase per effective"
                                + " soul, limited in total by insightCap.";
                    case "banner.insightCap" ->
                            "万魂幡提供的见切属性加值上限；10 表示最多增加 10 点见切。\n"
                                + "Maximum flat Insight increase supplied by the banner; 10 allows"
                                + " up to +10 Insight.";
                    default -> throw new IllegalArgumentException(key);
                };
        RELICS.put(
                key,
                b.comment(description)
                        .defineInRange(
                                "items.relics." + key,
                                value,
                                key.equals("banner.capacity") ? 1 : 0,
                                max));
    }

    public static void define(ForgeConfigSpec.Builder b) {
        SunBladeConfig.blade(b);
        relic(b, "king.burningBonus", .15, 1000);
        relic(b, "king.lifesteal", .05, 1);
        relic(b, "king.undeadReduction", .20, 1);
        relic(b, "king.overlord", .10, 1);
        relic(b, "king.environmentBonus", .10, 1000);
        SOUL_CAPACITY =
                b.comment(
                                "万魂幡的容量上限；调低后只限制实际生效的魂数，不删除已储存的魂。\n"
                                    + "Maximum soul capacity of Myriad Souls Banner. Lowering it"
                                    + " limits the number of souls that take effect without"
                                    + " deleting stored souls.")
                        .defineInRange("items.relics.banner.capacity", 10000, 1, 1000000000);
        relic(b, "banner.maliceCriticalPerSoul", .00002, 1);
        relic(b, "banner.witnessCriticalPerSoul", .00001, 1);
        relic(b, "banner.insightPerSoul", .001, 1000000);
        relic(b, "banner.insightCap", 10, 1000000);
        WITHER_BONUS =
                b.comment(
                                "一千年后：每级凋零提供的伤害加成比例；0.15表示提高15%。此加成与本模组其他同类伤害加成相加。\n"
                                    + "A Thousand Years Later: fractional damage bonus per Wither"
                                    + " level; 0.15 means +15%. Adds to other damage bonuses of the"
                                    + " same kind from this mod.")
                        .defineInRange("items.thousandYears.witherBonusPerLevel", .15, 0, 1000000);
        KILL_THRESHOLD =
                b.comment(
                                "死者之书：generic_kill伤害在减免前达到此值时，无法触发死者苏生与冥府之见证。\n"
                                    + "判断依据为传入的伤害值，而非减免后的伤害值。\n"
                                    + "Book of the Dead: generic_kill damage at or above this value"
                                    + " before reductions prevents Resurrection and Underworld"
                                    + " Witness from triggering.\n"
                                    + "Uses the incoming damage value, not the value after"
                                    + " reductions.")
                        .defineInRange("items.book.killThreshold", 10000000D, 1, Double.MAX_VALUE);
        PROTECTION =
                b.comment(
                                "死者苏生后获得的原版出生保护持续时间，单位：游戏刻；默认60游戏刻，相当于3秒。原版中可绕过出生保护的伤害仍可生效。\n"
                                    + "Vanilla spawn protection duration after Resurrection, in"
                                    + " game ticks; the default 60 ticks equals 3 seconds. Damage"
                                    + " that bypasses vanilla spawn protection still applies.")
                        .defineInRange("items.book.spawnProtectionTicks", 60, 0, 12000);
    }

    private UnderworldItemsConfig() {}
}

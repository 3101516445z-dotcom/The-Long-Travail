package com.thelongtravail.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class UnderworldItemsConfig {
    public static ForgeConfigSpec.DoubleValue WITHER_BONUS, KILL_THRESHOLD;
    public static ForgeConfigSpec.IntValue PROTECTION, SOUL_CAPACITY;

    public static final java.util.Map<String, ForgeConfigSpec.DoubleValue> RELICS =
            new java.util.LinkedHashMap<>();

    private static void relic(ForgeConfigSpec.Builder b, String key, double value, double max) {
        RELICS.put(
                key,
                b.comment(
                                "冥府饰品的数值设置；比例设为 0 时不提供对应加成，容量须为正整数。\nUnderworld accessory setting. A fractional bonus of 0 disables that bonus; capacity must be a positive integer.")
                        .defineInRange(
                                "items.relics." + key,
                                value,
                                key.equals("banner.capacity") ? 1 : 0,
                                max));
    }

    public static void define(ForgeConfigSpec.Builder b) {
        relic(b, "king.burningBonus", .15, 1000);
        relic(b, "king.lifesteal", .05, 1);
        relic(b, "king.undeadReduction", .20, 1);
        relic(b, "king.overlord", .10, 1);
        relic(b, "king.environmentBonus", .10, 1000);
        SOUL_CAPACITY =
                b.comment(
                                "万魂幡的容量上限；调低后只限制实际生效的魂数，不删除已储存的魂。\nMaximum soul capacity of Myriad Souls Banner. Lowering it limits the number of souls that take effect without deleting stored souls.")
                        .defineInRange("items.relics.banner.capacity", 10000, 1, 1000000000);
        relic(b, "banner.maliceCriticalPerSoul", .00002, 1);
        relic(b, "banner.witnessCriticalPerSoul", .00001, 1);
        relic(b, "banner.insightPerSoul", .001, 1000000);
        relic(b, "banner.insightCap", 10, 1000000);
        WITHER_BONUS =
                b.comment(
                                "一千年后：每级凋零提供的伤害加成比例；0.15表示提高15%。此加成与本模组其他同类伤害加成相加。\nA Thousand Years Later: fractional damage bonus per Wither level; 0.15 means +15%. Adds to other damage bonuses of the same kind from this mod.")
                        .defineInRange("items.thousandYears.witherBonusPerLevel", .15, 0, 1000000);
        KILL_THRESHOLD =
                b.comment(
                                "死者之书：generic_kill伤害在减免前达到此值时，无法触发死者苏生与冥府之见证。\n判断依据为传入的伤害值，而非减免后的伤害值。\nBook of the Dead: generic_kill damage at or above this value before reductions prevents Resurrection and Underworld Witness from triggering.\nUses the incoming damage value, not the value after reductions.")
                        .defineInRange("items.book.killThreshold", 10000000D, 1, Double.MAX_VALUE);
        PROTECTION =
                b.comment(
                                "死者苏生后获得的原版出生保护持续时间，单位：游戏刻；默认60游戏刻，相当于3秒。原版中可绕过出生保护的伤害仍可生效。\nVanilla spawn protection duration after Resurrection, in game ticks; the default 60 ticks equals 3 seconds. Damage that bypasses vanilla spawn protection still applies.")
                        .defineInRange("items.book.spawnProtectionTicks", 60, 0, 12000);
    }

    private UnderworldItemsConfig() {}
}

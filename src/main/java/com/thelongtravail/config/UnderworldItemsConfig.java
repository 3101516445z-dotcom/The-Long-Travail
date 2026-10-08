package com.thelongtravail.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class UnderworldItemsConfig {
    public static ForgeConfigSpec.DoubleValue WITHER_BONUS, KILL_THRESHOLD;
    public static ForgeConfigSpec.IntValue PROTECTION;
    public static void define(ForgeConfigSpec.Builder b) {
        WITHER_BONUS = b.comment("一千年后：每级凋零提供的伤害加成比例；0.1表示提高10%。此加成与本模组其他同类伤害加成相加。\nA Thousand Years Later: fractional damage bonus per Wither level; 0.1 means +10%. Adds to other damage bonuses of the same kind from this mod.")
                .defineInRange("items.thousandYears.witherBonusPerLevel", .1, 0, 1000000);
        KILL_THRESHOLD = b.comment("死者之书：generic_kill伤害在减免前达到此值时，无法触发死者苏生与冥府之见证。判断依据为传入的伤害值，而非减免后的伤害值。\nBook of the Dead: generic_kill damage at or above this value before reductions prevents Resurrection and Underworld Witness from triggering. Uses the incoming damage value, not the value after reductions.")
                .defineInRange("items.book.killThreshold", 10000000D, 1, Double.MAX_VALUE);
        PROTECTION = b.comment("死者苏生后获得的原版出生保护持续时间，单位：游戏刻；默认60游戏刻，相当于3秒。原版中可绕过出生保护的伤害仍可生效。\nVanilla spawn protection duration after Resurrection, in game ticks; the default 60 ticks equals 3 seconds. Damage that bypasses vanilla spawn protection still applies.")
                .defineInRange("items.book.spawnProtectionTicks", 60, 0, 12000);
    }
    private UnderworldItemsConfig() {}
}

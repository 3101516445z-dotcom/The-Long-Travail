package com.thelongtravail.valley;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SwordLanternConfig {
    public static ForgeConfigSpec.DoubleValue MEMORY, TIMEOUT, DURATION, BONUS, HEALTH;
    public static void define(ForgeConfigSpec.Builder b) {
        b.push("items.swordAndLantern");
        MEMORY = b.comment("与敌人发生有效伤害交互后，保留该记录的时间，单位：秒。判断敌我关系时，优先按队友身份处理。\nHow long to retain a record of an effective damage interaction with an enemy, in seconds. Teammate status takes precedence when determining friend or foe.")
                .defineInRange("enemyMemorySeconds",20D,0D,1_000_000D);
        TIMEOUT = b.comment("连续未发生有效伤害交互的时间达到此值时，结束战斗状态并重置持续战斗计时，单位：秒。\nTime without an effective damage interaction before combat ends and the continuous-combat timer resets, in seconds.")
                .defineInRange("combatTimeoutSeconds",20D,0D,1_000_000D);
        DURATION = b.comment("持续战斗时间超过此值时，获得久战伤害加成，单位：秒。卸下饰品后，持续战斗计时重置。\nContinuous combat must last longer than this duration to grant the prolonged-combat damage bonus, in seconds. Unequipping the accessory resets the timer.")
                .defineInRange("prolongedCombatSeconds",300D,0D,1_000_000D);
        BONUS = b.comment("每项条件的增伤比例，五项相加；0.10表示10%。\nFractional damage bonus per condition. All five bonuses add together; 0.10 means 10%.")
                .defineInRange("bonusPerCondition",.10D,0D,100D);
        HEALTH = b.comment("佩戴者当前生命值占最大生命值的比例低于此值时，获得伤害加成；计算时不计入吸收生命值。\nGrants a damage bonus while the wearer's current health as a fraction of maximum health is below this value. Absorption is not included.")
                .defineInRange("lowHealthThreshold",.50D,0D,1D);
        b.pop();
    }
    public static long ticks(ForgeConfigSpec.DoubleValue value) { return (long)Math.ceil(value.get()*20); }
    private SwordLanternConfig() {}
}

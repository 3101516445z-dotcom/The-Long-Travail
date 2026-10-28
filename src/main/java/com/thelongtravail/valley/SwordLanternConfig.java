package com.thelongtravail.valley;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SwordLanternConfig {
    public static ForgeConfigSpec.DoubleValue MEMORY,
            TIMEOUT,
            DURATION,
            HEALTH,
            CROWD_BONUS,
            UNDEAD_BONUS,
            ALONE_BONUS,
            PROLONGED_BONUS,
            LOW_HEALTH_BONUS;

    public static ForgeConfigSpec.IntValue CROWD_MINIMUM;
    public static ForgeConfigSpec.DoubleValue RADIUS;

    public static void define(ForgeConfigSpec.Builder b) {
        b.push("items.swordAndLantern");
        RADIUS =
                b.comment(
                                "周围实体的判定半径，单位：格；同时用于敌人、亡灵与独行判定。",
                                "Nearby-entity radius in blocks, shared by enemy, undead and alone checks.")
                        .defineInRange("detectionRadius", 64D, 8D, 64D);
        CROWD_MINIMUM =
                b.comment(
                                "触发围攻伤害加成所需的最低敌人数，包含正在攻击的有效目标。",
                                "Minimum enemy count for the crowd damage bonus, including an eligible"
                                        + " target of the current attack.")
                        .defineInRange("crowdMinimumEnemies", 11, 2, 64);
        MEMORY =
                b.comment(
                                "受到敌人攻击或对敌人实际造成伤害后，保留该记录的时间，单位：秒。判断敌我关系时，优先按队友身份处理。\nHow long to retain a "
                                        + "record of receiving an enemy attack or actually damaging an enemy, in "
                                        + "seconds. Teammate status takes precedence when determining friend or foe.")
                        .defineInRange("enemyMemorySeconds", 20D, 0D, 1_000_000D);
        TIMEOUT =
                b.comment(
                                "连续未受击且未实际造成伤害的时间达到此值时，结束战斗状态并重置持续战斗计时，单位：秒。\nTime without receiving an "
                                        + "attack or actually dealing damage before combat ends and the "
                                        + "continuous-combat timer resets, in seconds.")
                        .defineInRange("combatTimeoutSeconds", 20D, 0D, 1_000_000D);
        DURATION =
                b.comment(
                                "持续战斗时间超过此值时，获得久战伤害加成，单位：秒。卸下饰品后，持续战斗计时重置。\nContinuous combat must last "
                                        + "longer than this duration to grant the prolonged-combat damage bonus, in "
                                        + "seconds. Unequipping the accessory resets the timer.")
                        .defineInRange("prolongedCombatSeconds", 300D, 0D, 1_000_000D);
        CROWD_BONUS =
                bonus(b, "crowdBonus", .40, "敌人数达到围攻阈值", "Enemy count reaches the crowd threshold");
        UNDEAD_BONUS = bonus(b, "undeadBonus", .15, "存在亡灵敌人", "Undead enemy present");
        ALONE_BONUS = bonus(b, "aloneBonus", .10, "独行", "Fighting alone");
        PROLONGED_BONUS = bonus(b, "prolongedBonus", .50, "持续战斗超过阈值", "Prolonged combat");
        LOW_HEALTH_BONUS = bonus(b, "lowHealthBonus", .25, "生命低于阈值", "Low health");
        HEALTH =
                b.comment(
                                "佩戴者当前生命值占最大生命值的比例低于此值时，获得伤害加成；计算时不计入吸收生命值。\nGrants a damage bonus while "
                                        + "the wearer's current health as a fraction of maximum health is below this "
                                        + "value. Absorption is not included.")
                        .defineInRange("lowHealthThreshold", .50D, 0D, 1D);
        b.pop(2);
    }

    private static ForgeConfigSpec.DoubleValue bonus(
            ForgeConfigSpec.Builder b,
            String key,
            double value,
            String condition,
            String englishCondition) {
        return b.comment(
                        condition
                                + "时，造成的伤害增加的比例；各项加成相加。\n"
                                + englishCondition
                                + ": fractional damage bonus while this condition is met; adds to the bonuses "
                                + "from other conditions.")
                .defineInRange(key, value, 0D, 100D);
    }

    public static double damageBonus(
            boolean crowd, boolean undead, boolean alone, boolean prolonged, boolean lowHealth) {
        return (crowd ? CROWD_BONUS.get() : 0)
                + (undead ? UNDEAD_BONUS.get() : 0)
                + (alone ? ALONE_BONUS.get() : 0)
                + (prolonged ? PROLONGED_BONUS.get() : 0)
                + (lowHealth ? LOW_HEALTH_BONUS.get() : 0);
    }

    public static double radiusSquared() {
        double radius = RADIUS.get();
        return radius * radius;
    }

    public static long ticks(ForgeConfigSpec.DoubleValue value) {
        return (long) Math.ceil(value.get() * 20);
    }

    private SwordLanternConfig() {}
}

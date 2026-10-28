package com.thelongtravail.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class FlourishingGrowthConfig {
    public static ForgeConfigSpec.IntValue MAX_LEVEL, RADIUS, HEIGHT, INTERVAL, RESISTANCE_LEVEL;
    public static ForgeConfigSpec.BooleanValue GROWTH, DEFAULT_GROWTH;

    public static void define(ForgeConfigSpec.Builder b) {
        MAX_LEVEL =
                b.comment("槲寄生的最高等级；每次使用提升一级。\nMaximum Mistletoe level; each use adds one level.")
                        .defineInRange("items.mistletoe.maxLevel", 10, 1, 256);
        number(
                b,
                "mistletoe.durationSeconds",
                45,
                .05,
                1000000,
                "每次使用槲寄生增加的持续时间，单位：秒；首次使用也采用此时长。\nDuration added by each use of Mistletoe, "
                        + "in seconds. Also used for the first application.");
        number(
                b,
                "mistletoe.speedBonusPerLevel",
                .05,
                0,
                1000,
                "每级槲寄生提供的移动速度加成比例。\nFractional movement speed bonus per Mistletoe level.");
        number(
                b,
                "mistletoe.damageBonusPerLevel",
                .2,
                0,
                1000,
                "每级槲寄生提供的伤害加成比例。\nFractional damage bonus per Mistletoe level.");
        number(
                b,
                "mistletoe.damageReductionPerLevel",
                .1,
                0,
                1,
                "每级槲寄生提供的伤害减免比例；总减免比例最高为 100%。\nFractional damage reduction per Mistletoe "
                        + "level, capped at 100% in total.");
        number(
                b,
                "mistletoe.expiryDamagePerLevel",
                .05,
                0,
                1,
                "槲寄生到期时，每级效果造成的魔法伤害占最大生命值的比例。\nMagic damage dealt when Mistletoe expires, "
                        + "per effect level, as a fraction of maximum health.");
        number(
                b,
                "mistletoe.lootChancePerLevel",
                .1,
                0,
                1,
                "每级槲寄生增加的整套战利品掉落概率；总概率最高为 100%。\nChance per Mistletoe level to drop the "
                        + "full loot table, capped at 100% in total.");
        GROWTH =
                b.comment(
                                "是否允许无穷花在服务端催生植物。\nAllow Everlasting Flower to accelerate plant growth on the server.")
                        .define("items.everlasting_flower.growth.enabled", true);
        DEFAULT_GROWTH =
                b.comment(
                                "尚未保存催生开关状态的物品，是否默认开启催生。\nEnable growth acceleration by default for stacks "
                                        + "that have no saved toggle state.")
                        .define("items.everlasting_flower.growth.defaultEnabled", true);
        RADIUS =
                b.comment(
                                "催生范围的水平圆形半径，单位：方块。\nHorizontal radius of the circular growth area, in blocks.")
                        .defineInRange(
                                "items.everlasting_flower.growth.horizontalRadius", 8, 1, 32);
        HEIGHT =
                b.comment(
                                "催生范围从中心向上、向下各延伸的距离，单位：方块。\nDistance the growth area extends above and "
                                        + "below its center, in blocks.")
                        .defineInRange("items.everlasting_flower.growth.verticalRadius", 4, 0, 16);
        INTERVAL =
                b.comment(
                                "对同一植物执行额外随机刻的最短间隔，单位：游戏刻。处理预算不足时降低执行频率，不补发跳过的随机刻。\nMinimum interval "
                                        + "between extra random ticks for the same plant, in game ticks. If the "
                                        + "processing budget is exhausted, ticks occur less often; skipped ticks are "
                                        + "not made up later.")
                        .defineInRange(
                                "items.everlasting_flower.growth.intervalTicks", 20, 1, 1200000);
        number(
                b,
                "everlasting_flower.malice.speedBonus",
                .15,
                0,
                1000,
                "位于花卉群系时，移动速度的加成比例。\nFractional movement speed bonus in flower biomes.");
        number(
                b,
                "everlasting_flower.malice.damageReduction",
                .3,
                0,
                1,
                "位于花卉群系时，受到的伤害减免比例。\nFractional damage reduction in flower biomes.");
        number(
                b,
                "everlasting_flower.malice.resistanceChance",
                .2,
                0,
                1,
                "实际造成伤害后，获得抗性提升效果的概率。\nChance to gain Resistance after actually dealing damage.");
        number(
                b,
                "everlasting_flower.malice.resistanceSeconds",
                10,
                .05,
                1000000,
                "抗性提升效果的持续时间，单位：秒。\nDuration of the Resistance effect, in seconds.");
        RESISTANCE_LEVEL =
                b.comment(
                                "抗性提升效果显示的等级；1 表示抗性提升 I。\nDisplayed Resistance level; 1 means Resistance I.")
                        .defineInRange("items.everlasting_flower.malice.resistanceLevel", 1, 1, 5);
        number(
                b,
                "everlasting_flower.witness.damageBonus",
                .20,
                0,
                1000,
                "位于花卉群系时，造成的伤害加成比例。\nFractional damage bonus in flower biomes.");
        number(
                b,
                "everlasting_flower.witness.repeatChance",
                .2,
                0,
                1,
                "触发追加伤害的概率；每次续接均使用此概率。\nChance to trigger an extra hit, also used for each "
                        + "subsequent hit in the chain.");
    }

    private static void number(
            ForgeConfigSpec.Builder b,
            String key,
            double value,
            double min,
            double max,
            String comment) {
        FlourishingItemsConfig.NUMBERS.put(
                key, b.comment(comment).defineInRange("items." + key, value, min, max));
    }

    public static double mistletoe(String key) {
        return FlourishingItemsConfig.get("mistletoe." + key);
    }

    public static double flower(String key) {
        return FlourishingItemsConfig.get("everlasting_flower." + key);
    }

    private FlourishingGrowthConfig() {}
}

package com.thelongtravail.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.LinkedHashMap;
import java.util.Map;

/** 两件物品的参数仍分别归入对应主题文件。 */
public final class SunBladeConfig {
    public static final Map<String, ForgeConfigSpec.DoubleValue> VALUES = new LinkedHashMap<>();

    public static void sun(ForgeConfigSpec.Builder b) {
        define(b, "sun_chaser.projectileBonus", .50, 100, "弹射物增伤比例 / Projectile damage bonus");
        define(b, "sun_chaser.burningBonus", .30, 100, "燃烧目标增伤比例 / Burning target damage bonus");
        define(
                b,
                "sun_chaser.healing",
                1,
                1000000,
                "岩浆中每秒恢复生命值 / Health restored per second in lava");
        define(
                b,
                "sun_chaser.suppressionSeconds",
                10,
                3600,
                "禁疗与受伤冷却抑制秒数 / Healing and hurt cooldown suppression seconds");
        define(
                b,
                "sun_chaser.strikeFraction",
                .01,
                1,
                "追加直击占目标最大生命比例，不再增伤或必杀 / Additional direct strike fraction of target maximum"
                        + " health; no bonus or critical");
    }

    public static void blade(ForgeConfigSpec.Builder b) {
        define(
                b,
                "kagotsurube.witherSeconds",
                5,
                3600,
                "每笔实际伤害延长凋零秒数 / Wither seconds added per confirmed damage");
        define(b, "kagotsurube.insight", 15, 1000000, "恶意见切加值 / Malice flat Insight bonus");
        define(b, "kagotsurube.overlord", .1, 100, "恶意霸主加值 / Malice flat Overlord bonus");
    }

    private static void define(
            ForgeConfigSpec.Builder b, String key, double value, double max, String text) {
        VALUES.put(
                key,
                b.comment(text + "\n0禁用该数值效果 / 0 disables this numeric effect")
                        .defineInRange("items." + key, value, 0, max));
    }

    public static double value(String key) {
        return VALUES.get(key).get();
    }

    public static int ticks(String key) {
        return (int) Math.ceil(value(key) * 20);
    }

    private SunBladeConfig() {}
}

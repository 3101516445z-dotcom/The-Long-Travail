package com.thelongtravail.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.LinkedHashMap;
import java.util.Map;

// 两件物品的参数仍分别归入对应主题文件。
public final class SunBladeConfig {
    public static final Map<String, ForgeConfigSpec.DoubleValue> VALUES = new LinkedHashMap<>();

    public static void sun(ForgeConfigSpec.Builder b) {
        define(
                b,
                "sun_chaser.projectileBonus",
                .50,
                100,
                "造成的弹射物伤害提高的比例；0.5 表示提高 50%。\nFractional projectile damage bonus; 0.5 means 50%.");
        define(
                b,
                "sun_chaser.burningBonus",
                .30,
                100,
                "对正在燃烧的目标造成伤害时，额外提高的伤害比例；0.3 表示提高 30%。\n"
                        + "Additional damage bonus against burning targets; 0.3 means 30%.");
        define(
                b,
                "sun_chaser.healing",
                1,
                1000000,
                "身处熔岩时，每秒恢复的生命值；2 点生命值为 1 颗心。\n"
                        + "Health restored per second in lava; 2 health points equal 1 heart.");
        define(
                b,
                "sun_chaser.suppressionSeconds",
                10,
                3600,
                "禁止目标治疗并抑制其受伤冷却的持续时间，单位：秒。\n"
                        + "Duration of healing and hurt cooldown suppression, in seconds.");
        define(
                b,
                "sun_chaser.strikeFraction",
                .01,
                1,
                "追加直击伤害占目标最大生命值的比例；0.01 表示 1%。这部分伤害不再获得增伤或必杀。\n"
                        + "Additional direct strike fraction of target maximum health; no bonus or"
                        + " critical");
    }

    public static void blade(ForgeConfigSpec.Builder b) {
        define(
                b,
                "kagotsurube.witherSeconds",
                5,
                3600,
                "每次实际造成伤害时，为凋零增加的持续时间，单位：秒。\nWither duration added per confirmed hit, in seconds.");
        define(
                b,
                "kagotsurube.insight",
                15,
                1000000,
                "冥府恶意状态下，增加的见切属性点数；15 表示增加 15 点。\n"
                        + "Flat Insight bonus with Underworld Malice; 15 adds 15 points.");
        define(
                b,
                "kagotsurube.overlord",
                .1,
                100,
                "冥府恶意状态下，增加的霸主属性点数；0.1 表示增加 0.1 点，不是提高 10%。\n"
                        + "Flat Overlord bonus with Underworld Malice; 0.1 adds 0.1 points, not 10%.");
    }

    private static void define(
            ForgeConfigSpec.Builder b, String key, double value, double max, String text) {
        VALUES.put(
                key,
                b.comment(text + "\n设为 0 时，不提供此项效果。\n0 disables this numeric effect.")
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

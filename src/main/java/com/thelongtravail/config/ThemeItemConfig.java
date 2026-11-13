package com.thelongtravail.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.LinkedHashMap;
import java.util.Map;

// 六主题物品参数分别写入各主题配置文件。
public final class ThemeItemConfig {
    public static final Map<String, ForgeConfigSpec.ConfigValue<? extends Number>> VALUES =
            new LinkedHashMap<>();

    public static void define(Map<String, ForgeConfigSpec.Builder> builders) {
        var b = builders.get("aspects/flourishing.toml");
        food(b, "moon_toad_pastry", 10, 10);
        effect(b, "moon_toad_pastry", "magic", 15, 2);
        effect(b, "moon_toad_pastry", "resistance", 15, 2);
        effect(b, "moon_toad_pastry", "saturation", 90, 1);
        effect(b, "moon_toad_pastry", "luck", 120, 2);
        b = builders.get("aspects/abyss.toml");
        number(
                b,
                "sea_in_a_bottle.healingBonus",
                .25,
                0,
                100,
                "受治疗量增幅（0.25 = 25%） / Received healing bonus (0.25 = 25%)");
        number(
                b,
                "sea_in_a_bottle.magicBonus",
                .3,
                0,
                100,
                "魔法类伤害增幅（0.3 = 30%） / Magic-class damage bonus (0.3 = 30%)");
        integer(
                b,
                "sea_in_a_bottle.extraSlots",
                1,
                0,
                64,
                "额外饰品栏位数量（整数） / Extra curio slots (integer)");
        b = builders.get("aspects/far_reach.toml");
        number(
                b,
                "horizon_theory.damageCap",
                .4,
                0,
                1,
                "单次最终扣血上限，占最大生命比例 / Final health loss cap as a fraction of maximum health");
        number(
                b,
                "horizon_theory.invulnerabilitySeconds",
                1.5,
                0,
                3600,
                "独立无敌帧时长，秒 / Independent invulnerability duration in seconds");
        b = builders.get("aspects/deep_valley.toml");
        number(b, "last_affairs.holdSeconds", 1, .05, 3600, "长按使用时间，秒 / Hold duration in seconds");
        number(
                b,
                "last_affairs.revivalSeconds",
                7.5,
                0,
                1000000,
                "还魂持续时间，秒 / Revival duration in seconds");
        b = builders.get("aspects/underworld.toml");
        food(b, "living_flesh", 6, 8);
        effect(b, "living_flesh", "physical", 15, 2);
        effect(b, "living_flesh", "wither", 30, 1);
        effect(b, "living_flesh", "speed", 30, 2);
        number(
                b,
                "living_flesh.maraSeconds",
                15,
                0,
                1000000,
                "波旬变生持续时间，秒 / Mara Transformation duration in seconds");
        b = builders.get("aspects/boundless.toml");
        number(
                b,
                "holy_fool.durationSeconds",
                15,
                0,
                1000000,
                "我于万物之中持续时间，秒 / Among All Things duration in seconds");
        number(
                b,
                "holy_fool.cooldownSeconds",
                90,
                0,
                1000000,
                "技能冷却时间，秒 / Skill cooldown in seconds");
    }

    private static void food(ForgeConfigSpec.Builder b, String id, int nutrition, int saturation) {
        integer(
                b,
                id + ".nutrition",
                nutrition,
                0,
                1000,
                "恢复饱食度（整数） / Nutrition restored (integer)");
        number(b, id + ".saturation", saturation, 0, 1000, "恢复饱和度点数 / Saturation points restored");
    }

    private static void effect(
            ForgeConfigSpec.Builder b, String id, String effect, double seconds, int level) {
        number(
                b,
                id + "." + effect + "Seconds",
                seconds,
                0,
                1000000,
                "效果持续时间，秒 / Effect duration in seconds");
        integer(
                b,
                id + "." + effect + "Level",
                level,
                1,
                256,
                "效果等级（整数，1为Ⅰ） / Effect level (integer, 1 means I)");
    }

    private static void number(
            ForgeConfigSpec.Builder b,
            String key,
            double value,
            double min,
            double max,
            String comment) {
        VALUES.put(key, b.comment(comment).defineInRange("items." + key, value, min, max));
    }

    private static void integer(
            ForgeConfigSpec.Builder b, String key, int value, int min, int max, String comment) {
        VALUES.put(key, b.comment(comment).defineInRange("items." + key, value, min, max));
    }

    public static double get(String key) {
        return VALUES.get(key).get().doubleValue();
    }

    public static int ticks(String key) {
        return (int) Math.round(get(key) * 20);
    }

    public static double display(String key) {
        return com.thelongtravail.network.TooltipConfigSync.decimal("themeItem." + key, get(key));
    }

    private ThemeItemConfig() {}
}

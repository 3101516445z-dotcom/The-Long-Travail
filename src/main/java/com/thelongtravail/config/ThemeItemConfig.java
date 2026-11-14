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
                "受到的治疗量提高的比例；0.25 表示提高 25%。\n"
                        + "Fractional increase in healing received; 0.25 means 25%.");
        number(
                b,
                "sea_in_a_bottle.magicBonus",
                .3,
                0,
                100,
                "造成的魔法类伤害提高的比例；0.3 表示提高 30%。\n"
                        + "Fractional increase in magic-class damage dealt; 0.3 means 30%.");
        integer(
                b,
                "sea_in_a_bottle.extraSlots",
                1,
                0,
                64,
                "额外增加的通用饰品栏位数量；填写整数，0 表示不增加。\n"
                        + "Additional generic curio slots; use an integer, with 0 adding none.");
        b = builders.get("aspects/far_reach.toml");
        number(
                b,
                "horizon_theory.damageCap",
                .4,
                0,
                1,
                "穷遐恶意状态下，单次伤害最多扣除的生命值占最大生命值的比例；0.4 表示最多扣除 40%。\n"
                        + "With Far Reach Malice, caps health lost per hit as a fraction of maximum"
                        + " health; 0.4 means 40%.");
        number(
                b,
                "horizon_theory.invulnerabilitySeconds",
                1.5,
                0,
                3600,
                "穷遐见证状态下，受伤后获得的独立无敌时间，单位：秒；0 表示不提供这段保护。\n"
                        + "Independent invulnerability after damage with Far Reach Witness, in seconds;"
                        + " 0 disables this protection.");
        b = builders.get("aspects/deep_valley.toml");
        number(
                b,
                "last_affairs.holdSeconds",
                1,
                .05,
                3600,
                "持续按住使用键多久后发动，单位：秒。\nTime to hold the use key before activation, in seconds.");
        number(
                b,
                "last_affairs.revivalSeconds",
                7.5,
                0,
                1000000,
                "还魂状态的持续时间，单位：秒。\nDuration of the revival state, in seconds.");
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
                "食用后获得的波旬变生效果持续时间，单位：秒；0 表示不获得该效果。\n"
                        + "Duration of Mara Transformation after eating, in seconds; 0 grants no"
                        + " effect.");
        b = builders.get("aspects/boundless.toml");
        number(
                b,
                "holy_fool.durationSeconds",
                15,
                0,
                1000000,
                "发动技能后，我于万物之中效果的持续时间，单位：秒；0 表示不获得该效果。\n"
                        + "Duration of Among All Things after activation, in seconds; 0 grants no"
                        + " effect.");
        number(
                b,
                "holy_fool.cooldownSeconds",
                90,
                0,
                1000000,
                "从技能成功发动时开始计算的冷却时间，单位：秒；0 表示无冷却。\n"
                        + "Cooldown from successful activation, in seconds; 0 means no cooldown.");
    }

    private static void food(ForgeConfigSpec.Builder b, String id, int nutrition, int saturation) {
        integer(
                b,
                id + ".nutrition",
                nutrition,
                0,
                1000,
                "每次食用恢复的饱食度；填写整数，2 点相当于饥饿条上的 1 个鸡腿图标。\n"
                        + "Nutrition restored per use; use an integer, with 2 points equal to one"
                        + " hunger icon.");
        number(
                b,
                id + ".saturation",
                saturation,
                0,
                1000,
                "每次食用恢复的饱和度点数，不是恢复倍率。饱食度恢复量设为 0 时，此项也不恢复饱和度。\n"
                        + "Saturation points restored per use, not a multiplier. No saturation is restored when nutrition is 0.");
    }

    private static void effect(
            ForgeConfigSpec.Builder b, String id, String effect, double seconds, int level) {
        String name =
                switch (effect) {
                    case "magic" -> "魔法肃正";
                    case "physical" -> "物理肃正";
                    case "resistance" -> "抗性提升";
                    case "saturation" -> "饱和";
                    case "luck" -> "幸运";
                    case "wither" -> "凋零";
                    case "speed" -> "速度";
                    default -> throw new IllegalArgumentException(effect);
                };
        number(
                b,
                id + "." + effect + "Seconds",
                seconds,
                0,
                1000000,
                name
                        + "效果的持续时间，单位：秒；0 表示不获得该效果。\n"
                        + "Duration of this effect in seconds; 0 grants no effect.");
        integer(
                b,
                id + "." + effect + "Level",
                level,
                1,
                256,
                name
                        + "效果的等级；填写整数，1 表示Ⅰ级，2 表示Ⅱ级。\n"
                        + "Level of this effect; use an integer, with 1 meaning I and 2 meaning"
                        + " II.");
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

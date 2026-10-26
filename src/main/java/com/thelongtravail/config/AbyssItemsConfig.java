package com.thelongtravail.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.*;

public final class AbyssItemsConfig {
    public static ForgeConfigSpec.DoubleValue ABUNDANCE_HOLD, ABUNDANCE_DURATION;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> ABUNDANCE_BLACKLIST;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLIST;
    public static ForgeConfigSpec.BooleanValue MAID_SOUL_ADDED, MONSTER_BATTLE_DEVICE_ADDED;
    public static ForgeConfigSpec.IntValue REGENERATION, SATURATION;
    public static final Map<String, ForgeConfigSpec.DoubleValue> NUMBERS = new LinkedHashMap<>();

    public static void define(ForgeConfigSpec.Builder b) {
        ABUNDANCE_HOLD =
                b.comment(
                                "液化丰饶：长按转换为所选类型药水所需秒数，按游戏刻计时。\n"
                                    + "Seconds held to create the selected potion type, measured in"
                                    + " game ticks.")
                        .defineInRange(
                                "items.liquefied_abundance.longPressSeconds", 1.0, 0.5, 3600.0);
        ABUNDANCE_DURATION =
                b.comment(
                                "没有有效已注册药水参数的持续效果，其默认持续秒数；瞬时效果不使用此值。\n"
                                    + "Fallback duration in seconds for effects without valid"
                                    + " registered potion samples; ignored for instant effects.")
                        .defineInRange(
                                "items.liquefied_abundance.fallbackDurationSeconds",
                                15.0,
                                0.05,
                                1000000.0);
        ABUNDANCE_BLACKLIST =
                b.comment(
                                "禁止抽取的状态效果 ID，精确匹配；仅影响新生成的药水。\n"
                                    + "Exact status effect IDs excluded from future conversions.")
                        .defineListAllowEmpty(
                                "items.liquefied_abundance.effectBlacklist",
                                List.of(),
                                v ->
                                        v instanceof String s
                                                && s.contains(":")
                                                && ResourceLocation.tryParse(s) != null);
        BLACKLIST =
                b.comment(
                                "禁止完美流体复制的物品"
                                    + " ID。精确匹配；复制产物的容器及嵌套内容也会清除这些物品，原件不变。检测到女仆或性格模组时，对应联动物品会自动填入一次，之后可自行删除。\n"
                                    + "Exact item IDs excluded from duplication, including contents"
                                    + " of copied containers and nested containers; originals are"
                                    + " unchanged. Maid compatibility items are added once when"
                                    + " their mods are detected and may then be removed freely.")
                        .defineListAllowEmpty(
                                "items.perfect_fluid.itemBlacklist",
                                List.of(),
                                v ->
                                        v instanceof String s
                                                && s.contains(":")
                                                && ResourceLocation.tryParse(s) != null);
        MAID_SOUL_ADDED =
                b.comment(
                                "车万女仆（Touhou Little Maid）：初始及已收纳女仆的魂符（不含空魂符）。\n"
                                        + "Touhou Little Maid: initial and occupied Soul Spells,"
                                        + " excluding empty ones.")
                        .define("items.perfect_fluid.compatInitialization.maidSoulAdded", false);
        MONSTER_BATTLE_DEVICE_ADDED =
                b.comment(
                                "车万女仆性格模组（Touhou Little Maid Personality）：怪兽战斗仪。\n"
                                        + "Touhou Little Maid Personality: Monster Battle Device.")
                        .define(
                                "items.perfect_fluid.compatInitialization.monsterBattleDeviceAdded",
                                false);
        REGENERATION =
                b.comment(
                                "新获得生命恢复效果时增加的等级数；0 表示不增加。与穷遐的等级加成叠加，并共用效果等级上限；不会降低已有的更高等级。\n"
                                    + "Levels added to newly applied Regeneration; 0 disables the"
                                    + " bonus. Stacks with the Far Reach level bonus and uses the"
                                    + " same effect-level cap. Never lowers an existing higher"
                                    + " level.")
                        .defineInRange("items.midnight_lafite.regenerationLevelBonus", 1, 0, 255);
        SATURATION =
                b.comment(
                                "新获得饱和效果时增加的等级数；0 表示不增加。\n"
                                        + "Levels added to newly applied Saturation; 0 disables the"
                                        + " bonus.")
                        .defineInRange("items.midnight_lafite.saturationLevelBonus", 1, 0, 255);
        number(
                b,
                "poisonDurationMultiplier",
                .5,
                .01,
                100,
                "新获得中毒效果的持续时间倍率；1 表示不变，无限持续时间不受影响。",
                "Duration multiplier for newly applied Poison; 1 leaves the duration unchanged."
                        + " Effects with infinite duration are unaffected.");
        number(
                b,
                "nauseaDurationMultiplier",
                2,
                .01,
                100,
                "新获得反胃效果的持续时间倍率；1 表示不变，无限持续时间不受影响。",
                "Duration multiplier for newly applied Nausea; 1 leaves the duration unchanged."
                        + " Effects with infinite duration are unaffected.");
        number(
                b,
                "malicePeak",
                .4,
                0,
                1000,
                "恶意状态下，普通环境中午夜时的伤害加成比例；0.4 表示提高 40%，0 表示不提供加成。",
                "Midnight damage bonus with Malice in ordinary conditions; 0.4 means a 40%"
                        + " increase, and 0 disables the bonus.");
        number(
                b,
                "shelteredNonClearPeak",
                .6,
                0,
                1000,
                "未处于露天环境且天气非晴朗时，恶意状态下午夜的伤害加成比例，替代普通环境中的峰值。末地始终视为晴天，下界始终不视为晴天。",
                "Midnight damage bonus with Malice when sheltered from the sky in non-clear"
                    + " weather. Replaces the ordinary peak bonus. The End always counts as clear"
                    + " weather; the Nether never does.");
        number(
                b,
                "witnessBonus",
                .15,
                0,
                1000,
                "见证状态下，夜间的固定伤害加成比例；0.15 表示提高 15%，0 表示不提供加成。",
                "Fixed nighttime damage bonus with Witness; 0.15 means a 15% increase, and 0"
                        + " disables the bonus.");
    }

    private static void number(
            ForgeConfigSpec.Builder b,
            String key,
            double value,
            double min,
            double max,
            String zh,
            String en) {
        NUMBERS.put(
                key,
                b.comment(zh + "\n" + en)
                        .defineInRange("items.midnight_lafite." + key, value, min, max));
    }

    public static double get(String key) {
        return NUMBERS.get(key).get();
    }

    private AbyssItemsConfig() {}
}

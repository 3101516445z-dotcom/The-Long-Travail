package com.thelongtravail.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.*;

public final class AbyssItemsConfig {
    public static ForgeConfigSpec.DoubleValue ABUNDANCE_HOLD, ABUNDANCE_DURATION;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> ABUNDANCE_BLACKLIST,
            ABUNDANCE_WHITELIST;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLIST;
    public static ForgeConfigSpec.BooleanValue MAID_SOUL_ADDED, MONSTER_BATTLE_DEVICE_ADDED;
    public static ForgeConfigSpec.IntValue REGENERATION, SATURATION;
    public static final Map<String, ForgeConfigSpec.DoubleValue> NUMBERS = new LinkedHashMap<>();

    public static void define(ForgeConfigSpec.Builder b) {
        ABUNDANCE_HOLD =
                b.comment(
                                "液化丰饶：长按转换为所选类型药水所需的时间，单位：秒；按游戏刻计时。\n"
                                        + "Seconds held to create the selected potion type, measured in"
                                        + " game ticks.")
                        .defineInRange(
                                "items.liquefied_abundance.longPressSeconds", 1.0, 0.5, 3600.0);
        ABUNDANCE_DURATION =
                b.comment(
                                "非瞬时状态效果没有可用的已注册药水样本时，使用此默认持续时间，单位：秒。瞬时效果不使用此值。\n"
                                        + "Fallback duration in seconds for effects without valid"
                                        + " registered potion samples; ignored for instant effects.")
                        .defineInRange(
                                "items.liquefied_abundance.fallbackDurationSeconds",
                                15.0,
                                0.05,
                                1000000.0);
        ABUNDANCE_WHITELIST =
                b.comment(
                                "允许生成的状态效果 ID。非空时只从名单内选择；[] 表示不限制。黑名单优先。\n"
                                        + "Allowed effect IDs. A nonempty list limits selection to"
                                        + " these entries; [] removes this restriction. The blacklist"
                                        + " takes precedence.\n"
                                        + "填写完整 ID，如 minecraft:speed。未知 ID"
                                        + " 会被忽略，重复项不增加概率；没有可用效果时取消转换，不消耗材料。\n"
                                        + "Use full IDs, such as minecraft:speed. Unknown IDs are"
                                        + " ignored; duplicates add no weight. If no effects are"
                                        + " eligible, conversion is cancelled without consuming materials.")
                        .defineListAllowEmpty(
                                "items.liquefied_abundance.effectWhitelist",
                                List.of(),
                                // 保留错误字符串，避免非空名单被校正为空列表后意外放开限制。
                                v -> v instanceof String);
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
                                "禁止复制的物品 ID；也会从复制出的容器及嵌套内容中移除这些物品。填写完整 ID，精确匹配。\n"
                                        + "Item IDs excluded from duplication. These items are also"
                                        + " removed from copied containers and nested contents."
                                        + " Use exact, full IDs.")
                        .defineListAllowEmpty(
                                "items.perfect_fluid.itemBlacklist",
                                List.of(),
                                v ->
                                        v instanceof String s
                                                && s.contains(":")
                                                && ResourceLocation.tryParse(s) != null);
        MAID_SOUL_ADDED =
                b.comment("魂符黑名单的预设处理记录。\n" + "Soul Spell blacklist preset record.")
                        .define("presets.perfect_fluid.maidSoulAdded", false);
        MONSTER_BATTLE_DEVICE_ADDED =
                b.comment("怪兽战斗仪黑名单的预设处理记录。\n" + "Monster Battle Device blacklist preset record.")
                        .define("presets.perfect_fluid.monsterBattleDeviceAdded", false);
        REGENERATION =
                b.comment(
                                "新获得生命恢复效果时增加的等级数；0 表示不增加。\n"
                                        + "Levels added to newly applied Regeneration; 0 disables the"
                                        + " bonus.")
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
                .5,
                .01,
                1000,
                "恶意状态下，普通环境中午夜时的伤害加成比例；0.5 表示提高 50%。",
                "Midnight damage bonus with Malice in ordinary conditions; 0.5 means a 50%"
                        + " increase.");
        number(
                b,
                "shelteredNonClearExtraPeak",
                .25,
                .01,
                1000,
                "未处于露天环境且天气非晴朗时，恶意状态下在普通环境午夜峰值上额外增加的比例；最终峰值为malicePeak加本项。末地始终视为晴天，下界始终不视为晴天。",
                "Additional midnight damage bonus above malicePeak when sheltered in non-clear"
                        + " weather. Added to the ordinary peak bonus. The End always counts as clear"
                        + " weather; the Nether never does.");
        number(
                b,
                "witnessBonus",
                .20,
                .01,
                1000,
                "见证状态下，夜间的固定伤害加成比例；0.20 表示提高 20%。",
                "Fixed nighttime damage bonus with Witness; 0.20 means a 20% increase.");
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

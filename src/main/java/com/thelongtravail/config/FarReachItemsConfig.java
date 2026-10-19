package com.thelongtravail.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import java.util.*;

public final class FarReachItemsConfig {
    public static final Map<String, ForgeConfigSpec.DoubleValue> NUMBERS = new LinkedHashMap<>();
    public static ForgeConfigSpec.BooleanValue ALLOW_HARMFUL;
    public static ForgeConfigSpec.IntValue IGNITE_SECONDS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> ENCHANTMENT_BLACKLIST,
            FOOD_BLACKLIST,
            EXTRA_IMMUNITIES;

    public static void define(ForgeConfigSpec.Builder b) {
        FarReachRelicsConfig.define(b);
        n(
                b,
                "golden_age.digCooldown",
                .1,
                0,
                3600,
                "空手掏取的冷却时间，单位：秒；每名玩家单独计时。0.1秒相当于2游戏刻，掏取不消耗沙块。\nCooldown for empty-handed digging, in seconds, tracked separately for each player. 0.1 seconds equals 2 game ticks; digging does not consume sand blocks.");
        n(
                b,
                "golden_age.waterTime",
                .5,
                .05,
                3600,
                "主动投入的金块开始转化前，所需的连续浸水时间，单位：秒。金块离水后重新计时，时停期间暂停计时。\nContinuous immersion time required before a deliberately thrown gold block can transform, in seconds. The timer resets when the block leaves water and pauses during a time stop.");
        n(
                b,
                "golden_age.range",
                8,
                1,
                64,
                "允许金块转化时，投掷者与金块之间的最大距离，单位：方块。转化时，投掷者须存活、与金块处于同一维度，并佩戴黄金时代与苦旅。\nMaximum distance between the thrower and the gold block at transformation, in blocks. The thrower must be alive, in the same dimension as the block, and wearing both Golden Age and The Long Travail.");
        n(
                b,
                "golden_age.batchSize",
                8,
                1,
                64,
                "每名玩家每游戏刻最多转化的金块数量。每个金块分别进行随机判定，未处理的金块在后续游戏刻继续处理。\nMaximum gold blocks transformed per player per game tick. Each block is rolled separately; remaining blocks are processed in subsequent ticks.");
        n(
                b,
                "golden_age.minNutrition",
                4,
                0,
                1000,
                "候选食物至少需要恢复的饱食度；2点相当于饥饿条上的1个鸡腿图标。\nMinimum hunger restored by an eligible food; 2 points equal one drumstick icon on the hunger bar.");
        n(
                b,
                "golden_age.minSaturation",
                6,
                0,
                1000,
                "候选食物的理论饱和度恢复量下限。\n理论恢复量按“2×食物恢复的饱食度×饱和度系数”计算，不考虑玩家当前状态对实际恢复量的限制。\nMinimum theoretical saturation restored by an eligible food. Calculated as 2 x hunger restored x saturation modifier, without applying limits imposed by the player's current state.");
        ALLOW_HARMFUL =
                b.comment(
                                "是否允许选中在标准食物属性中声明了有害效果的食物。由自定义代码产生副作用的食物，需通过食物黑名单手动排除。\nAllow foods whose standard food properties declare harmful effects. Foods with side effects implemented in custom code must be excluded manually using the food blacklist.")
                        .define("items.golden_age.allowHarmfulFood", false);
        ENCHANTMENT_BLACKLIST =
                ids(
                        b,
                        "items.golden_age.enchantmentBlacklist",
                        "禁止出现的附魔ID；默认为空列表。除此之外所有已注册附魔均可出现，等级固定为I。\nEnchantment IDs excluded from selection; empty by default. All other registered enchantments are eligible, always at level I.");
        FOOD_BLACKLIST =
                ids(
                        b,
                        "items.golden_age.foodBlacklist",
                        "禁止出现的食物物品ID；默认为空列表。\nFood item IDs excluded from selection; empty by default.");
        IGNITE_SECONDS =
                b.comment(
                                "命中后点燃目标的持续时间，单位：秒；\n0表示不点燃。\n若目标剩余的燃烧时间更长，重复命中不会将其缩短。\n实际燃烧时间仍受原版火焰保护附魔影响。\nDuration for which a hit sets the target on fire, in seconds; 0 disables ignition. Repeated hits do not shorten a longer remaining burn duration. Vanilla Fire Protection still affects the actual duration.")
                        .defineInRange("items.icarus.igniteSeconds", 10, 0, 3600);
        n(
                b,
                "icarus.malicePeak",
                .4,
                0,
                1000,
                "穷遐之恶意生效时，未同时满足露天和晴天条件的环境中，正午时的伤害加成比例；0.4表示提高40%。夜晚不提供此项加成。\nNoon damage bonus with Far Reach Malice active when the open-sky and clear-weather conditions are not both met; 0.4 means +40%. This bonus is zero at night.");
        n(
                b,
                "icarus.sunnyPeak",
                .6,
                0,
                1000,
                "穷遐之恶意生效时，露天且晴天条件下的正午伤害加成比例；\n0.6表示提高60%。\n末地始终视为晴天，下界始终不视为晴天；\n昼夜均按世界时间判断。\nNoon damage bonus with Far Reach Malice active under open sky in clear weather; 0.6 means +60%. The End always counts as clear weather, and the Nether never does. Day and night are determined by world time in all dimensions.");
        n(
                b,
                "icarus.witnessBonus",
                .15,
                0,
                1000,
                "见证分支露天时的固定增伤比例，不受时间与天气限制。\nFixed damage bonus under open sky with Witness active, regardless of time or weather.");
        EXTRA_IMMUNITIES =
                b.comment(
                                "见证额外免疫池，默认为空列表。\n填写伤害类型ID或#伤害类型标签；\n允许非魔法类型。\n默认仅内置minecraft:magic和minecraft:indirect_magic，不自动兼容其他模组。\nAdditional Witness damage immunities; empty by default. Accepts damage type IDs or #damage_type_tags, including non-magic types. Only minecraft:magic and minecraft:indirect_magic are included by default; damage types from other mods are not automatically included.")
                        .defineListAllowEmpty(
                                "items.icarus.extraImmunities",
                                List.of(),
                                v ->
                                        v instanceof String s
                                                && ResourceLocation.tryParse(s.startsWith("#") ? s.substring(1) : s)
                                                        != null);
    }

    private static void n(ForgeConfigSpec.Builder b, String k, double d, double min, double max, String comment) {
        NUMBERS.put(k, b.comment(comment).defineInRange("items." + k, d, min, max));
    }

    private static ForgeConfigSpec.ConfigValue<List<? extends String>> ids(
            ForgeConfigSpec.Builder b, String k, String comment) {
        return b.comment(comment)
                .defineListAllowEmpty(k, List.of(), v -> v instanceof String s && ResourceLocation.tryParse(s) != null);
    }

    public static double get(String key) {
        return NUMBERS.get(key).get();
    }

    public static int ticks(String key) {
        return RainConfig.ticks(get(key));
    }

    private FarReachItemsConfig() {}
}

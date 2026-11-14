package com.thelongtravail.config;

import com.thelongtravail.config.storage.ConfigProblems;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.*;

public final class DeepValleyItemsConfig {
    public static final Map<String, ForgeConfigSpec.DoubleValue> VALUES = new LinkedHashMap<>();
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_ATTRIBUTES;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> EXTRA_ORES, EXTRA_INGOTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> TREMOR_BLACKLIST;

    public static ForgeConfigSpec.EnumValue<TravailConfig.ReceivedTrigger>
            PHOSPHOPHYLLITE_HEAL_TRIGGER;
    public static ForgeConfigSpec.IntValue PHOSPHOPHYLLITE_HEAL_COOLDOWN;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>>
            PHOSPHOPHYLLITE_HEAL_EXCLUSIONS;

    public static boolean healingRequiresHealthLossDisplay() {
        return com.thelongtravail.network.TooltipConfigSync.decimal(
                        "deepValleyItem.phosphophyllite.healRequiresHealthLoss",
                        PHOSPHOPHYLLITE_HEAL_TRIGGER.get()
                                        == TravailConfig.ReceivedTrigger.HEALTH_LOSS
                                ? 1
                                : 0)
                != 0;
    }

    public static void define(ForgeConfigSpec.Builder b) {
        PHOSPHOPHYLLITE_HEAL_TRIGGER =
                b.comment(
                                "磷叶石见证治疗的触发方式：ATTACK_ATTEMPT 为受击；HEALTH_LOSS 为因伤害损失生命值，不含伤害吸收。独立于苦旅恶意配置。",
                                "Witness healing trigger: ATTACK_ATTEMPT on hit; HEALTH_LOSS on actual health loss from damage, excluding absorption. Independent of diary Malice settings.")
                        .defineEnum(
                                "items.phosphophyllite.healTrigger",
                                TravailConfig.ReceivedTrigger.ATTACK_ATTEMPT);
        PHOSPHOPHYLLITE_HEAL_COOLDOWN =
                b.comment(
                                "见证治疗两次概率判定之间的间隔，单位：游戏刻（20 游戏刻 = 1 秒）。即使本次没有触发治疗，也会进入冷却；0 表示无冷却。",
                                "Interval between Witness healing attempts in ticks, including"
                                        + " unsuccessful chance rolls. 0 disables the cooldown.")
                        .defineInRange("items.phosphophyllite.healCooldownTicks", 0, 0, 1_000_000);
        PHOSPHOPHYLLITE_HEAL_EXCLUSIONS =
                b.comment(
                                "不触发磷叶石见证治疗的伤害类型 ID；不改变伤害本身。",
                                "Damage type IDs excluded from Witness healing; does not change damage itself.")
                        .defineListAllowEmpty(
                                "items.phosphophyllite.healExcludedDamageTypes",
                                List.of(),
                                value ->
                                        value instanceof String id
                                                && net.minecraft.resources.ResourceLocation
                                                                .tryParse(id)
                                                        != null);
        TREMOR_BLACKLIST =
                b.comment(
                                "见证状态下潜行右键时，禁止破坏的方块 ID。填写完整 ID；[] 表示不限制。\n"
                                        + "Block IDs excluded from Witness sneak-use breaking. Use full"
                                        + " IDs; [] removes this restriction.")
                        .defineListAllowEmpty(
                                "items.high_frequency_tremor.witnessBreakBlacklist",
                                List.of("minecraft:barrier"),
                                x ->
                                        x instanceof String id
                                                && id.equals(
                                                        String.valueOf(
                                                                net.minecraft.resources
                                                                        .ResourceLocation.tryParse(
                                                                        id)))
                                                && id.contains(":")
                                                && !id.endsWith(":"));
        number(
                b,
                "phosphophyllite.maliceAttributes",
                .10,
                0,
                10,
                "恶意状态下，各项属性提高的比例；0.1 表示提高 10%。\n"
                        + "Fractional bonus to all affected attributes while Malice is active.");
        number(
                b,
                "phosphophyllite.witnessAttributes",
                .05,
                0,
                10,
                "见证状态下，各项属性提高的比例；0.05 表示提高 5%。\n"
                        + "Fractional bonus to all affected attributes while Witness is active.");
        number(
                b,
                "phosphophyllite.maliceDamage",
                .40,
                0,
                10,
                "恶意状态下，受到的伤害额外提高的比例；0.4 表示多承受 40% 伤害。\n"
                        + "Fractional increase in damage taken while Malice is active.");
        number(
                b,
                "phosphophyllite.witnessDamage",
                .20,
                0,
                10,
                "见证状态下，受到的伤害额外提高的比例；0.2 表示多承受 20% 伤害。\n"
                        + "Fractional increase in damage taken while Witness is active.");
        number(
                b,
                "phosphophyllite.healChance",
                .20,
                0,
                1,
                "见证状态下，每次符合治疗触发条件时，恢复生命值的概率；0.2 表示 20%。\n"
                        + "Healing chance per eligible Witness healing attempt.");
        number(
                b,
                "phosphophyllite.healFraction",
                .05,
                0,
                10,
                "每次治疗恢复的生命值占自身最大生命值的比例；0.05 表示恢复 5%。有效治疗量超出生命上限的部分转为伤害吸收（黄心）。\n"
                        + "Healing amount as a fraction of the wearer's maximum health; effective"
                        + " overhealing becomes absorption.");
        EXCLUDED_ATTRIBUTES =
                b.comment(
                                "不参与属性乘算的属性 ID。零值保持为零，负值和特殊属性经乘算后不保证有益。\nAttribute IDs excluded from "
                                        + "multiplicative bonuses. Zero values remain zero; multiplying negative "
                                        + "values or special attributes is not guaranteed to be beneficial.")
                        .defineListAllowEmpty(
                                "items.phosphophyllite.excludedAttributes",
                                List.of(),
                                x ->
                                        x instanceof String s
                                                && net.minecraft.resources.ResourceLocation
                                                                .tryParse(s)
                                                        != null);
        EXTRA_ORES = extraItems(b, "extraOres", "额外矿石物品ID\nAdditional ore item IDs");
        EXTRA_INGOTS = extraItems(b, "extraIngots", "额外锭物品ID\nAdditional ingot item IDs");
        number(
                b,
                "underground_velvet.oreChance",
                .8,
                0,
                1,
                "获得矿石的概率；其余情况获得锭。\nChance to receive ore; otherwise, an ingot is awarded.");
        number(
                b,
                "underground_velvet.mineshaftChance",
                .2,
                0,
                1,
                "恶意状态下抽取矿井战利品的概率；与古城概率之和超过 1 时，两项按原比例缩放至合计 1。\nChance to select mineshaft "
                        + "loot with Malice. If this and the ancient-city chance total more than 1, "
                        + "both are scaled proportionally to total 1.");
        number(
                b,
                "underground_velvet.cityChance",
                .1,
                0,
                1,
                "恶意状态下抽取古城战利品的概率。\nChance to select ancient-city loot with Malice.");
        number(
                b,
                "underground_velvet.cooldownTicks",
                2,
                0,
                1200,
                "使用冷却时间，单位：游戏刻；小数向上取整。\nUse cooldown in game ticks; fractional values are rounded up.");
    }

    // 注册表就绪后在服务端线程检查，保留未安装可选模组的合法 ID。
    public static void checkTremorBlacklist() {
        for (String id : TREMOR_BLACKLIST.get()) {
            if (!net.minecraftforge.registries.ForgeRegistries.BLOCKS.containsKey(
                    new net.minecraft.resources.ResourceLocation(id))) {
                ConfigProblems.report(
                        net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR
                                .get()
                                .resolve("the_long_travail/aspects/deep_valley.toml"),
                        "items.high_frequency_tremor.witnessBreakBlacklist: 未注册的方块 ID，保留原值；请检查拼写或模组是否安装 / Unknown block ID retained: "
                                + id);
            }
        }
    }

    private static ForgeConfigSpec.ConfigValue<List<? extends String>> extraItems(
            ForgeConfigSpec.Builder b, String key, String description) {
        return b.comment(
                        description,
                        "与对应标签中的物品合并后等概率抽取；重复项、未知物品和空气会被忽略。",
                        "Merged with the corresponding tag and sampled uniformly; duplicates,"
                                + " unknown items and air are ignored.")
                .defineListAllowEmpty(
                        "items.underground_velvet." + key,
                        List.of(),
                        x ->
                                x instanceof String id
                                        && net.minecraft.resources.ResourceLocation.tryParse(id)
                                                != null);
    }

    private static void number(
            ForgeConfigSpec.Builder b,
            String key,
            double value,
            double min,
            double max,
            String description) {
        VALUES.put(key, b.comment(description).defineInRange("items." + key, value, min, max));
    }

    public static double get(String key) {
        return VALUES.get(key).get();
    }

    public static double shown(String key) {
        return com.thelongtravail.network.TooltipConfigSync.decimal(
                "deepValleyItem." + key, get(key));
    }

    private DeepValleyItemsConfig() {}
}

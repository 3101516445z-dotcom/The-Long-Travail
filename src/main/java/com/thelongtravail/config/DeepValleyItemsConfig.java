package com.thelongtravail.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.*;

public final class DeepValleyItemsConfig {
    public static final Map<String, ForgeConfigSpec.DoubleValue> VALUES = new LinkedHashMap<>();
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_ATTRIBUTES;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> EXTRA_ORES, EXTRA_INGOTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> TREMOR_BLACKLIST;

    public static void define(ForgeConfigSpec.Builder b) {
        TREMOR_BLACKLIST =
                b.comment(
                                "高频战栗在幽谷见证状态下，潜行右键破坏所使用的方块黑名单；同样适用于 FTB 连锁破坏。默认禁止屏障；允许空列表。必须填写完整 namespace:path，未知 ID 保留并警告。",
                                "Block IDs excluded from Witness breaking, including FTB Ultimine. Defaults to barrier; an empty list is allowed. Use full namespace:path IDs; unknown IDs are retained with a warning.")
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
                .20,
                0,
                10,
                "恶意状态下，各项属性的加成比例。\nFractional bonus to all affected attributes while Malice is active.");
        number(
                b,
                "phosphophyllite.witnessAttributes",
                .10,
                0,
                10,
                "见证状态下，各项属性的加成比例。\nFractional bonus to all affected attributes while Witness is active.");
        number(
                b,
                "phosphophyllite.maliceDamage",
                .50,
                0,
                10,
                "恶意状态下，受到的伤害增加的比例。\nFractional increase in damage taken while Malice is active.");
        number(
                b,
                "phosphophyllite.witnessDamage",
                .20,
                0,
                10,
                "见证状态下，受到的伤害增加的比例。\nFractional increase in damage taken while Witness is active.");
        number(
                b,
                "phosphophyllite.healChance",
                .20,
                0,
                1,
                "见证状态下，受击时触发治疗的概率；触发方式与苦旅一致。\nChance to trigger healing when hit while "
                        + "Witness is active; follows the diary's on-hit trigger mode.");
        number(
                b,
                "phosphophyllite.healFraction",
                .05,
                0,
                10,
                "治疗量占自身最大生命值的比例；有效溢出治疗转化为伤害吸收量。\nHealing amount as a fraction of the "
                        + "wearer's maximum health; effective overhealing becomes absorption.");
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
                        "与对应标签合并去重后等概率抽取。未知物品和空气忽略，仅影响地下丝绒，不修改全局标签。",
                        "Merged with the tag, deduplicated and sampled uniformly. Unknown items and "
                                + "air are ignored. Only affects Underground Velvet.")
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

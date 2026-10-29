package com.thelongtravail.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.*;

public final class FarReachRelicsConfig {
    public static final Map<String, ForgeConfigSpec.BooleanValue> CARRY = new LinkedHashMap<>();
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> SIZES;

    public static void define(ForgeConfigSpec.Builder b) {
        n(
                b,
                "don_quixote.sizeFactor",
                .40,
                0,
                100,
                "综合尺寸每比标准玩家多出一倍时，增加的伤害加成比例。\n"
                        + "Fractional damage bonus for each additional multiple of the standard player"
                        + " size in the combined size metric.");
        n(
                b,
                "don_quixote.maxBonus",
                1,
                0,
                100,
                "体型增伤的上限；1表示增加100%。\n"
                        + "Maximum fractional size-based damage bonus; 1 means a 100% increase.");
        n(
                b,
                "don_quixote.healthRatio",
                500,
                1,
                1e12,
                "当目标当前生命值不低于佩戴者当前生命值乘以此倍率时，追加直击伤害。\n"
                        + "Deals additional direct strike damage only when the target's current health is at"
                        + " least this multiple of the wearer's current health.");
        n(
                b,
                "don_quixote.trueFraction",
                .04,
                0,
                1,
                "追加直击伤害占目标最大生命的比例，不重复获得输出加成。\n"
                        + "Additional direct strike damage as a fraction of the target's maximum health;"
                        + " outgoing damage bonuses are not applied again.");
        n(
                b,
                "don_quixote.witnessThreshold",
                .05,
                0,
                1,
                "原攻击单次实际造成的伤害须严格大于目标最大生命值乘以此比例。\n"
                        + "Actual damage from a single original attack must strictly exceed this"
                        + " fraction of the target's maximum health.");
        n(
                b,
                "don_quixote.reduction",
                .6,
                0,
                1,
                "见证防护期间，来自对应目标的伤害减免比例。\n"
                        + "Fractional reduction in damage taken from the corresponding target while"
                        + " Witness protection is active.");
        n(
                b,
                "don_quixote.duration",
                2,
                .05,
                3600,
                "针对对应目标的减伤持续时间，单位：秒。\n"
                        + "Duration of damage reduction against the corresponding target, in seconds.");
        n(
                b,
                "don_quixote.cooldown",
                4,
                0,
                3600,
                "从触发时开始计算的玩家全局冷却时间，单位：秒；离线时暂停计时。\n"
                        + "Player-wide cooldown starting when protection triggers, in seconds; paused"
                        + " while offline.");
        n(
                b,
                "babel.priceFactor",
                .1,
                .001,
                1,
                "恶意状态下，交易第一项材料的价格倍率；计算结果向上取整，且至少为 1。\n"
                        + "Price multiplier for the first trade ingredient while Malice is active. The"
                        + " result is rounded up to a minimum of 1.");
        n(
                b,
                "babel.restockRange",
                16,
                1,
                128,
                "加速补货所需的携带者最大距离，单位：方块；携带者须与村民处于同一维度。\n"
                        + "Maximum distance to a carrier for accelerated restocking, in blocks. The"
                        + " carrier must be in the same dimension as the villager.");
        n(
                b,
                "babel.restockSeconds",
                30,
                .05,
                1200,
                "加速补货的最短间隔，单位：秒；仍须满足村民正常工作的条件。\n"
                        + "Minimum interval between accelerated restocks, in seconds. The villager must"
                        + " still meet its normal work requirements.");
        n(
                b,
                "babel.restocksPerDay",
                8,
                2,
                128,
                "加速补货期间，每个世界日实际补货的次数上限；普通补货也计入。\n"
                        + "Maximum number of actual restocks per world day while accelerated restocking"
                        + " is active. Normal restocks also count toward this limit.");
        for (String area :
                List.of(
                        "inventory",
                        "offhand",
                        "armor",
                        "enderChest",
                        "curios",
                        "externalInventoryTick"))
            CARRY.put(
                    area,
                    b.comment(ConfigFilesDescription.carrying(area))
                            .define("items.babel.carrying." + area, true));
        SIZES =
                b.comment(
                                "体积指标覆盖值，格式：实体 ID=宽度平方×高度。仅用于堂吉诃德的计算，不改变实体碰撞箱。\n"
                                        + "Size metric overrides in the format"
                                        + " entity_id=width_squared_times_height. These affect only Don"
                                        + " Quixote calculations and do not change entity hitboxes.")
                        .defineListAllowEmpty(
                                "items.don_quixote.sizeOverrides",
                                List.of(),
                                v -> {
                                    if (!(v instanceof String s)) return false;
                                    try {
                                        String[] p = s.split("=", 2);
                                        double d = Double.parseDouble(p[1]);
                                        return net.minecraft.resources.ResourceLocation.tryParse(
                                                                p[0])
                                                        != null
                                                && Double.isFinite(d)
                                                && d > 0;
                                    } catch (Exception e) {
                                        return false;
                                    }
                                });
    }

    private static void n(
            ForgeConfigSpec.Builder b,
            String key,
            double value,
            double min,
            double max,
            String comment) {
        FarReachItemsConfig.NUMBERS.put(
                key, b.comment(comment).defineInRange("items." + key, value, min, max));
    }

    public static double value(String key) {
        return FarReachItemsConfig.NUMBERS.get(key).get();
    }

    public static int ticks(String key) {
        return (int) Math.ceil(value(key) * 20);
    }

    public static boolean carrying(String area) {
        return CARRY.get(area).get();
    }

    private FarReachRelicsConfig() {}
}

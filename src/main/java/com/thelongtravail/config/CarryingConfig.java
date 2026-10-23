package com.thelongtravail.config;

import java.util.*;
import net.minecraftforge.common.ForgeConfigSpec;

public final class CarryingConfig {
    public static final Map<String, ForgeConfigSpec.BooleanValue> AREAS = new LinkedHashMap<>();

    public static void define(ForgeConfigSpec.Builder b) {
        b.comment("配置的内部升级版本，请勿修改。\nInternal configuration upgrade version. Do not edit.")
                .defineInRange("upgrades.boundlessRelics", 1, 0, Integer.MAX_VALUE);
        for (String area :
                List.of(
                        "inventory",
                        "offhand",
                        "armor",
                        "enderChest",
                        "curios",
                        "externalInventoryTick")) {
            String label =
                    switch (area) {
                        case "inventory" -> "主背包与快捷栏";
                        case "offhand" -> "副手";
                        case "armor" -> "盔甲栏";
                        case "enderChest" -> "末影箱";
                        case "curios" -> "Curios 饰品栏";
                        case "externalInventoryTick" -> "其他模组触发的物品栏更新（需要对应模组支持）";
                        default -> throw new IllegalArgumentException(area);
                    };
            AREAS.put(
                    area,
                    b.comment(
                                    "统一携带检测的位置："
                                            + label
                                            + "。不包括外观槽，也不递归检查嵌套容器。\nEnable this location for shared carrying checks. Cosmetic slots are excluded, and nested containers are not searched recursively.")
                            .define("carrying." + area, true));
        }
    }

    public static boolean enabled(String area) {
        return AREAS.get(area).get();
    }

    private CarryingConfig() {}
}

package com.thelongtravail.config;

import java.util.*;
import net.minecraftforge.common.ForgeConfigSpec;

public final class CarryingConfig {
    public static final Map<String, ForgeConfigSpec.BooleanValue> AREAS = new LinkedHashMap<>();

    public static void define(ForgeConfigSpec.Builder b) {
        b.comment("配置的内部升级版本，请勿修改。\nInternal configuration upgrade version. Do not edit.")
                .defineInRange("upgrades.boundlessRelics", 1, 0, Integer.MAX_VALUE);
        for (String area : List.of("inventory", "offhand", "armor", "enderChest", "curios", "externalInventoryTick"))
            AREAS.put(
                    area,
                    b.comment(
                                    "统一携带检测的位置："
                                            + area
                                            + "。不包括外观槽，也不递归检查嵌套容器。\nEnable this location for shared carrying checks. Cosmetic slots are excluded, and nested containers are not searched recursively.")
                            .define("carrying." + area, true));
    }

    public static boolean enabled(String area) {
        return AREAS.get(area).get();
    }

    private CarryingConfig() {}
}

package com.thelongtravail.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.*;

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
            AREAS.put(
                    area,
                    b.comment(ConfigFilesDescription.carrying(area))
                            .define("carrying." + area, true));
        }
    }

    public static boolean enabled(String area) {
        return AREAS.get(area).get();
    }

    private CarryingConfig() {}
}

package com.thelongtravail.config;

import com.thelongtravail.config.storage.ConfigFilesDescription;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.*;

public final class CarryingConfig {
    public static final Map<String, ForgeConfigSpec.BooleanValue> AREAS = new LinkedHashMap<>();

    public static void define(ForgeConfigSpec.Builder b) {
        b.comment("配置的内部升级版本，请勿修改。\nInternal configuration upgrade version. Do not edit.")
                .defineInRange("upgrades.boundlessRelics", 1, 0, Integer.MAX_VALUE);
        b.comment("六主题物品战利品升级版本 / Six themed items loot upgrade version")
                .defineInRange("upgrades.sixThemeItems", 1, 0, Integer.MAX_VALUE);
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
        var value = AREAS.get(area);
        if (value == null) throw new IllegalArgumentException("Unknown carrying area: " + area);
        return value.get();
    }

    private CarryingConfig() {}
}

package com.thelongtravail.config;

import com.thelongtravail.TheLongTravail;

import java.nio.file.*;
import java.util.*;

public final class ConfigUpgrade {
    static boolean boundless(Path file, com.electronwill.nightconfig.core.CommentedConfig config) {
        if (!Files.exists(file)) return false;
        Object version = config.get("upgrades.boundlessRelics");
        if (version instanceof Number n && n.intValue() >= 1) return false;
        Object raw = config.get("loot.tables");
        if (raw instanceof com.electronwill.nightconfig.core.Config tables) {
            Object table = tables.get(List.of("minecraft:chests/end_city_treasure"));
            if (table instanceof com.electronwill.nightconfig.core.Config end) {
                Object items = end.get("items");
                if (items instanceof List<?> list
                        && list.equals(
                                List.of(
                                        "the_long_travail:icarus|2|1|1",
                                        "the_long_travail:free_and_easy_wandering|3|1|1",
                                        "the_long_travail:star_voice|2|1|1"))) {
                    var updated = new ArrayList<Object>(list);
                    updated.add("the_long_travail:lorentz_fantasy|3|1|1");
                    end.set("items", updated);
                } else if (items instanceof List<?> list
                        && list.stream()
                                .noneMatch(
                                        x -> x.toString()
                                                .startsWith("the_long_travail:lorentz_fantasy|")))
                    TheLongTravail.LOGGER.warn(
                            "Custom End City loot preserved; add the_long_travail:lorentz_fantasy"
                                    + " manually if desired: {}",
                            file);
            }
        }
        config.set("upgrades.boundlessRelics", 1);
        return true;
    }

    private ConfigUpgrade() {}
}

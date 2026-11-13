package com.thelongtravail.config.storage;

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
                                        x ->
                                                x.toString()
                                                        .startsWith(
                                                                "the_long_travail:lorentz_fantasy|")))
                    TheLongTravail.LOGGER.warn(
                            "Custom End City loot preserved; add the_long_travail:lorentz_fantasy"
                                    + " manually if desired: {}",
                            file);
            }
        }
        config.set("upgrades.boundlessRelics", 1);
        return true;
    }

    static boolean sixThemeItems(
            Path file, com.electronwill.nightconfig.core.CommentedConfig config) {
        if (!Files.exists(file)) return false;
        Object version = config.get("upgrades.sixThemeItems");
        if (version instanceof Number n && n.intValue() >= 1) return false;
        Object raw = config.get("loot.tables");
        if (raw instanceof com.electronwill.nightconfig.core.Config tables) {
            var defaults = com.thelongtravail.loot.UnifiedLootConfig.defaults();
            for (String id :
                    List.of(
                            "village/village_temple",
                            "desert_pyramid",
                            "jungle_temple",
                            "end_city_treasure")) {
                String key = "minecraft:chests/" + id;
                Object table = tables.get(List.of(key));
                if (!(table instanceof com.electronwill.nightconfig.core.Config current)) continue;
                com.electronwill.nightconfig.core.Config updated = defaults.get(List.of(key));
                List<String> entries = updated.get("items");
                var oldEntries =
                        entries.stream()
                                .filter(x -> !x.startsWith("the_long_travail:holy_fool|"))
                                .map(
                                        x ->
                                                id.endsWith("pyramid") || id.equals("jungle_temple")
                                                        ? x.replace("|3|", "|1|")
                                                        : x)
                                .toList();
                Object chance = current.get("chance");
                double oldChance = id.equals("end_city_treasure") ? .2 : .15;
                if (oldEntries.equals(current.get("items"))
                        && chance instanceof Number n
                        && Math.abs(n.doubleValue() - oldChance) < 1e-9) {
                    current.set("chance", updated.get("chance"));
                    current.set("items", new ArrayList<>(entries));
                } else if (current.get("items") instanceof List<?> list
                        && list.stream()
                                .noneMatch(
                                        x ->
                                                x.toString()
                                                        .startsWith(
                                                                "the_long_travail:holy_fool|"))) {
                    TheLongTravail.LOGGER.warn(
                            "Custom loot preserved; add the_long_travail:holy_fool manually if desired: {} {}",
                            file,
                            key);
                }
            }
        }
        config.set("upgrades.sixThemeItems", 1);
        return true;
    }

    private ConfigUpgrade() {}
}

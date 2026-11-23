package com.thelongtravail.migration.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.thelongtravail.TheLongTravail;
import com.thelongtravail.config.storage.ConfigFileIO;
import com.thelongtravail.config.storage.ConfigValidation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.math.BigDecimal;

import java.nio.file.*;
import java.util.*;

public final class ConfigUpgrade {
    public static void witnessRewards(Path aspects) throws java.io.IOException {
        Path underworldFile = aspects.resolve("underworld.toml");
        Path boundlessFile = aspects.resolve("boundless.toml");
        if (!Files.exists(underworldFile) && !Files.exists(boundlessFile)) return;
        var underworld = ConfigFileIO.readForMigration(underworldFile);
        var boundless = ConfigFileIO.readForMigration(boundlessFile);
        String fishing = "travail.witness.fishing.";
        String slot = "travail.witness.extraGenericCurioSlots";
        Map<String, Object> rewards = new LinkedHashMap<>();
        for (String key : List.of("fishingSpecialItemChance", "fishingSpecialItems"))
            if (underworld.contains(fishing + key))
                rewards.put(fishing + key, underworld.getRaw(fishing + key));

        // 较早版本把联动标记存在 compatInitialization；较新的显式值优先。
        boolean changed = false;
        for (String group : List.of("travail.witness.compatInitialization", "presets.witness")) {
            if (!(underworld.getRaw(group) instanceof CommentedConfig presets)) continue;
            for (String namespace : List.copyOf(presets.valueMap().keySet())) {
                String suffix = namespace + ".fishingItemsAdded";
                if (underworld.contains(group + "." + suffix))
                    rewards.put(
                            "presets.witness." + suffix, underworld.getRaw(group + "." + suffix));
                removeKey(underworld, group + "." + suffix);
                changed |= removeKey(underworld, group + "." + namespace + ".fishingEntitiesAdded");
            }
        }
        // 先提交目标文件，再清理来源；中途退出可在下次启动重试。
        if (!rewards.isEmpty()) {
            Path abyssFile = aspects.resolve("abyss.toml");
            var abyss = ConfigFileIO.readForMigration(abyssFile);
            for (var entry : rewards.entrySet())
                if (!abyss.contains(entry.getKey())) abyss.set(entry.getKey(), entry.getValue());
            ConfigFileIO.write(abyssFile, abyss);
        }
        changed |= !rewards.isEmpty();
        for (String key :
                List.of(
                        "fishingSpecialItemChance",
                        "fishingSpecialItems",
                        "fishingEntityChance",
                        "fishingEntities")) changed |= removeKey(underworld, fishing + key);
        if (boundless.contains(slot)) {
            if (!underworld.contains(slot)) underworld.set(slot, boundless.getRaw(slot));
            ConfigFileIO.write(underworldFile, underworld);
            removeKey(boundless, slot);
            ConfigFileIO.write(boundlessFile, boundless);
        } else if (changed) {
            ConfigFileIO.write(underworldFile, underworld);
        }
    }

    // 只收起由本次迁移清空的分组；同组的其他自定义键留给原配置流程。
    private static boolean removeKey(CommentedConfig config, String key) {
        if (!config.contains(key)) return false;
        while (true) {
            config.removeComment(key);
            config.remove(key);
            int dot = key.lastIndexOf('.');
            if (dot < 0) break;
            key = key.substring(0, dot);
            if (!(config.getRaw(key) instanceof CommentedConfig group) || !group.isEmpty()) break;
        }
        return true;
    }

    public static boolean upgradeGeneral(Path file, CommentedConfig config) {
        boolean changed = boundless(file, config);
        return sixThemeItems(file, config) || changed;
    }

    public static void beforeValidation(
            CommentedConfig config, ForgeConfigSpec spec, List<String> issues) {
        if (spec.getSpec().getRaw("travail.witness.cleanseOrGrantCount") != null)
            removeKey(config, "travail.witness.grantedEffectDurationSeconds");
        ConfigPresetMigration.migrate(config, spec);
        migrateEnvironmentPeak(
                config, spec, "items.icarus.", "sunnyPeak", "sunnyExtraPeak", issues);
        migrateEnvironmentPeak(
                config,
                spec,
                "items.midnight_lafite.",
                "shelteredNonClearPeak",
                "shelteredNonClearExtraPeak",
                issues);
    }

    private static void migrateEnvironmentPeak(
            CommentedConfig config,
            ForgeConfigSpec spec,
            String prefix,
            String oldKey,
            String newKey,
            List<String> issues) {
        if (spec.getSpec().getRaw(prefix + newKey) == null
                || config.getRaw(prefix + newKey) != null
                || config.getRaw(prefix + oldKey) == null) return;
        try {
            double peak = ConfigValidation.number(config.getRaw(prefix + oldKey)).doubleValue();
            Object base = config.getRaw(prefix + "malicePeak");
            double ordinary = base == null ? .5 : ConfigValidation.number(base).doubleValue();
            double extra =
                    BigDecimal.valueOf(peak).subtract(BigDecimal.valueOf(ordinary)).doubleValue();
            config.set(prefix + newKey, Math.max(.01, Math.min(1000, extra)));
            issues.add(
                    prefix
                            + oldKey
                            + " -> "
                            + newKey
                            + ": 环境总峰值已迁移为额外增量，下限0.01 / Total peak migrated to additional bonus, minimum 0.01");
        } catch (IllegalArgumentException ignored) {
            // 旧值无效时不生成新值；后续补齐默认值并清理旧键，问题原文由恢复流程归档。
        }
    }

    private static boolean boundless(
            Path file, com.electronwill.nightconfig.core.CommentedConfig config) {
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

    private static boolean sixThemeItems(
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
                            "Custom loot preserved; add the_long_travail:holy_fool manually if"
                                    + " desired: {} {}",
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

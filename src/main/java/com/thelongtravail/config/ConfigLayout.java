package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

final class ConfigLayout {
    static CommentedConfig arrange(Path file, UnmodifiableCommentedConfig source) {
        return arrange(file.getFileName().toString(), List.of(), source);
    }

    private static CommentedConfig arrange(
            String file, List<String> path, UnmodifiableCommentedConfig source) {
        CommentedConfig result = CommentedConfig.of(LinkedHashMap::new, TomlFormat.instance());
        List<String> keys = new ArrayList<>(preferred(file, path));
        for (String key : source.valueMap().keySet()) {
            if (!keys.contains(key)) keys.add(key);
        }
        for (String key : keys) {
            if (!source.valueMap().containsKey(key)) continue;
            Object value = source.valueMap().get(key);
            if (value instanceof UnmodifiableCommentedConfig child) {
                List<String> childPath = new ArrayList<>(path);
                childPath.add(key);
                value = arrange(file, childPath, child);
            }
            // 仅重排配置节；列表顺序、未知字段及其注释均保持原样。
            List<String> entryPath = List.of(key);
            result.set(entryPath, value);
            String comment = source.getComment(entryPath);
            if (comment != null) result.setComment(entryPath, comment);
        }
        return result;
    }

    private static List<String> preferred(String file, List<String> path) {
        if (path.isEmpty()) {
            return switch (file) {
                case "general.toml" ->
                        List.of(
                                "starter",
                                "requirements",
                                "malice",
                                "receivedMalice",
                                "carrying",
                                "journeyDetection",
                                "effectLimits",
                                "rewardDelivery",
                                "compatibility",
                                "loot",
                                "upgrades");
                case "client.toml" ->
                        List.of("nameEffects", "visualDeprivation", "messages", "timeStop");
                default -> List.of("travail", "items");
            };
        }
        if (path.equals(List.of("travail"))) {
            return List.of("requirements", "malice", "witness", "visualDeprivation");
        }
        return List.of();
    }

    private ConfigLayout() {}
}

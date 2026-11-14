package com.thelongtravail.config.storage;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

// 将旧预设处理记录移到独立分组，避免已处理的预设再次补入。
final class ConfigPresetMigration {
    private static final List<String> OLD_GROUPS =
            List.of(
                    "travail.requirements.compatInitialization",
                    "travail.witness.compatInitialization",
                    "items.golden_age.compatInitialization",
                    "items.perfect_fluid.compatInitialization");
    private static final List<String> NEW_GROUPS =
            List.of("exploration", "witness", "golden_age", "perfect_fluid");

    static void migrate(CommentedConfig config, ForgeConfigSpec spec) {
        Object parent = config.getRaw("presets");
        // 无效的新分组交由现有校验处理，保留原文供恢复流程归档。
        if (parent != null && !(parent instanceof CommentedConfig)) return;
        for (int i = 0; i < OLD_GROUPS.size(); i++) {
            String oldPath = OLD_GROUPS.get(i);
            String newPath = "presets." + NEW_GROUPS.get(i);
            if (!(spec.getSpec().getRaw(newPath) instanceof UnmodifiableConfig)) continue;
            if (!(config.getRaw(oldPath) instanceof CommentedConfig oldGroup)) continue;
            Object current = config.getRaw(newPath);
            if (current == null) {
                config.set(newPath, oldGroup);
                String comment = config.getComment(oldPath);
                if (comment != null) config.setComment(newPath, comment);
            } else if (current instanceof CommentedConfig newGroup) {
                mergeMissing(newGroup, oldGroup);
            } else {
                continue;
            }
            config.remove(oldPath);
            config.removeComment(oldPath);
        }
    }

    private static void mergeMissing(CommentedConfig target, CommentedConfig source) {
        for (var entry : source.entrySet()) {
            List<String> key = List.of(entry.getKey());
            Object current = target.getRaw(key);
            if (current == null) {
                target.set(key, entry.getValue());
                String comment = source.getComment(key);
                if (comment != null) target.setComment(key, comment);
            } else if (current instanceof CommentedConfig child
                    && entry.getValue() instanceof CommentedConfig oldChild) {
                mergeMissing(child, oldChild);
            }
            // 新旧位置同时有值时，以新位置为准，包括显式设置的 false。
        }
    }

    private ConfigPresetMigration() {}
}

package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

/** Default-list editing and the legacy live-first initialization policy. */
final class ConfigDefaultEdits {
    /** Mutates active configuration before saving. A rejected save does not roll back the edit.
     * This intentionally differs from ManagedConfig.update's disk-first publication policy.
     */
    static void applyLive(ModConfig config, ForgeConfigSpec definition, Predicate<CommentedConfig> edit) {
        if (edit.test(config.getConfigData())) {
            config.save();
            definition.afterReload();
        }
    }

    /** Appends missing entries in declaration order; existing duplicates remain untouched. */
    static boolean appendOnce(CommentedConfig config, String listKey, String marker,
            List<String> items, BooleanSupplier available) {
        if (Boolean.TRUE.equals(config.get(marker)) || !available.getAsBoolean()) return false;
        List<String> values = new ArrayList<>(config.<List<String>>get(listKey));
        for (String item : items) if (!values.contains(item)) values.add(item);
        config.set(listKey, values);
        config.set(marker, true);
        return true;
    }

    private ConfigDefaultEdits() {}
}

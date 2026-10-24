package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

// 维护默认列表，以及先修改活动配置、再保存的初始化流程。
final class ConfigDefaultEdits {
    // 先修改活动配置再保存；保存被拒绝时不回滚活动值。
    // 这与 ManagedConfig.update 先写盘再发布的流程不同。
    static void applyLive(ModConfig config, ForgeConfigSpec definition, Predicate<CommentedConfig> edit) {
        if (edit.test(config.getConfigData())) {
            config.save();
            definition.afterReload();
        }
    }

    // 按声明顺序追加缺少的条目，保留已有重复项。
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

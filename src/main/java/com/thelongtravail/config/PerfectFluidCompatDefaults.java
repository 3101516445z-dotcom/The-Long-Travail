package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.thelongtravail.compat.maid.MaidFluidDefaults;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;

// 按处理标记补入联动黑名单，复制限制以当前黑名单为准。
public final class PerfectFluidCompatDefaults {
    private static final String BASE = "items.perfect_fluid.";
    private static ModConfig loaded;

    static void loaded(net.minecraftforge.fml.event.config.ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == TravailConfig.SPECS.get("aspects/abyss.toml"))
            loaded = event.getConfig();
    }

    static void initialize() {
        if (loaded == null) throw new IllegalStateException("Abyss config was not loaded");
        ConfigDefaultEdits.applyLive(
                loaded,
                TravailConfig.SPECS.get("aspects/abyss.toml"),
                config -> apply(
                        config,
                        ModList.get()::isLoaded,
                        id -> ForgeRegistries.ITEMS.containsKey(new ResourceLocation(id))));
    }

    public static boolean apply(
            CommentedConfig config, Predicate<String> modPresent, Predicate<String> itemPresent) {
        boolean changed = false;
        for (var rule : MaidFluidDefaults.rules()) {
            changed =
                    add(config, modPresent, itemPresent, rule.mod(), rule.items(), rule.flag())
                            || changed;
        }
        return changed;
    }

    private static boolean add(
            CommentedConfig config,
            Predicate<String> modPresent,
            Predicate<String> itemPresent,
            String mod,
            List<String> items,
            String flag) {
        String marker = BASE + "compatInitialization." + flag;
        return ConfigDefaultEdits.appendOnce(
                config, BASE + "itemBlacklist", marker, items,
                () -> modPresent.test(mod) && items.stream().allMatch(itemPresent));
    }

    private PerfectFluidCompatDefaults() {}
}

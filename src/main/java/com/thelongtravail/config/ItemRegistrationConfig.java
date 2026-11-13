package com.thelongtravail.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.thelongtravail.config.storage.ConfigFileIO;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.*;

// 注册策略在启动时固定，游戏运行期间不重新加载。
public final class ItemRegistrationConfig {
    public static final Map<String, List<String>> GROUPS;

    static {
        var groups = new LinkedHashMap<String, List<String>>();
        groups.put(
                "flourishing",
                List.of(
                        "spring_corner",
                        "affection",
                        "mistletoe",
                        "everlasting_flower",
                        "immortal_bud",
                        "moon_toad_pastry"));
        groups.put(
                "abyss",
                List.of(
                        "eki",
                        "tokaido",
                        "perfect_fluid",
                        "midnight_lafite",
                        "liquefied_abundance",
                        "sea_in_a_bottle"));
        groups.put(
                "far_reach",
                List.of(
                        "golden_age",
                        "icarus",
                        "don_quixote",
                        "tower_of_babel",
                        "sun_chaser",
                        "horizon_theory"));
        groups.put(
                "deep_valley",
                List.of(
                        "azrael",
                        "sword_and_lantern",
                        "phosphophyllite",
                        "underground_velvet",
                        "high_frequency_tremor",
                        "last_affairs"));
        groups.put(
                "underworld",
                List.of(
                        "a_thousand_years_later",
                        "book_of_the_dead",
                        "demon_king_of_the_sixth_heaven",
                        "myriad_souls_banner",
                        "kagotsurube",
                        "living_flesh"));
        groups.put(
                "boundless",
                List.of(
                        "free_and_easy_wandering",
                        "star_voice",
                        "little_prince",
                        "lorentz_fantasy",
                        "ancient_strange_tales",
                        "holy_fool"));
        GROUPS = Collections.unmodifiableMap(groups);
    }

    private static final class Startup {
        private static final Set<String> DISABLED =
                read(FMLPaths.CONFIGDIR.get().resolve("the_long_travail/item-registration.toml"));
    }

    public static boolean enabled(String id) {
        return !Startup.DISABLED.contains(id);
    }

    public static boolean anyDisabled() {
        return !Startup.DISABLED.isEmpty();
    }

    public static boolean disabled(ResourceLocation id) {
        return id != null
                && id.getNamespace().equals("the_long_travail")
                && Startup.DISABLED.contains(id.getPath());
    }

    // 客户端与服务端沿用现有通道协商，比较启动时的注册集合。
    public static String signature() {
        long bits = 0;
        int index = 0;
        for (var group : GROUPS.values())
            for (String id : group) {
                if (!enabled(id)) bits |= 1L << index;
                index++;
            }
        return Long.toHexString(bits);
    }

    public static Set<String> read(Path file) {
        try {
            var config = ConfigFileIO.read(file);
            var disabled = new LinkedHashSet<String>();
            boolean changed = false;
            for (var group : GROUPS.entrySet()) {
                Object section = config.get(group.getKey());
                if (section != null && !(section instanceof UnmodifiableConfig))
                    throw new IllegalArgumentException("Expected a table: " + group.getKey());
                for (String id : group.getValue()) {
                    String key = group.getKey() + "." + id;
                    Object value = config.get(key);
                    if (value == null) {
                        config.set(key, true);
                        config.setComment(
                                key,
                                "true: register / 注册；false: disable / 不注册。Restart required / 重启生效。");
                        changed = true;
                    } else if (!(value instanceof Boolean)) {
                        throw new IllegalArgumentException("Expected true or false: " + key);
                    } else if (Boolean.FALSE.equals(value)) disabled.add(id);
                }
            }
            for (var entry : config.entrySet()) {
                var known = GROUPS.get(entry.getKey());
                if (known == null) warn(entry.getKey());
                else if (entry.getValue() instanceof UnmodifiableConfig section)
                    for (var key : section.entrySet())
                        if (!known.contains(key.getKey()))
                            warn(entry.getKey() + "." + key.getKey());
            }
            if (changed) {
                config.setComment(
                        "flourishing",
                        "主题物品注册开关 / Theme item registration\n"
                                + "修改后重启；客户端与服务器必须一致。/ Restart required; both sides must match.\n"
                                + "关闭注册可能丢失旧存档物品，请先备份世界。/ Disabling may remove saved items; back up your world.");
                ConfigFileIO.write(file, config);
            }
            return Collections.unmodifiableSet(disabled);
        } catch (Exception failure) {
            throw new IllegalStateException(
                    "Cannot load startup item registration config / 物品注册配置读取失败: "
                            + file
                            + "; repair and restart / 修复后重启",
                    failure);
        }
    }

    private static void warn(String key) {
        com.thelongtravail.TheLongTravail.LOGGER.warn(
                "Unknown item registration key (preserved): {}", key);
    }

    private ItemRegistrationConfig() {}
}

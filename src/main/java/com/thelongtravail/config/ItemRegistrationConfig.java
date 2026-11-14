package com.thelongtravail.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.thelongtravail.config.storage.ConfigFileIO;
import com.thelongtravail.config.storage.ConfigFilesDescription;

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
                String heading =
                        switch (group.getKey()) {
                            case "flourishing" -> "繁茂\nFlourishing";
                            case "abyss" -> "归墟\nAbyss";
                            case "far_reach" -> "穷遐\nFar Reach";
                            case "deep_valley" -> "幽谷\nDeep Valley";
                            case "underworld" -> "冥府\nUnderworld";
                            case "boundless" -> "无垠\nBoundless";
                            default -> throw new IllegalArgumentException(group.getKey());
                        };
                if (group.getKey().equals("flourishing"))
                    heading =
                            "主题物品开关\n"
                                    + "Theme item registration\n"
                                    + "修改后需重启游戏或服务器；联机双方的设置必须一致。\n"
                                    + "Restart the game or server after editing; both sides must"
                                    + " match.\n"
                                    + "关闭注册可能使旧存档中的对应物品丢失，请先备份世界。\n"
                                    + "Disabling registration may remove saved items; back up the world"
                                    + " first.\n\n"
                                    + heading;
                if (!heading.equals(config.getComment(group.getKey()))) {
                    config.setComment(group.getKey(), heading);
                    changed = true;
                }
                for (String id : group.getValue()) {
                    String key = group.getKey() + "." + id;
                    Object value = config.get(key);
                    if (value == null) {
                        config.set(key, true);
                        changed = true;
                    } else if (!(value instanceof Boolean)) {
                        throw new IllegalArgumentException("Expected true or false: " + key);
                    } else if (Boolean.FALSE.equals(value)) disabled.add(id);
                    String comment =
                            ConfigFilesDescription.itemLabel(id)
                                    + "\n"
                                    + "true：启用此物品；false：禁用此物品。\n"
                                    + "true: register this item; false: disable its registration.";
                    if (!comment.equals(config.getComment(key))) {
                        config.setComment(key, comment);
                        changed = true;
                    }
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

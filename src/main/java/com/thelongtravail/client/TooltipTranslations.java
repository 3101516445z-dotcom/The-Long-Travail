package com.thelongtravail.client;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// 只为本模组说明提供包内英文备用文本，不依赖外部资源包是否完整。
public final class TooltipTranslations {
    private static final Map<String, String> ENGLISH = load();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private static Map<String, String> load() {
        try (var stream =
                TooltipTranslations.class.getResourceAsStream(
                        "/assets/the_long_travail/lang/en_us.json")) {
            if (stream == null) throw new IllegalStateException("Bundled English locale missing");
            var values = new HashMap<String, String>();
            JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject()
                    .entrySet()
                    .forEach(e -> values.put(e.getKey(), e.getValue().getAsString()));
            return Map.copyOf(values);
        } catch (Exception e) {
            LogUtils.getLogger().error("Cannot load bundled tooltip fallback", e);
            return Map.of();
        }
    }

    public static MutableComponent text(String key, Object... args) {
        String fallback = ENGLISH.get(key);
        if (!Language.getInstance().has(key) && WARNED.add(key))
            LogUtils.getLogger()
                    .warn(
                            "Missing tooltip translation: {} (bundled fallback: {})",
                            key,
                            fallback != null);
        if (fallback == null) fallback = "Description unavailable";
        return Component.translatableWithFallback(key, fallback, args);
    }

    private TooltipTranslations() {}
}

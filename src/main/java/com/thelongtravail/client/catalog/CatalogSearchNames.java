package com.thelongtravail.client.catalog;

import com.google.gson.JsonParser;
import com.thelongtravail.TheLongTravail;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = TheLongTravail.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class CatalogSearchNames implements ResourceManagerReloadListener {
    private static volatile Map<String, List<String>> names = Map.of();

    public static List<String> forKey(String key) {
        return names.getOrDefault(key, List.of());
    }

    @SubscribeEvent
    public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new CatalogSearchNames());
    }

    @Override
    public void onResourceManagerReload(ResourceManager resources) {
        Map<String, List<String>> result = new HashMap<>();
        for (String language : List.of("zh_cn", "en_us")) {
            Map<String, String> translations = new HashMap<>();
            for (var resource :
                    resources.getResourceStack(
                            new ResourceLocation(
                                    TheLongTravail.MODID, "lang/" + language + ".json"))) {
                try (var reader = resource.openAsReader()) {
                    JsonParser.parseReader(reader)
                            .getAsJsonObject()
                            .entrySet()
                            .forEach(
                                    entry ->
                                            translations.put(
                                                    entry.getKey(),
                                                    entry.getValue().getAsString()));
                } catch (Exception failure) {
                    TheLongTravail.LOGGER.warn(
                            "Unable to index catalogue language {} from {}",
                            language,
                            resource.sourcePackId(),
                            failure);
                }
            }
            translations.forEach(
                    (key, value) ->
                            result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value));
        }
        result.replaceAll((key, values) -> List.copyOf(values));
        names = Map.copyOf(result);
    }
}

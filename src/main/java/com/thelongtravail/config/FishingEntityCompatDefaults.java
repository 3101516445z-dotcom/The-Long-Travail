package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.thelongtravail.compat.arselemental.ArsElementalFishingDefaults;
import com.thelongtravail.compat.arsnouveau.ArsNouveauFishingDefaults;
import com.thelongtravail.compat.maid.MaidFishingDefaults;
import com.thelongtravail.compat.youkaisfeasts.YoukaisFeastsFishingDefaults;
import com.thelongtravail.compat.youkaishomecoming.YoukaisHomecomingFishingDefaults;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

public final class FishingEntityCompatDefaults {
    public static final String POOL = "travail.witness.fishing.fishingEntities";

    private record Preset(String namespace, String name, List<String> entries) {}

    private static final List<Preset> PRESETS =
            List.of(
                    new Preset(
                            "youkaisfeasts",
                            "Youkai's Feasts",
                            YoukaisFeastsFishingDefaults.entities()),
                    new Preset(
                            "youkaishomecoming",
                            "Youkai's Homecoming",
                            YoukaisHomecomingFishingDefaults.entities()),
                    new Preset("ars_nouveau", "Ars Nouveau", ArsNouveauFishingDefaults.entities()),
                    new Preset(
                            "ars_elemental",
                            "Ars Elemental",
                            ArsElementalFishingDefaults.entities()),
                    new Preset(
                            "touhou_little_maid",
                            "Touhou Little Maid",
                            MaidFishingDefaults.entities()));
    private static ManagedConfig loaded;

    private static String marker(Preset preset) {
        return "travail.witness.compatInitialization."
                + preset.namespace()
                + ".fishingEntitiesAdded";
    }

    static void define(ForgeConfigSpec.Builder builder) {
        for (Preset preset : PRESETS) builder.comment(preset.name()).define(marker(preset), false);
    }

    static void loaded(ModConfigEvent.Loading event) {
        if (event.getConfig() instanceof ManagedConfig config
                && config.getSpec() == TravailConfig.SPECS.get("aspects/underworld.toml"))
            loaded = config;
    }

    static void starting(ServerStartingEvent event) {
        if (loaded == null) return;
        var namespaces = new HashSet<String>();
        ForgeRegistries.ENTITY_TYPES.getKeys().forEach(id -> namespaces.add(id.getNamespace()));
        var presets =
                PRESETS.stream().filter(preset -> namespaces.contains(preset.namespace())).toList();
        if (presets.isEmpty()) return;
        Predicate<String> exists =
                id -> ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation(id));
        loaded.update(
                config -> {
                    boolean changed = false;
                    for (Preset preset : presets)
                        changed =
                                apply(config, preset.entries(), marker(preset), true, exists)
                                        || changed;
                    return changed;
                });
    }

    public static boolean apply(
            CommentedConfig config,
            List<String> preset,
            String marker,
            boolean present,
            Predicate<String> exists) {
        return WeightedPresetEntries.apply(config, POOL, preset, marker, present, exists);
    }

    private FishingEntityCompatDefaults() {}
}

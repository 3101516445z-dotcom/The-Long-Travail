package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.journey.RequirementPools;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class BiomeCompatDefaults {
    public static final String POOL = "travail.requirements.biomes";
    public static final String TERRALITH_MARKER =
            "travail.requirements.compatInitialization.terralithBiomesAdded";
    public static final String INCENDIUM_MARKER =
            "travail.requirements.compatInitialization.incendiumBiomesAdded";
    public static final String ALEXS_CAVES_MARKER =
            "travail.requirements.compatInitialization.alexsCavesBiomesAdded";

    private record Preset(
            String namespace,
            String name,
            String marker,
            java.util.function.Function<TravailAspect, List<String>> biomes) {}

    private static final List<Preset> PRESETS =
            com.thelongtravail.compat.CompatPresets.biomes(Preset::new);
    private static final Map<TravailAspect, ManagedConfig> LOADED =
            new EnumMap<>(TravailAspect.class);

    static void define(ForgeConfigSpec.Builder builder, TravailAspect aspect) {
        for (Preset preset : PRESETS) {
            if (preset.biomes().apply(aspect).isEmpty()) continue;
            builder.comment(ConfigFilesDescription.presetMarker(preset.name(), "群系", "biome"))
                    .define(preset.marker(), false);
        }
    }

    static void loaded(ModConfigEvent.Loading event) {
        if (!(event.getConfig() instanceof ManagedConfig config)) return;
        for (TravailAspect aspect : TravailAspect.values()) {
            if (config.getSpec() == TravailConfig.SPECS.get("aspects/" + aspect.id() + ".toml"))
                LOADED.put(aspect, config);
        }
    }

    static void starting(ServerStartingEvent event) {
        var biomes = event.getServer().registryAccess().registryOrThrow(Registries.BIOME);
        var namespaces = new HashSet<String>();
        biomes.keySet().forEach(id -> namespaces.add(id.getNamespace()));
        Predicate<String> exists = id -> biomes.containsKey(new ResourceLocation(id));
        for (var entry : LOADED.entrySet()) {
            var presets =
                    PRESETS.stream()
                            .filter(preset -> namespaces.contains(preset.namespace()))
                            .filter(preset -> !preset.biomes().apply(entry.getKey()).isEmpty())
                            .toList();
            if (presets.isEmpty()) continue;
            entry.getValue()
                    .update(
                            config -> {
                                boolean changed = false;
                                for (Preset preset : presets) {
                                    changed =
                                            apply(
                                                            config,
                                                            preset.biomes().apply(entry.getKey()),
                                                            preset.marker(),
                                                            true,
                                                            exists)
                                                    || changed;
                                }
                                return changed;
                            });
        }
        RequirementPools.invalidate();
    }

    public static boolean apply(
            CommentedConfig config,
            List<String> preset,
            String marker,
            boolean present,
            Predicate<String> exists) {
        return ExplorationPresetEntries.apply(
                config, POOL, preset, marker, "biome", present, exists);
    }

    private BiomeCompatDefaults() {}
}

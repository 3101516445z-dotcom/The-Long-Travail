package com.thelongtravail.config.defaults;

import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.config.storage.ConfigFilesDescription;
import com.thelongtravail.config.storage.ManagedConfig;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.thelongtravail.TravailAspect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class EffectCompatDefaults {
    public static final String POOL = "travail.witness.positiveEffects";

    record Preset(String namespace, String name, List<String> farReach, List<String> deepValley) {
        List<String> entries(TravailAspect aspect) {
            return switch (aspect) {
                case FAR_REACH -> farReach;
                case DEEP_VALLEY -> deepValley;
                default -> List.of();
            };
        }
    }

    private static final List<TravailAspect> ASPECTS =
            List.of(TravailAspect.FAR_REACH, TravailAspect.DEEP_VALLEY);
    private static final List<Preset> PRESETS =
            com.thelongtravail.compat.CompatPresets.effects(Preset::new);
    private static final Map<TravailAspect, ManagedConfig> LOADED =
            new EnumMap<>(TravailAspect.class);

    private static String marker(Preset preset) {
        return "presets.witness." + preset.namespace() + ".positiveEffectsAdded";
    }

    public static void define(ForgeConfigSpec.Builder builder, TravailAspect aspect) {
        for (Preset preset : PRESETS) {
            if (preset.entries(aspect).isEmpty()) continue;
            builder.comment(
                            ConfigFilesDescription.presetMarker(
                                    preset.name(), "状态效果", "status effect"))
                    .define(marker(preset), false);
        }
    }

    public static void loaded(ModConfigEvent.Loading event) {
        if (!(event.getConfig() instanceof ManagedConfig config)) return;
        for (TravailAspect aspect : ASPECTS)
            if (config.getSpec() == TravailConfig.SPECS.get("aspects/" + aspect.id() + ".toml"))
                LOADED.put(aspect, config);
    }

    public static void starting(ServerStartingEvent event) {
        var namespaces = new HashSet<String>();
        ForgeRegistries.MOB_EFFECTS.getKeys().forEach(id -> namespaces.add(id.getNamespace()));
        var presets =
                PRESETS.stream().filter(preset -> namespaces.contains(preset.namespace())).toList();
        if (presets.isEmpty()) return;
        Predicate<String> eligible =
                id -> {
                    var key = new ResourceLocation(id);
                    if (!ForgeRegistries.MOB_EFFECTS.containsKey(key)) return false;
                    return ForgeRegistries.MOB_EFFECTS.getValue(key).getCategory()
                            == MobEffectCategory.BENEFICIAL;
                };
        for (TravailAspect aspect : ASPECTS) {
            ManagedConfig file = LOADED.get(aspect);
            if (file == null) continue;
            file.update(
                    config -> {
                        boolean changed = false;
                        for (Preset preset : presets)
                            changed = apply(config, aspect, preset, true, eligible) || changed;
                        return changed;
                    });
        }
    }

    static boolean apply(
            CommentedConfig config,
            TravailAspect aspect,
            Preset preset,
            boolean present,
            Predicate<String> eligible) {
        return WeightedPresetEntries.apply(
                config, POOL, preset.entries(aspect), marker(preset), present, eligible);
    }

    private EffectCompatDefaults() {}
}

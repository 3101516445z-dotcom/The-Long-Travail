package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.compat.aquamirae.AquamiraeStructureDefaults;
import com.thelongtravail.compat.arsnouveau.ArsNouveauStructureDefaults;
import com.thelongtravail.compat.cataclysm.CataclysmStructureDefaults;
import com.thelongtravail.compat.eeeabsmobs.EEEABMobsStructureDefaults;
import com.thelongtravail.compat.goety.GoetyStructureDefaults;
import com.thelongtravail.compat.goetyawaken.GoetyAwakenStructureDefaults;
import com.thelongtravail.compat.incendium.IncendiumStructureDefaults;
import com.thelongtravail.compat.ironsspellbooks.IronsSpellbooksStructureDefaults;
import com.thelongtravail.compat.legendarymonsters.LegendaryMonstersStructureDefaults;
import com.thelongtravail.compat.maidspell.MaidSpellStructureDefaults;
import com.thelongtravail.compat.mowziesmobs.MowziesMobsStructureDefaults;
import com.thelongtravail.compat.terralith.TerralithStructureDefaults;
import com.thelongtravail.compat.townsandtowers.TownsAndTowersStructureDefaults;
import com.thelongtravail.compat.traveloptics.TravelOpticsStructureDefaults;
import com.thelongtravail.data.RequirementPools;

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

public final class StructureCompatDefaults {
    public static final String POOL = "travail.requirements.structures";

    private record Preset(
            String namespace,
            String name,
            String marker,
            java.util.function.Function<TravailAspect, List<String>> structures) {}

    private static final List<Preset> PRESETS =
            List.of(
                    new Preset(
                            "eeeabsmobs",
                            "EEEAB's Mobs",
                            "travail.requirements.compatInitialization.eeeabsmobsStructuresAdded",
                            EEEABMobsStructureDefaults::structures),
                    new Preset(
                            "legendary_monsters",
                            "Legendary Monsters",
                            "travail.requirements.compatInitialization.legendarymonstersStructuresAdded",
                            LegendaryMonstersStructureDefaults::structures),
                    new Preset(
                            "cataclysm",
                            "Cataclysm",
                            "travail.requirements.compatInitialization.cataclysmStructuresAdded",
                            CataclysmStructureDefaults::structures),
                    new Preset(
                            "terralith",
                            "Terralith",
                            "travail.requirements.compatInitialization.terralithStructuresAdded",
                            TerralithStructureDefaults::structures),
                    new Preset(
                            "goetyawaken",
                            "Goety Awaken",
                            "travail.requirements.compatInitialization.goetyAwakenStructuresAdded",
                            GoetyAwakenStructureDefaults::structures),
                    new Preset(
                            "towns_and_towers",
                            "Towns and Towers",
                            "travail.requirements.compatInitialization.townsAndTowersStructuresAdded",
                            TownsAndTowersStructureDefaults::structures),
                    new Preset(
                            "goety",
                            "Goety",
                            "travail.requirements.compatInitialization.goetyStructuresAdded",
                            GoetyStructureDefaults::structures),
                    new Preset(
                            "incendium",
                            "Incendium",
                            "travail.requirements.compatInitialization.incendiumStructuresAdded",
                            IncendiumStructureDefaults::structures),
                    new Preset(
                            "irons_spellbooks",
                            "Iron's Spells and Spellbooks",
                            "travail.requirements.compatInitialization.ironsSpellbooksStructuresAdded",
                            IronsSpellbooksStructureDefaults::structures),
                    new Preset(
                            "touhou_little_maid_spell",
                            "Touhou Little Maid: Spell",
                            "travail.requirements.compatInitialization.maidSpellStructuresAdded",
                            MaidSpellStructureDefaults::structures),
                    new Preset(
                            "ars_nouveau",
                            "Ars Nouveau",
                            "travail.requirements.compatInitialization.arsNouveauStructuresAdded",
                            ArsNouveauStructureDefaults::structures),
                    new Preset(
                            "aquamirae",
                            "Aquamirae",
                            "travail.requirements.compatInitialization.aquamiraeStructuresAdded",
                            AquamiraeStructureDefaults::structures),
                    new Preset(
                            "mowziesmobs",
                            "Mowzie's Mobs",
                            "travail.requirements.compatInitialization.mowziesMobsStructuresAdded",
                            MowziesMobsStructureDefaults::structures),
                    new Preset(
                            "traveloptics",
                            "T.O Magic and Extras",
                            "travail.requirements.compatInitialization.travelOpticsStructuresAdded",
                            TravelOpticsStructureDefaults::structures));
    private static final Map<TravailAspect, ManagedConfig> LOADED =
            new EnumMap<>(TravailAspect.class);

    static void define(ForgeConfigSpec.Builder builder, TravailAspect aspect) {
        for (Preset preset : PRESETS) {
            if (preset.structures().apply(aspect).isEmpty()) continue;
            builder.comment(ConfigFilesDescription.presetMarker(preset.name(), "结构", "structure"))
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
        var structures = event.getServer().registryAccess().registryOrThrow(Registries.STRUCTURE);
        var namespaces = new HashSet<String>();
        structures.keySet().forEach(id -> namespaces.add(id.getNamespace()));
        Predicate<String> exists = id -> structures.containsKey(new ResourceLocation(id));
        for (var entry : LOADED.entrySet()) {
            var presets =
                    PRESETS.stream()
                            .filter(preset -> namespaces.contains(preset.namespace()))
                            .filter(preset -> !preset.structures().apply(entry.getKey()).isEmpty())
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
                                                            preset.structures()
                                                                    .apply(entry.getKey()),
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
                config, POOL, preset, marker, "structure", present, exists);
    }

    private StructureCompatDefaults() {}
}

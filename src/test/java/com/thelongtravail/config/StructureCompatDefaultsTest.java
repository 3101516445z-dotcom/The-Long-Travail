package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.thelongtravail.TravailAspect;

import java.util.*;
import java.util.function.Function;

public final class StructureCompatDefaultsTest {
    private record Case(
            String namespace,
            String marker,
            Function<TravailAspect, List<String>> entries,
            int[] counts) {}

    private static final List<Case> CASES =
            List.of(
                    new Case(
                            "terralith",
                            "travail.requirements.compatInitialization.terralithStructuresAdded",
                            com.thelongtravail.compat.terralith.TerralithStructureDefaults
                                    ::structures,
                            new int[] {8, 1, 3, 5, 1, 0}),
                    new Case(
                            "goetyawaken",
                            "travail.requirements.compatInitialization.goetyAwakenStructuresAdded",
                            com.thelongtravail.compat.goetyawaken.GoetyAwakenStructureDefaults
                                    ::structures,
                            new int[] {2, 1, 4, 1, 1, 0}),
                    new Case(
                            "towns_and_towers",
                            "travail.requirements.compatInitialization.townsAndTowersStructuresAdded",
                            com.thelongtravail.compat.townsandtowers.TownsAndTowersStructureDefaults
                                    ::structures,
                            new int[] {0, 7, 0, 0, 0, 1}),
                    new Case(
                            "goety",
                            "travail.requirements.compatInitialization.goetyStructuresAdded",
                            com.thelongtravail.compat.goety.GoetyStructureDefaults::structures,
                            new int[] {1, 0, 4, 1, 1, 2}),
                    new Case(
                            "incendium",
                            "travail.requirements.compatInitialization.incendiumStructuresAdded",
                            com.thelongtravail.compat.incendium.IncendiumStructureDefaults
                                    ::structures,
                            new int[] {0, 0, 0, 0, 5, 1}),
                    new Case(
                            "irons_spellbooks",
                            "travail.requirements.compatInitialization.ironsSpellbooksStructuresAdded",
                            com.thelongtravail.compat.ironsspellbooks
                                            .IronsSpellbooksStructureDefaults
                                    ::structures,
                            new int[] {1, 1, 2, 1, 1, 1}),
                    new Case(
                            "touhou_little_maid_spell",
                            "travail.requirements.compatInitialization.maidSpellStructuresAdded",
                            com.thelongtravail.compat.maidspell.MaidSpellStructureDefaults
                                    ::structures,
                            new int[] {2, 0, 0, 0, 1, 0}),
                    new Case(
                            "ars_nouveau",
                            "travail.requirements.compatInitialization.arsNouveauStructuresAdded",
                            com.thelongtravail.compat.arsnouveau.ArsNouveauStructureDefaults
                                    ::structures,
                            new int[] {0, 0, 0, 3, 0, 0}),
                    new Case(
                            "aquamirae",
                            "travail.requirements.compatInitialization.aquamiraeStructuresAdded",
                            com.thelongtravail.compat.aquamirae.AquamiraeStructureDefaults
                                    ::structures,
                            new int[] {0, 2, 0, 0, 0, 0}),
                    new Case(
                            "mowziesmobs",
                            "travail.requirements.compatInitialization.mowziesMobsStructuresAdded",
                            com.thelongtravail.compat.mowziesmobs.MowziesMobsStructureDefaults
                                    ::structures,
                            new int[] {1, 0, 1, 1, 0, 0}),
                    new Case(
                            "traveloptics",
                            "travail.requirements.compatInitialization.travelOpticsStructuresAdded",
                            com.thelongtravail.compat.traveloptics.TravelOpticsStructureDefaults
                                    ::structures,
                            new int[] {0, 0, 0, 0, 1, 0}),
                    new Case(
                            "cataclysm",
                            "travail.requirements.compatInitialization.cataclysmStructuresAdded",
                            com.thelongtravail.compat.cataclysm.CataclysmStructureDefaults
                                    ::structures,
                            new int[] {0, 2, 2, 1, 2, 1}),
                    new Case(
                            "legendary_monsters",
                            "travail.requirements.compatInitialization.legendarymonstersStructuresAdded",
                            com.thelongtravail.compat.legendarymonsters
                                            .LegendaryMonstersStructureDefaults
                                    ::structures,
                            new int[] {1, 0, 0, 0, 0, 2}),
                    new Case(
                            "eeeabsmobs",
                            "travail.requirements.compatInitialization.eeeabsmobsStructuresAdded",
                            com.thelongtravail.compat.eeeabsmobs.EEEABMobsStructureDefaults
                                    ::structures,
                            new int[] {0, 0, 0, 1, 1, 0}));
    private static int checks;

    private static void expect(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }

    private static CommentedConfig fresh() {
        var config = CommentedConfig.inMemory();
        config.set(
                StructureCompatDefaults.POOL,
                new ArrayList<>(List.of("ror:moon_dungeon|Manual excluded mod|7")));
        config.set(BiomeCompatDefaults.POOL, List.of("minecraft:plains|Manual biome|3"));
        config.set(BiomeCompatDefaults.TERRALITH_MARKER, true);
        config.set("drawCount", 0);
        return config;
    }

    private static List<String> entries(CommentedConfig config) {
        return config.get(StructureCompatDefaults.POOL);
    }

    public static void main(String[] args) {
        var allIds = new HashSet<String>();
        var markers = new HashSet<String>();
        for (var test : CASES) {
            expect(markers.add(test.marker()), "Unique mod marker");
            expect(
                    !Set.of("ror", "astral_dimension", "netherman", "medievalend", "minecraft")
                            .contains(test.namespace()),
                    "Excluded sources not preset");
            for (var aspect : TravailAspect.values()) {
                var preset = test.entries().apply(aspect);
                expect(
                        preset.size() == test.counts()[aspect.ordinal()],
                        "Theme count: " + test.namespace() + aspect);
                for (String id : preset) {
                    expect(id.startsWith(test.namespace() + ":"), "Correct namespace");
                    expect(allIds.add(id), "Unique configured target");
                }
                var config = fresh();
                expect(
                        !StructureCompatDefaults.apply(
                                config, preset, test.marker(), false, id -> true),
                        "Absent stays pending");
                expect(!config.contains(test.marker()), "Absent not marked");
                expect(
                        StructureCompatDefaults.apply(
                                        config, preset, test.marker(), true, id -> true)
                                == !preset.isEmpty(),
                        "Only applicable themes processed");
                expect(
                        entries(config).get(0).equals("ror:moon_dungeon|Manual excluded mod|7"),
                        "Excluded manual entry kept");
                expect(
                        config.get(BiomeCompatDefaults.POOL)
                                .equals(List.of("minecraft:plains|Manual biome|3")),
                        "Biome pool untouched");
                expect(
                        Boolean.TRUE.equals(config.get(BiomeCompatDefaults.TERRALITH_MARKER)),
                        "Biome marker independent");
                expect(config.<Integer>get("drawCount") == 0, "Draw count untouched");
                if (preset.isEmpty()) {
                    expect(!config.contains(test.marker()), "Unrelated theme unmarked");
                    continue;
                }
                expect(
                        entries(config).size() == preset.size() + 1,
                        "Append all registered entries");
                for (String id : preset)
                    expect(
                            entries(config)
                                    .contains(id + "|@structure." + id.replace(':', '.') + "|1"),
                            "Translation key and weight");
                config.set(StructureCompatDefaults.POOL, List.of());
                expect(
                        !StructureCompatDefaults.apply(
                                config, preset, test.marker(), true, id -> true),
                        "Deletion respected");
                expect(entries(config).isEmpty(), "No refill after deletion");
                config.remove(test.marker());
                var manual = preset.stream().map(id -> id + "|我的名称|9").toList();
                config.set(StructureCompatDefaults.POOL, manual);
                expect(
                        StructureCompatDefaults.apply(
                                config, preset, test.marker(), true, id -> true),
                        "Zero additions marked");
                expect(entries(config).equals(manual), "Manual weights and names retained");
                expect(Boolean.TRUE.equals(config.get(test.marker())), "Persistable marker value");
                config.set(test.marker(), false);
                config.set(StructureCompatDefaults.POOL, List.of());
                expect(
                        StructureCompatDefaults.apply(
                                config, preset, test.marker(), true, id -> false),
                        "No available candidate marks attempted");
                expect(entries(config).isEmpty(), "Unavailable targets skipped");
                expect(
                        !StructureCompatDefaults.apply(
                                config, preset, test.marker(), true, id -> true),
                        "Newly available target does not retry");
                config.remove(test.marker());
                expect(
                        StructureCompatDefaults.apply(
                                config, preset, test.marker(), true, preset.get(0)::equals),
                        "Reset retries with partial registry");
                expect(entries(config).size() == 1, "One available target");
            }
        }
        expect(allIds.size() == 82, "Exactly 82 approved structures");
        for (var aspect : TravailAspect.values()) {
            var config = fresh();
            int expected = 1;
            for (var test : CASES) {
                var preset = test.entries().apply(aspect);
                StructureCompatDefaults.apply(config, preset, test.marker(), true, id -> true);
                expected += preset.size();
            }
            expect(entries(config).size() == expected, "All mods merge in one theme");
            for (var test : CASES)
                expect(
                        !StructureCompatDefaults.apply(
                                config,
                                test.entries().apply(aspect),
                                test.marker(),
                                true,
                                id -> true),
                        "All markers prevent reinjection");
        }
        System.out.println("STRUCTURE_COMPAT_DEFAULTS_PASS checks=" + checks);
    }
}

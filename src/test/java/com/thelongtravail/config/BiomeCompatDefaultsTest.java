package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.compat.terralith.TerralithBiomeDefaults;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class BiomeCompatDefaultsTest {
    private static int checks;

    private static void expect(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }

    private static CommentedConfig config(List<String> entries) {
        var config = CommentedConfig.inMemory();
        config.set(BiomeCompatDefaults.POOL, new ArrayList<>(entries));
        config.set(BiomeCompatDefaults.TERRALITH_MARKER, false);
        config.set("travail.requirements.structures", List.of("minecraft:stronghold|Custom|7"));
        return config;
    }

    private static List<String> entries(CommentedConfig config) {
        return config.get(BiomeCompatDefaults.POOL);
    }

    public static void main(String[] args) {
        var all = new HashSet<String>();
        int[] counts = {16, 10, 19, 9, 4, 7};
        for (TravailAspect aspect : TravailAspect.values()) {
            var preset = TerralithBiomeDefaults.biomes(aspect);
            expect(preset.size() == counts[aspect.ordinal()], "Theme count: " + aspect);
            for (String id : preset) expect(all.add(id), "Unique preset ID: " + id);
            var config = config(List.of("minecraft:plains|Manual plains|9"));
            expect(
                    !BiomeCompatDefaults.apply(
                            config,
                            preset,
                            BiomeCompatDefaults.TERRALITH_MARKER,
                            false,
                            id -> true),
                    "Absent pack");
            expect(
                    Boolean.FALSE.equals(config.get(BiomeCompatDefaults.TERRALITH_MARKER)),
                    "No early marker");
            expect(
                    BiomeCompatDefaults.apply(
                            config, preset, BiomeCompatDefaults.TERRALITH_MARKER, true, id -> true),
                    "First attempt");
            expect(entries(config).size() == preset.size() + 1, "All targets appended");
            expect(
                    entries(config).get(0).equals("minecraft:plains|Manual plains|9"),
                    "Original entry retained");
            for (String id : preset)
                expect(
                        entries(config).contains(id + "|@biome." + id.replace(':', '.') + "|1"),
                        "Key and weight: " + id);
            expect(
                    config.get("travail.requirements.structures")
                            .equals(List.of("minecraft:stronghold|Custom|7")),
                    "Structures unchanged");
            config.set(BiomeCompatDefaults.POOL, List.of());
            expect(
                    !BiomeCompatDefaults.apply(
                            config, preset, BiomeCompatDefaults.TERRALITH_MARKER, true, id -> true),
                    "Deleted entries stay deleted");
            expect(entries(config).isEmpty(), "Empty marked pool preserved");
            config.remove(BiomeCompatDefaults.TERRALITH_MARKER);
            expect(
                    BiomeCompatDefaults.apply(
                            config, preset, BiomeCompatDefaults.TERRALITH_MARKER, true, id -> true),
                    "Removed marker retries");
            expect(entries(config).size() == preset.size(), "Reset fills empty pool");

            var manual = preset.stream().map(id -> id + "|我的名称|8").toList();
            config = config(manual);
            expect(
                    BiomeCompatDefaults.apply(
                            config, preset, BiomeCompatDefaults.TERRALITH_MARKER, true, id -> true),
                    "All manual entries still mark completion");
            expect(entries(config).equals(manual), "Existing names, weights and order retained");
            expect(
                    Boolean.TRUE.equals(config.get(BiomeCompatDefaults.TERRALITH_MARKER)),
                    "Zero additions marked");

            config = config(List.of());
            expect(
                    BiomeCompatDefaults.apply(
                            config,
                            preset,
                            BiomeCompatDefaults.TERRALITH_MARKER,
                            true,
                            id -> false),
                    "No available targets still mark attempt");
            expect(entries(config).isEmpty(), "Unavailable IDs skipped");
            expect(
                    !BiomeCompatDefaults.apply(
                            config, preset, BiomeCompatDefaults.TERRALITH_MARKER, true, id -> true),
                    "Later availability does not retry");
            config.set(BiomeCompatDefaults.TERRALITH_MARKER, false);
            expect(
                    BiomeCompatDefaults.apply(
                            config,
                            preset,
                            BiomeCompatDefaults.TERRALITH_MARKER,
                            true,
                            preset.get(0)::equals),
                    "Partial availability");
            expect(entries(config).size() == 1, "Only registered entry added");
        }
        expect(all.size() == 65, "Total preset count");
        var duplicates = config(List.of(" terralith:test |Manual|7", "terralith:test|Second|2"));
        BiomeCompatDefaults.apply(
                duplicates,
                List.of("terralith:test", "terralith:new", "terralith:new"),
                BiomeCompatDefaults.TERRALITH_MARKER,
                true,
                id -> true);
        expect(entries(duplicates).size() == 3, "ID-based matching and repeated preset IDs");
        expect(
                entries(duplicates).get(0).equals(" terralith:test |Manual|7"),
                "Preserve original formatting");
        additionalPresets();
        System.out.println("BIOME_COMPAT_DEFAULTS_PASS checks=" + checks);
    }

    private static void additionalPresets() {
        for (TravailAspect aspect : TravailAspect.values()) {
            var fire = com.thelongtravail.compat.incendium.IncendiumBiomeDefaults.biomes(aspect);
            var caves = com.thelongtravail.compat.alexscaves.AlexsCavesBiomeDefaults.biomes(aspect);
            expect(
                    fire.size() == (aspect == TravailAspect.UNDERWORLD ? 8 : 0),
                    "Incendium theme scope");
            expect(
                    caves.size() == (aspect == TravailAspect.DEEP_VALLEY ? 2 : 0),
                    "Alex's Caves theme scope");
            var config = config(List.of());
            config.set(BiomeCompatDefaults.TERRALITH_MARKER, true);
            for (int i = 0; i < 2; i++) {
                var preset = i == 0 ? fire : caves;
                String marker =
                        i == 0
                                ? BiomeCompatDefaults.INCENDIUM_MARKER
                                : BiomeCompatDefaults.ALEXS_CAVES_MARKER;
                expect(
                        !BiomeCompatDefaults.apply(config, preset, marker, false, id -> true),
                        "Absent integration stays pending");
                expect(!config.contains(marker), "Absent or unrelated theme has no marker");
                expect(
                        BiomeCompatDefaults.apply(config, preset, marker, true, id -> true)
                                == !preset.isEmpty(),
                        "Independent integration marker");
                if (preset.isEmpty()) {
                    expect(!config.contains(marker), "Do not mark unrelated theme");
                    continue;
                }
                for (String id : preset)
                    expect(
                            entries(config).contains(id + "|@biome." + id.replace(':', '.') + "|1"),
                            "New preset localized and weighted");
                config.set(BiomeCompatDefaults.POOL, List.of());
                expect(
                        !BiomeCompatDefaults.apply(config, preset, marker, true, id -> true),
                        "New integration respects deletion");
                config.remove(marker);
                var manual = preset.stream().map(id -> id + "|Custom|9").toList();
                config.set(BiomeCompatDefaults.POOL, manual);
                expect(
                        BiomeCompatDefaults.apply(config, preset, marker, true, id -> true),
                        "Manual-only integration marks processed");
                expect(entries(config).equals(manual), "Manual new integration values retained");
                config.set(marker, false);
                config.set(BiomeCompatDefaults.POOL, List.of());
                expect(
                        BiomeCompatDefaults.apply(config, preset, marker, true, id -> false),
                        "Zero available targets mark processed");
                expect(entries(config).isEmpty(), "Unavailable new targets skipped");
                config.set(marker, false);
                expect(
                        BiomeCompatDefaults.apply(
                                config, preset, marker, true, preset.get(0)::equals),
                        "Partial new registry");
                expect(entries(config).size() == 1, "Only available new target added");
            }
            expect(
                    Boolean.TRUE.equals(config.get(BiomeCompatDefaults.TERRALITH_MARKER)),
                    "Terralith marker unchanged");
        }
    }
}

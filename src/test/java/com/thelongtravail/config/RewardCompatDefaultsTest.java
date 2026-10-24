package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.thelongtravail.compat.apprenticecodex.ApprenticeCodexRewardDefaults;
import com.thelongtravail.compat.celestialcore.CelestialCoreRewardDefaults;
import com.thelongtravail.compat.curseofpandora.CurseOfPandoraRewardDefaults;
import com.thelongtravail.compat.enigmaticaddons.EnigmaticAddonsRewardDefaults;
import com.thelongtravail.compat.enigmaticdelicacy.EnigmaticDelicacyRewardDefaults;
import com.thelongtravail.compat.enigmaticlegacy.EnigmaticLegacyRewardDefaults;
import com.thelongtravail.compat.ironsspellbooks.IronsSpellbooksRewardDefaults;
import com.thelongtravail.compat.l2complements.L2ComplementsRewardDefaults;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class RewardCompatDefaultsTest {
    private record Case(
            String namespace, List<String> entries, List<String> phantom, int count, int weight) {}

    private static final List<Case> CASES =
            List.of(
                    new Case(
                            "l2complements",
                            L2ComplementsRewardDefaults.fishingItems(),
                            L2ComplementsRewardDefaults.phantomDrops(),
                            17,
                            34),
                    new Case(
                            "curseofpandora",
                            CurseOfPandoraRewardDefaults.fishingItems(),
                            CurseOfPandoraRewardDefaults.phantomDrops(),
                            16,
                            32),
                    new Case(
                            "celestial_core",
                            CelestialCoreRewardDefaults.fishingItems(),
                            CelestialCoreRewardDefaults.phantomDrops(),
                            8,
                            24),
                    new Case(
                            "irons_spellbooks",
                            IronsSpellbooksRewardDefaults.fishingItems(),
                            IronsSpellbooksRewardDefaults.phantomDrops(),
                            3,
                            8),
                    new Case(
                            "apprenticecodex",
                            ApprenticeCodexRewardDefaults.fishingItems(),
                            ApprenticeCodexRewardDefaults.phantomDrops(),
                            2,
                            4),
                    new Case(
                            "enigmaticlegacy",
                            EnigmaticLegacyRewardDefaults.fishingItems(),
                            EnigmaticLegacyRewardDefaults.phantomDrops(),
                            1,
                            3),
                    new Case(
                            "enigmaticaddons",
                            EnigmaticAddonsRewardDefaults.fishingItems(),
                            EnigmaticAddonsRewardDefaults.phantomDrops(),
                            1,
                            3),
                    new Case(
                            "enigmaticdelicacy",
                            EnigmaticDelicacyRewardDefaults.fishingItems(),
                            EnigmaticDelicacyRewardDefaults.phantomDrops(),
                            1,
                            3));
    private static final List<String> MANUAL =
            List.of("minecraft:apple|6|1|5", "ror:neonite|7|4|9");
    private static int checks;

    private static void expect(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }

    private static CommentedConfig fresh(String pool) {
        var config = CommentedConfig.inMemory();
        config.set(pool, new ArrayList<>(MANUAL));
        config.set("travail.witness.fishing.fishingEntities", List.of("minecraft:pig|10"));
        config.set("travail.witness.fishing.fishingSpecialItemChance", 0.5);
        config.set("travail.witness.phantom.phantomMaxKillsPerScan", 16);
        config.set(BiomeCompatDefaults.TERRALITH_MARKER, true);
        return config;
    }

    public static void main(String[] args) {
        var ids = new HashSet<String>();
        int totalWeight = 0;
        for (var test : CASES) {
            expect(test.entries().size() == test.count(), "Approved source count");
            int weight = 0;
            for (String entry : test.entries()) {
                String[] fields = entry.split("\\|");
                expect(fields.length == 4, "Reward format without display name");
                expect(fields[0].startsWith(test.namespace() + ":"), "Source namespace");
                expect(ids.add(fields[0]), "Unique approved item");
                expect(fields[2].equals("1") && fields[3].equals("3"), "Approved count range");
                weight += Integer.parseInt(fields[1]);
            }
            expect(weight == test.weight(), "Preserve source weights");
            totalWeight += weight;
            for (String pool :
                    List.of(RewardCompatDefaults.FISH_POOL, RewardCompatDefaults.PHANTOM_POOL)) {
                List<String> preset =
                        pool.equals(RewardCompatDefaults.FISH_POOL)
                                ? test.entries()
                                : test.phantom();
                expect(
                        preset.equals(test.entries()),
                        "Current independent presets retain approved values");
                String marker =
                        "travail.witness.compatInitialization." + test.namespace() + ".itemsAdded";
                var config = fresh(pool);
                expect(
                        !RewardCompatDefaults.apply(
                                config, pool, preset, marker, false, id -> true),
                        "Absent not changed");
                expect(!config.contains(marker), "Absent not marked");
                expect(
                        !RewardCompatDefaults.apply(
                                config, pool, List.of(), marker, true, id -> true),
                        "Empty preset ignored");
                String first = preset.get(0).split("\\|")[0];
                List<String> original = new ArrayList<>(MANUAL);
                original.add(" " + first + " |9|4|7");
                config.set(pool, original);
                expect(
                        RewardCompatDefaults.apply(config, pool, preset, marker, true, id -> true),
                        "Installed source applied");
                List<String> merged = config.get(pool);
                expect(
                        merged.subList(0, original.size()).equals(original),
                        "Manual namespaced entries and order retained");
                expect(
                        merged.subList(original.size(), merged.size())
                                .equals(preset.subList(1, preset.size())),
                        "Append exact weights and counts");
                expect(merged.size() == MANUAL.size() + test.count(), "Same ID not duplicated");
                expect(Boolean.TRUE.equals(config.get(marker)), "Recorded completion");
                expect(
                        config.<List<String>>get("travail.witness.fishing.fishingEntities")
                                .equals(List.of("minecraft:pig|10")),
                        "Entity pool preserved");
                expect(
                        config.<Double>get("travail.witness.fishing.fishingSpecialItemChance")
                                == 0.5,
                        "Trigger probability preserved");
                expect(
                        config.<Integer>get("travail.witness.phantom.phantomMaxKillsPerScan") == 16,
                        "Phantom rules preserved");
                config.set(pool, List.of());
                expect(
                        !RewardCompatDefaults.apply(config, pool, preset, marker, true, id -> true),
                        "Deletion not refilled");
                expect(
                        config.<List<String>>get(pool).isEmpty(),
                        "Explicit empty pool retained after initialization");
                config.set(marker, false);
                expect(
                        RewardCompatDefaults.apply(
                                config, pool, preset, marker, true, first::equals),
                        "Reset and partial registry");
                expect(
                        config.<List<String>>get(pool).equals(preset.subList(0, 1)),
                        "Only registered candidate added");
                config.remove(marker);
                config.set(pool, List.of());
                expect(
                        RewardCompatDefaults.apply(config, pool, preset, marker, true, id -> false),
                        "No available candidate still records attempt");
                expect(Boolean.TRUE.equals(config.get(marker)), "Zero additions marked");
                expect(
                        !RewardCompatDefaults.apply(config, pool, preset, marker, true, id -> true),
                        "Later candidate does not trigger retry");
                config.remove(marker);
                config.set(pool, preset);
                expect(
                        RewardCompatDefaults.apply(config, pool, preset, marker, true, id -> true),
                        "Complete manual pool records completion");
                expect(config.<List<String>>get(pool).equals(preset), "Complete manual pool kept");
            }
        }
        expect(
                ids.size() == 49 && ids.stream().noneMatch(id -> id.startsWith("ror:")),
                "49 approved items, ROR excluded");
        expect(totalWeight == 111, "Approved mod weight total");
        var config = fresh(RewardCompatDefaults.FISH_POOL);
        config.set(RewardCompatDefaults.PHANTOM_POOL, List.of());
        var test = CASES.get(0);
        String base = "travail.witness.compatInitialization.l2complements.";
        config.set(base + "fishingItemsAdded", true);
        expect(
                !RewardCompatDefaults.apply(
                        config,
                        RewardCompatDefaults.FISH_POOL,
                        test.entries(),
                        base + "fishingItemsAdded",
                        true,
                        id -> true),
                "Pre-marked fishing skipped");
        expect(
                RewardCompatDefaults.apply(
                        config,
                        RewardCompatDefaults.PHANTOM_POOL,
                        test.entries(),
                        base + "phantomDropsAdded",
                        true,
                        id -> true),
                "Phantom independent of fishing");
        expect(
                config.<List<String>>get(RewardCompatDefaults.FISH_POOL).equals(MANUAL),
                "Other pool retained");
        independentPresets();
        System.out.println("REWARD_COMPAT_DEFAULTS_PASS checks=" + checks);
    }

    private static void independentPresets() {
        var fishing = List.of("example:shared|2|1|3", "example:fish_only|5|2|4");
        var phantom = List.of("example:shared|9|4|7", "example:phantom_only|1|8|9");
        var preset = new RewardCompatDefaults.Preset("example", "Example", fishing, phantom);
        var config = CommentedConfig.inMemory();
        config.set(RewardCompatDefaults.FISH_POOL, List.of());
        config.set(RewardCompatDefaults.PHANTOM_POOL, List.of());
        expect(
                RewardCompatDefaults.apply(
                        config, RewardCompatDefaults.FISHING, preset, true, id -> true),
                "Route fishing preset");
        expect(
                config.<List<String>>get(RewardCompatDefaults.FISH_POOL).equals(fishing),
                "Fishing-specific items and shared item values");
        expect(
                config.<List<String>>get(RewardCompatDefaults.PHANTOM_POOL).isEmpty(),
                "Fishing does not write phantom");
        expect(
                RewardCompatDefaults.apply(
                        config, RewardCompatDefaults.PHANTOM, preset, true, id -> true),
                "Route phantom preset independently");
        expect(
                config.<List<String>>get(RewardCompatDefaults.PHANTOM_POOL).equals(phantom),
                "Phantom-specific items and distinct shared item values");
        expect(
                config.<List<String>>get(RewardCompatDefaults.FISH_POOL).equals(fishing),
                "Phantom does not change fishing");
        config.set(RewardCompatDefaults.FISH_POOL, List.of());
        expect(
                !RewardCompatDefaults.apply(
                        config, RewardCompatDefaults.FISHING, preset, true, id -> true),
                "Fishing completion still prevents refill");
        config.remove("travail.witness.compatInitialization.example.fishingItemsAdded");
        var changed =
                new RewardCompatDefaults.Preset(
                        "example", "Example", List.of("example:new_fish|3|2|5"), phantom);
        expect(
                RewardCompatDefaults.apply(
                        config, RewardCompatDefaults.FISHING, changed, true, id -> true),
                "Reset uses revised fishing data");
        expect(
                config.<List<String>>get(RewardCompatDefaults.FISH_POOL)
                        .equals(changed.fishingItems()),
                "Updated fishing data isolated");
        expect(
                config.<List<String>>get(RewardCompatDefaults.PHANTOM_POOL).equals(phantom),
                "Phantom data retained after fishing revision");
        var oneSided =
                new RewardCompatDefaults.Preset("phantom_only", "Phantom only", List.of(), phantom);
        expect(
                !RewardCompatDefaults.apply(
                        config, RewardCompatDefaults.FISHING, oneSided, true, id -> true),
                "Source without fishing candidates ignored");
        expect(
                !config.contains(
                        "travail.witness.compatInitialization.phantom_only.fishingItemsAdded"),
                "No completion for absent category");
        expect(
                RewardCompatDefaults.apply(
                        config, RewardCompatDefaults.PHANTOM, oneSided, true, id -> true),
                "Source with only phantom candidates still handled");
    }
}

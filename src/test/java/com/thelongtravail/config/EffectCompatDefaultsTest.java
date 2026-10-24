package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.compat.alexsmobs.AlexsMobsEffectDefaults;
import com.thelongtravail.compat.arsnouveau.ArsNouveauEffectDefaults;
import com.thelongtravail.compat.attributeslib.AttributesLibEffectDefaults;
import com.thelongtravail.compat.cataclysm.CataclysmEffectDefaults;
import com.thelongtravail.compat.enigmaticaddons.EnigmaticAddonsEffectDefaults;
import com.thelongtravail.compat.goety.GoetyEffectDefaults;
import com.thelongtravail.compat.goetyawaken.GoetyAwakenEffectDefaults;
import com.thelongtravail.compat.ironsspellbooks.IronsSpellbooksEffectDefaults;
import com.thelongtravail.compat.ironsspellsdelight.IronsSpellsDelightEffectDefaults;
import com.thelongtravail.compat.morepotioneffects.MorePotionEffectsDefaults;
import com.thelongtravail.compat.youkaisfeasts.YoukaisFeastsEffectDefaults;
import com.thelongtravail.compat.youkaishomecoming.YoukaisHomecomingEffectDefaults;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class EffectCompatDefaultsTest {
    private static final String POOL = EffectCompatDefaults.POOL;
    private static final List<EffectCompatDefaults.Preset> PRESETS =
            List.of(
                    new EffectCompatDefaults.Preset(
                            "goety",
                            "Goety",
                            GoetyEffectDefaults.farReach(),
                            GoetyEffectDefaults.deepValley()),
                    new EffectCompatDefaults.Preset(
                            "more_potion_effects",
                            "More Potion Effects",
                            MorePotionEffectsDefaults.farReach(),
                            MorePotionEffectsDefaults.deepValley()),
                    new EffectCompatDefaults.Preset(
                            "youkaisfeasts",
                            "Youkai's Feasts",
                            YoukaisFeastsEffectDefaults.farReach(),
                            YoukaisFeastsEffectDefaults.deepValley()),
                    new EffectCompatDefaults.Preset(
                            "youkaishomecoming",
                            "Youkai's Homecoming",
                            YoukaisHomecomingEffectDefaults.farReach(),
                            YoukaisHomecomingEffectDefaults.deepValley()),
                    new EffectCompatDefaults.Preset(
                            "irons_spells_delight",
                            "Iron's Spells Delight",
                            IronsSpellsDelightEffectDefaults.farReach(),
                            IronsSpellsDelightEffectDefaults.deepValley()),
                    new EffectCompatDefaults.Preset(
                            "goetyawaken",
                            "Goety Awaken",
                            GoetyAwakenEffectDefaults.farReach(),
                            GoetyAwakenEffectDefaults.deepValley()),
                    new EffectCompatDefaults.Preset(
                            "cataclysm",
                            "Cataclysm",
                            CataclysmEffectDefaults.farReach(),
                            CataclysmEffectDefaults.deepValley()),
                    new EffectCompatDefaults.Preset(
                            "enigmaticaddons",
                            "Enigmatic Addons",
                            EnigmaticAddonsEffectDefaults.farReach(),
                            EnigmaticAddonsEffectDefaults.deepValley()),
                    new EffectCompatDefaults.Preset(
                            "irons_spellbooks",
                            "Iron's Spells and Spellbooks",
                            IronsSpellbooksEffectDefaults.farReach(),
                            IronsSpellbooksEffectDefaults.deepValley()),
                    new EffectCompatDefaults.Preset(
                            "alexsmobs",
                            "Alex's Mobs",
                            AlexsMobsEffectDefaults.farReach(),
                            AlexsMobsEffectDefaults.deepValley()),
                    new EffectCompatDefaults.Preset(
                            "attributeslib",
                            "AttributesLib",
                            AttributesLibEffectDefaults.farReach(),
                            AttributesLibEffectDefaults.deepValley()),
                    new EffectCompatDefaults.Preset(
                            "ars_nouveau",
                            "Ars Nouveau",
                            ArsNouveauEffectDefaults.farReach(),
                            ArsNouveauEffectDefaults.deepValley()));
    private static final List<String> MANUAL =
            List.of(
                    "minecraft:strength|6|9",
                    "astral_dimension:lifesteal|2|1",
                    "esoteric_spell:mana_resonance|4|1",
                    "projectile_damage:impact|4|1",
                    "gtbcs_geomancy_plus:aegis|4|1");
    private static int checks;

    private static void expect(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }

    private static String marker(String namespace) {
        return "travail.witness.compatInitialization." + namespace + ".positiveEffectsAdded";
    }

    private static CommentedConfig fresh() {
        var config = CommentedConfig.inMemory();
        config.set(POOL, MANUAL);
        config.set("travail.witness.compatInitialization.example.fishingItemsAdded", true);
        config.set(
                "travail.witness.beneficialLevelBonusBlacklist", List.of("minecraft:night_vision"));
        config.set("travail.requirements.biomes", List.of("minecraft:plains|Plains|7"));
        return config;
    }

    public static void main(String[] args) {
        var all = new HashSet<String>();
        for (var preset : PRESETS) {
            expect(preset.farReach().equals(preset.deepValley()), "Current pool values match");
            for (String entry : preset.farReach()) {
                String[] fields = entry.split("\\|");
                expect(fields.length == 3, "Effect entry has ID, max level and weight");
                expect(fields[0].startsWith(preset.namespace() + ":"), "Namespace matches source");
                expect(all.add(fields[0]), "No duplicate candidate");
                expect(
                        Integer.parseInt(fields[1]) >= 1 && Integer.parseInt(fields[1]) <= 4,
                        "Approved effect cap");
                expect(fields[2].equals("1"), "Weight one");
            }
            for (var aspect : List.of(TravailAspect.FAR_REACH, TravailAspect.DEEP_VALLEY)) {
                var config = fresh();
                String mark = marker(preset.namespace());
                var entries = preset.entries(aspect);
                String first = entries.get(0).split("\\|")[0];
                expect(
                        !EffectCompatDefaults.apply(config, aspect, preset, false, id -> true),
                        "Absent source skipped");
                expect(!config.contains(mark), "Absent source remains pending");
                var manual = new ArrayList<>(MANUAL);
                manual.add(" " + first + " |8|13");
                config.set(POOL, manual);
                expect(
                        EffectCompatDefaults.apply(config, aspect, preset, true, id -> true),
                        "Present source initialized");
                var wanted = new ArrayList<>(manual);
                wanted.addAll(entries.subList(1, entries.size()));
                expect(
                        wanted.equals(config.get(POOL)),
                        "Preserve manual IDs, levels, weights and order");
                expect(Boolean.TRUE.equals(config.get(mark)), "Completion persisted");
                expect(
                        Boolean.TRUE.equals(
                                config.get(
                                        "travail.witness.compatInitialization.example.fishingItemsAdded")),
                        "Other category marker retained");
                expect(
                        List.of("minecraft:night_vision")
                                .equals(
                                        config.get(
                                                "travail.witness.beneficialLevelBonusBlacklist")),
                        "Blacklist untouched");
                expect(
                        List.of("minecraft:plains|Plains|7")
                                .equals(config.get("travail.requirements.biomes")),
                        "Exploration untouched");
                config.set(POOL, List.of());
                expect(
                        !EffectCompatDefaults.apply(config, aspect, preset, true, id -> true),
                        "Deleted candidates not restored");
                expect(config.<List<String>>get(POOL).isEmpty(), "Empty initialized pool retained");
                config.set(mark, false);
                expect(
                        EffectCompatDefaults.apply(config, aspect, preset, true, first::equals),
                        "Reset retries eligible subset");
                expect(
                        entries.subList(0, 1).equals(config.get(POOL)),
                        "Missing or non-beneficial candidates filtered");
                config.remove(mark);
                config.set(POOL, List.of());
                expect(
                        EffectCompatDefaults.apply(config, aspect, preset, true, id -> false),
                        "Zero eligible still initializes");
                expect(Boolean.TRUE.equals(config.get(mark)), "Zero eligible marked");
                expect(
                        !EffectCompatDefaults.apply(config, aspect, preset, true, id -> true),
                        "Registry additions do not refill");
                config.remove(mark);
                config.set(POOL, entries);
                expect(
                        EffectCompatDefaults.apply(config, aspect, preset, true, id -> true),
                        "Fully manual source records initialization");
                expect(entries.equals(config.get(POOL)), "Fully manual pool unchanged");
            }
        }
        expect(PRESETS.size() == 12 && all.size() == 36, "Approved 12 sources and 36 IDs");
        for (String ns :
                List.of(
                        "astral_dimension",
                        "esoteric_spell",
                        "projectile_damage",
                        "gtbcs_geomancy_plus"))
            expect(all.stream().noneMatch(id -> id.startsWith(ns + ":")), "Excluded source absent");
        independent();
        System.out.println("EFFECT_COMPAT_DEFAULTS_PASS checks=" + checks);
    }

    private static void independent() {
        var far = fresh();
        var valley = fresh();
        var preset =
                new EffectCompatDefaults.Preset(
                        "example",
                        "Example",
                        List.of("example:shared|2|3", "example:far_only|1|5"),
                        List.of("example:shared|4|7", "example:valley_only|3|2"));
        expect(
                EffectCompatDefaults.apply(far, TravailAspect.FAR_REACH, preset, true, id -> true),
                "Far initialized");
        expect(!valley.contains(marker("example")), "Valley remains pending");
        expect(
                EffectCompatDefaults.apply(
                        valley, TravailAspect.DEEP_VALLEY, preset, true, id -> true),
                "Valley initialized independently");
        var farExpected = new ArrayList<>(MANUAL);
        farExpected.addAll(preset.farReach());
        var valleyExpected = new ArrayList<>(MANUAL);
        valleyExpected.addAll(preset.deepValley());
        expect(farExpected.equals(far.get(POOL)), "Far-specific values routed");
        expect(valleyExpected.equals(valley.get(POOL)), "Valley-specific values routed");
        far.set(POOL, List.of());
        far.remove(marker("example"));
        expect(
                EffectCompatDefaults.apply(far, TravailAspect.FAR_REACH, preset, true, id -> true),
                "Reset only far");
        expect(preset.farReach().equals(far.get(POOL)), "Far reset restores own preset");
        expect(valleyExpected.equals(valley.get(POOL)), "Valley untouched by far reset");
        var oneSided =
                new EffectCompatDefaults.Preset("only", "Only", List.of(), preset.deepValley());
        expect(
                !EffectCompatDefaults.apply(
                        far, TravailAspect.FAR_REACH, oneSided, true, id -> true),
                "Empty category skipped");
        expect(!far.contains(marker("only")), "Empty category unmarked");
        expect(
                EffectCompatDefaults.apply(
                        valley, TravailAspect.DEEP_VALLEY, oneSided, true, id -> true),
                "Single-category preset supported");
    }
}

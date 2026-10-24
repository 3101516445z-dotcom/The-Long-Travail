package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.compat.youkaisfeasts.YoukaisFeastsFishingDefaults;
import com.thelongtravail.compat.youkaishomecoming.YoukaisHomecomingEffectDefaults;
import com.thelongtravail.compat.youkaishomecoming.YoukaisHomecomingFishingDefaults;

import java.util.List;

public final class YoukaiCompatDefaultsTest {
    private static int checks;

    private static void expect(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }

    public static void main(String[] args) {
        String pool = FishingEntityCompatDefaults.POOL;
        String feastMarker =
                "travail.witness.compatInitialization.youkaisfeasts.fishingEntitiesAdded";
        String homeMarker =
                "travail.witness.compatInitialization.youkaishomecoming.fishingEntitiesAdded";
        var config = CommentedConfig.inMemory();
        var manual = List.of("youkaisfeasts:deer|77", "youkaishomecoming:deer|99");
        config.set(pool, manual);
        config.set(feastMarker, true);
        expect(
                FishingEntityCompatDefaults.apply(
                        config,
                        YoukaisHomecomingFishingDefaults.entities(),
                        homeMarker,
                        true,
                        id -> id.startsWith("youkaishomecoming:")),
                "Initialized Feasts does not block Homecoming");
        List<String> merged = config.get(pool);
        expect(
                merged.subList(0, 2).equals(manual),
                "Same path in different namespaces retains distinct manual weights");
        expect(merged.size() == 6, "Only Homecoming missing entries added");
        expect(
                merged.subList(2, 6)
                        .equals(YoukaisHomecomingFishingDefaults.entities().subList(1, 5)),
                "Exact Homecoming IDs");
        config.set(pool, List.of());
        config.set(feastMarker, false);
        expect(
                !FishingEntityCompatDefaults.apply(
                        config,
                        YoukaisHomecomingFishingDefaults.entities(),
                        homeMarker,
                        true,
                        id -> true),
                "Reset Feasts does not reset Homecoming");
        expect(
                FishingEntityCompatDefaults.apply(
                        config,
                        YoukaisFeastsFishingDefaults.entities(),
                        feastMarker,
                        true,
                        id -> true),
                "Feasts independently reset");
        expect(
                config.get(pool).equals(YoukaisFeastsFishingDefaults.entities()),
                "No namespace migration");
        var fresh = CommentedConfig.inMemory();
        fresh.set(pool, List.of());
        expect(
                FishingEntityCompatDefaults.apply(
                        fresh,
                        YoukaisHomecomingFishingDefaults.entities(),
                        homeMarker,
                        true,
                        id -> id.startsWith("youkaisfeasts:")),
                "Other namespace is not a matching registration");
        expect(
                fresh.<List<String>>get(pool).isEmpty(),
                "Feasts registration never produces Homecoming IDs");

        var home =
                new EffectCompatDefaults.Preset(
                        "youkaishomecoming",
                        "Youkai's Homecoming",
                        YoukaisHomecomingEffectDefaults.farReach(),
                        YoukaisHomecomingEffectDefaults.deepValley());
        for (TravailAspect aspect : List.of(TravailAspect.FAR_REACH, TravailAspect.DEEP_VALLEY)) {
            var effects = CommentedConfig.inMemory();
            var existing = List.of("youkaisfeasts:craby|8|6", "youkaishomecoming:craby|7|9");
            effects.set(EffectCompatDefaults.POOL, existing);
            effects.set(
                    "travail.witness.compatInitialization.youkaisfeasts.positiveEffectsAdded",
                    true);
            expect(
                    EffectCompatDefaults.apply(effects, aspect, home, true, id -> true),
                    "Other version marker cannot block effect preset");
            List<String> entries = effects.get(EffectCompatDefaults.POOL);
            expect(
                    entries.subList(0, 2).equals(existing),
                    "Distinct namespace levels and weights preserved");
            expect(
                    entries.subList(2, 6).equals(home.entries(aspect).subList(1, 5)),
                    "Only missing Homecoming effects appended");
            expect(entries.size() == 6, "Full ID deduplication");
        }
        System.out.println("YOUKAI_NAMESPACE_PASS checks=" + checks);
    }

    private YoukaiCompatDefaultsTest() {}
}

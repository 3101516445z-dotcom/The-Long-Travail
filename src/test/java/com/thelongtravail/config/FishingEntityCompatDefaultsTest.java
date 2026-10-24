package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.thelongtravail.compat.arselemental.ArsElementalFishingDefaults;
import com.thelongtravail.compat.arsnouveau.ArsNouveauFishingDefaults;
import com.thelongtravail.compat.maid.MaidFishingDefaults;
import com.thelongtravail.compat.youkaisfeasts.YoukaisFeastsFishingDefaults;
import com.thelongtravail.compat.youkaishomecoming.YoukaisHomecomingFishingDefaults;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class FishingEntityCompatDefaultsTest {
    private record Case(String namespace, List<String> entries, int count, int weight) {}

    private static final List<Case> CASES =
            List.of(
                    new Case("youkaisfeasts", YoukaisFeastsFishingDefaults.entities(), 5, 40),
                    new Case(
                            "youkaishomecoming",
                            YoukaisHomecomingFishingDefaults.entities(),
                            5,
                            40),
                    new Case("ars_nouveau", ArsNouveauFishingDefaults.entities(), 3, 25),
                    new Case("ars_elemental", ArsElementalFishingDefaults.entities(), 1, 25),
                    new Case("touhou_little_maid", MaidFishingDefaults.entities(), 1, 15));
    private static int checks;

    private static void expect(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }

    public static void main(String[] args) {
        var ids = new HashSet<String>();
        var config = CommentedConfig.inMemory();
        List<String> manual = List.of("minecraft:pig|7", "starfantasy_goety:apollyon|1");
        String pool = FishingEntityCompatDefaults.POOL;
        config.set(pool, new ArrayList<>(manual));
        config.set(RewardCompatDefaults.FISH_POOL, List.of("minecraft:apple|6|1|5"));
        config.set(RewardCompatDefaults.PHANTOM_POOL, List.of("minecraft:diamond|3|1|2"));
        config.set("travail.witness.fishing.fishingEntityChance", 0.3);
        int expected = manual.size();
        for (Case test : CASES) {
            expect(test.entries().size() == test.count(), "Source count");
            for (String entry : test.entries()) {
                String[] parts = entry.split("\\|");
                expect(parts.length == 2, "Entity ID and weight format");
                expect(parts[0].startsWith(test.namespace() + ":"), "Source namespace");
                expect(Integer.parseInt(parts[1]) == test.weight(), "Approved weight");
                expect(ids.add(parts[0]), "No duplicate candidate");
            }
            String marker =
                    "travail.witness.compatInitialization."
                            + test.namespace()
                            + ".fishingEntitiesAdded";
            config.set(
                    "travail.witness.compatInitialization."
                            + test.namespace()
                            + ".fishingItemsAdded",
                    true);
            config.set("travail.requirements.compatInitialization.testBiomesAdded", true);
            expect(
                    !FishingEntityCompatDefaults.apply(
                            config, test.entries(), marker, false, id -> true),
                    "Absent source ignored");
            expect(!config.contains(marker), "Absent source not marked");
            expect(
                    FishingEntityCompatDefaults.apply(
                            config, test.entries(), marker, true, id -> true),
                    "Item and exploration markers do not block entities");
            expected += test.count();
            expect(config.<List<String>>get(pool).size() == expected, "Append each source once");
            expect(
                    config.<List<String>>get(pool).containsAll(manual),
                    "Manual excluded entity preserved");
            expect(
                    !FishingEntityCompatDefaults.apply(
                            config, test.entries(), marker, true, id -> true),
                    "Completed source not repeated");
            var separate = CommentedConfig.inMemory();
            String first = test.entries().get(0).split("\\|")[0];
            separate.set(pool, List.of(" " + first + " |99"));
            expect(
                    FishingEntityCompatDefaults.apply(
                            separate, test.entries(), marker, true, id -> true),
                    "Manual same-ID merge");
            List<String> merged = separate.get(pool);
            expect(
                    merged.get(0).equals(" " + first + " |99"),
                    "Manual weight and formatting retained");
            expect(merged.size() == test.count(), "Manual ID not duplicated");
            expect(
                    merged.subList(1, merged.size())
                            .equals(test.entries().subList(1, test.count())),
                    "Append order retained");
            separate.set(pool, List.of());
            expect(
                    !FishingEntityCompatDefaults.apply(
                            separate, test.entries(), marker, true, id -> true),
                    "Deleted candidates not restored");
            expect(
                    separate.<List<String>>get(pool).isEmpty(),
                    "Empty pool retained after initialization");
            separate.set(marker, false);
            expect(
                    FishingEntityCompatDefaults.apply(
                            separate, test.entries(), marker, true, first::equals),
                    "Reset retries with partial registry");
            expect(
                    separate.<List<String>>get(pool).equals(test.entries().subList(0, 1)),
                    "Only available entity appended");
            separate.remove(marker);
            separate.set(pool, List.of());
            expect(
                    FishingEntityCompatDefaults.apply(
                            separate, test.entries(), marker, true, id -> false),
                    "No registered candidate still records source attempt");
            expect(Boolean.TRUE.equals(separate.get(marker)), "Zero additions marked");
            expect(
                    !FishingEntityCompatDefaults.apply(
                            separate, test.entries(), marker, true, id -> true),
                    "Later availability does not retry");
            separate.remove(marker);
            separate.set(pool, test.entries());
            expect(
                    FishingEntityCompatDefaults.apply(
                            separate, test.entries(), marker, true, id -> true),
                    "Already complete pool recorded");
            expect(
                    separate.<List<String>>get(pool).equals(test.entries()),
                    "Existing complete list retained");
        }
        expect(
                ids.size() == 15
                        && ids.stream().noneMatch(id -> id.startsWith("starfantasy_goety:")),
                "Fifteen candidates excluding Apollyon");
        expect(
                config.<List<String>>get(RewardCompatDefaults.FISH_POOL)
                        .equals(List.of("minecraft:apple|6|1|5")),
                "Fishing items unchanged");
        expect(
                config.<List<String>>get(RewardCompatDefaults.PHANTOM_POOL)
                        .equals(List.of("minecraft:diamond|3|1|2")),
                "Phantom rewards unchanged");
        expect(
                config.<Double>get("travail.witness.fishing.fishingEntityChance") == 0.3,
                "Trigger chance unchanged");
        System.out.println("FISHING_ENTITY_DEFAULTS_PASS checks=" + checks);
    }
}

package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.thelongtravail.compat.celestialenchantments.CelestialEnchantmentsGoldenAgeDefaults;
import com.thelongtravail.compat.enigmaticdelicacy.EnigmaticDelicacyGoldenAgeDefaults;
import com.thelongtravail.compat.enigmaticlegacy.EnigmaticLegacyGoldenAgeDefaults;
import com.thelongtravail.compat.l2complements.L2ComplementsGoldenAgeDefaults;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class GoldenAgeCompatDefaultsTest {
    private static final GoldenAgeCompatDefaults.Pool FOOD = GoldenAgeCompatDefaults.Pool.FOOD;
    private static final GoldenAgeCompatDefaults.Pool ENCHANTMENT =
            GoldenAgeCompatDefaults.Pool.ENCHANTMENT;
    private static final List<String> FOODS =
            List.of("enigmaticlegacy:astral_fruit", "enigmaticdelicacy:abyssal_stew");
    private static final List<String> ENCHANTMENTS =
            List.of(
                    "l2complements:projectile_reject", "l2complements:fire_reject",
                    "l2complements:environment_reject", "l2complements:explosion_reject",
                    "l2complements:magic_reject", "l2complements:invincible",
                    "celestial_enchantments:death_blade",
                            "celestial_enchantments:celestial_shelter");
    private static int checks;

    private static void expect(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }

    private static CommentedConfig fresh() {
        var config = CommentedConfig.inMemory();
        config.set(FOOD.path(), List.of("minecraft:apple", "example:custom_food"));
        config.set(ENCHANTMENT.path(), List.of("minecraft:mending", "example:custom_enchantment"));
        config.set("items.golden_age.minNutrition", 4.0);
        config.set("travail.witness.positiveEffects", List.of("minecraft:strength|3|9"));
        config.set("travail.witness.compatInitialization.l2complements.fishingItemsAdded", true);
        return config;
    }

    public static void main(String[] args) {
        expect(
                EnigmaticLegacyGoldenAgeDefaults.foodBlacklist().equals(FOODS.subList(0, 1)),
                "Verified celestial fruit ID");
        expect(
                EnigmaticDelicacyGoldenAgeDefaults.foodBlacklist().equals(FOODS.subList(1, 2)),
                "Verified abyssal stew ID");
        expect(
                L2ComplementsGoldenAgeDefaults.enchantmentBlacklist()
                        .equals(ENCHANTMENTS.subList(0, 6)),
                "Verified six L2 immunity IDs");
        expect(
                CelestialEnchantmentsGoldenAgeDefaults.enchantmentBlacklist()
                        .equals(ENCHANTMENTS.subList(6, 8)),
                "Verified two celestial enchantments");
        var config = fresh();
        expect(
                !GoldenAgeCompatDefaults.apply(config, mod -> false, id -> true, id -> true),
                "Absent mods do nothing");
        expect(
                !config.contains("items.golden_age.compatInitialization"),
                "Absent mods remain pending");
        var food = new ArrayList<>(config.<List<String>>get(FOOD.path()));
        food.add(FOODS.get(0));
        var enchantments = new ArrayList<>(config.<List<String>>get(ENCHANTMENT.path()));
        enchantments.add(ENCHANTMENTS.get(0));
        config.set(FOOD.path(), food);
        config.set(ENCHANTMENT.path(), enchantments);
        expect(
                GoldenAgeCompatDefaults.apply(
                        config, mod -> true, FOODS::contains, ENCHANTMENTS::contains),
                "Initialize with matching registries");
        food.add(FOODS.get(1));
        enchantments.addAll(ENCHANTMENTS.subList(1, 8));
        expect(food.equals(config.get(FOOD.path())), "Manual food values and order preserved");
        expect(
                enchantments.equals(config.get(ENCHANTMENT.path())),
                "Manual enchantments and order preserved");
        expect(
                new HashSet<>(config.<List<String>>get(FOOD.path())).size() == food.size(),
                "No food duplicates");
        expect(
                new HashSet<>(config.<List<String>>get(ENCHANTMENT.path())).size()
                        == enchantments.size(),
                "No enchantment duplicates");
        expect(
                config.<Double>get("items.golden_age.minNutrition") == 4.0,
                "Food threshold unchanged");
        expect(
                List.of("minecraft:strength|3|9")
                        .equals(config.get("travail.witness.positiveEffects")),
                "Witness effect pool unchanged");
        expect(
                Boolean.TRUE.equals(
                        config.get(
                                "travail.witness.compatInitialization.l2complements.fishingItemsAdded")),
                "Reward marker independent");
        expect(
                !GoldenAgeCompatDefaults.apply(config, mod -> true, id -> true, id -> true),
                "Repeated load is idempotent");
        for (String mod : List.of("enigmaticlegacy", "enigmaticdelicacy")) {
            expect(Boolean.TRUE.equals(config.get(FOOD.marker(mod))), "Food marker set");
            expect(!config.contains(ENCHANTMENT.marker(mod)), "No unused enchantment marker");
        }
        for (String mod : List.of("l2complements", "celestial_enchantments")) {
            expect(
                    Boolean.TRUE.equals(config.get(ENCHANTMENT.marker(mod))),
                    "Enchantment marker set");
            expect(!config.contains(FOOD.marker(mod)), "No unused food marker");
        }
        config.set(FOOD.path(), List.of());
        config.set(ENCHANTMENT.path(), List.of());
        expect(
                !GoldenAgeCompatDefaults.apply(config, mod -> true, id -> true, id -> true),
                "Deleted lists not refilled");
        config.set(FOOD.marker("enigmaticlegacy"), false);
        expect(
                GoldenAgeCompatDefaults.apply(config, mod -> true, id -> true, id -> true),
                "Single food source reset");
        expect(List.of(FOODS.get(0)).equals(config.get(FOOD.path())), "Only reset source returns");
        expect(
                config.<List<String>>get(ENCHANTMENT.path()).isEmpty(),
                "Enchantment deletions retained");
        config.remove(ENCHANTMENT.marker("l2complements"));
        expect(
                GoldenAgeCompatDefaults.apply(
                        config, mod -> true, id -> true, ENCHANTMENTS.get(4)::equals),
                "Retry partial enchantment registry");
        expect(
                List.of(ENCHANTMENTS.get(4)).equals(config.get(ENCHANTMENT.path())),
                "Only registered enchantment appended");
        expect(
                !GoldenAgeCompatDefaults.apply(config, mod -> true, id -> true, id -> true),
                "Later registry additions do not refill");
        config = fresh();
        expect(
                GoldenAgeCompatDefaults.apply(config, mod -> true, id -> false, id -> false),
                "Zero candidates still marks present sources");
        expect(
                !GoldenAgeCompatDefaults.apply(config, mod -> true, id -> true, id -> true),
                "Zero-addition initialization persists");
        config = fresh();
        expect(
                GoldenAgeCompatDefaults.apply(
                        config, "l2complements"::equals, id -> true, id -> false),
                "Mod loaded but enchantments absent");
        expect(
                !config.contains(FOOD.marker("enigmaticlegacy")),
                "Unrelated absent source not marked");
        expect(
                Boolean.TRUE.equals(config.get(ENCHANTMENT.marker("l2complements"))),
                "Missing candidates record attempt");
        expect(
                config.<List<String>>get(ENCHANTMENT.path()).size() == 2,
                "Item presence cannot satisfy enchantment registry");
        independentCategory();
        System.out.println("GOLDEN_AGE_COMPAT_DEFAULTS_PASS checks=" + checks);
    }

    private static void independentCategory() {
        var config = fresh();
        var ids = List.of("example:shared", "example:other");
        config.set(FOOD.marker("example"), true);
        expect(
                !GoldenAgeCompatDefaults.add(config, FOOD, "example", ids, true, id -> true),
                "Pre-marked food skipped");
        expect(
                GoldenAgeCompatDefaults.add(config, ENCHANTMENT, "example", ids, true, id -> true),
                "Same mod enchantment category independent");
        expect(config.<List<String>>get(FOOD.path()).size() == 2, "Other category list retained");
        expect(
                config.<List<String>>get(ENCHANTMENT.path()).subList(2, 4).equals(ids),
                "Own category candidates appended");
        expect(
                !GoldenAgeCompatDefaults.add(config, FOOD, "empty", List.of(), true, id -> true),
                "No preset ignored");
        expect(!config.contains(FOOD.marker("empty")), "No marker for empty preset");
    }

    private GoldenAgeCompatDefaultsTest() {}
}

package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.thelongtravail.compat.celestialenchantments.CelestialEnchantmentsGoldenAgeDefaults;
import com.thelongtravail.compat.enigmaticdelicacy.EnigmaticDelicacyGoldenAgeDefaults;
import com.thelongtravail.compat.enigmaticlegacy.EnigmaticLegacyGoldenAgeDefaults;
import com.thelongtravail.compat.l2complements.L2ComplementsGoldenAgeDefaults;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

public final class GoldenAgeCompatDefaults {
    enum Pool {
        FOOD("foodBlacklist", "foodBlacklistAdded"),
        ENCHANTMENT("enchantmentBlacklist", "enchantmentBlacklistAdded");

        private final String key;
        private final String marker;

        Pool(String key, String marker) {
            this.key = key;
            this.marker = marker;
        }

        String path() {
            return "items.golden_age." + key;
        }

        String marker(String mod) {
            return "items.golden_age.compatInitialization." + mod + "." + marker;
        }
    }

    private record Preset(String mod, String name, Pool pool, List<String> entries) {}

    private static final List<Preset> PRESETS =
            List.of(
                    new Preset(
                            "enigmaticlegacy",
                            "Enigmatic Legacy",
                            Pool.FOOD,
                            EnigmaticLegacyGoldenAgeDefaults.foodBlacklist()),
                    new Preset(
                            "enigmaticdelicacy",
                            "Enigmatic Delicacy",
                            Pool.FOOD,
                            EnigmaticDelicacyGoldenAgeDefaults.foodBlacklist()),
                    new Preset(
                            "l2complements",
                            "L2 Complements",
                            Pool.ENCHANTMENT,
                            L2ComplementsGoldenAgeDefaults.enchantmentBlacklist()),
                    new Preset(
                            "celestial_enchantments",
                            "Celestial Enchantments",
                            Pool.ENCHANTMENT,
                            CelestialEnchantmentsGoldenAgeDefaults.enchantmentBlacklist()));
    private static ManagedConfig loaded;

    static void define(ForgeConfigSpec.Builder builder) {
        for (Preset preset : PRESETS)
            builder.comment(preset.name()).define(preset.pool().marker(preset.mod()), false);
    }

    static void loaded(ModConfigEvent.Loading event) {
        if (event.getConfig() instanceof ManagedConfig config
                && config.getSpec() == TravailConfig.SPECS.get("aspects/far_reach.toml"))
            loaded = config;
    }

    static void starting(ServerStartingEvent event) {
        if (loaded == null) return;
        loaded.update(
                config ->
                        apply(
                                config,
                                ModList.get()::isLoaded,
                                id -> ForgeRegistries.ITEMS.containsKey(new ResourceLocation(id)),
                                id ->
                                        ForgeRegistries.ENCHANTMENTS.containsKey(
                                                new ResourceLocation(id))));
    }

    public static boolean apply(
            CommentedConfig config,
            Predicate<String> modPresent,
            Predicate<String> itemPresent,
            Predicate<String> enchantmentPresent) {
        boolean changed = false;
        for (Preset preset : PRESETS)
            changed =
                    add(
                                    config,
                                    preset.pool(),
                                    preset.mod(),
                                    preset.entries(),
                                    modPresent.test(preset.mod()),
                                    preset.pool() == Pool.FOOD ? itemPresent : enchantmentPresent)
                            || changed;
        return changed;
    }

    static boolean add(
            CommentedConfig config,
            Pool pool,
            String mod,
            List<String> preset,
            boolean present,
            Predicate<String> exists) {
        String marker = pool.marker(mod);
        if (!present || preset.isEmpty() || Boolean.TRUE.equals(config.get(marker))) return false;
        List<String> entries = new ArrayList<>(config.<List<String>>get(pool.path()));
        var configured = new HashSet<>(entries);
        for (String id : preset) if (exists.test(id) && configured.add(id)) entries.add(id);
        config.set(pool.path(), entries);
        config.set(marker, true);
        return true;
    }

    private GoldenAgeCompatDefaults() {}
}

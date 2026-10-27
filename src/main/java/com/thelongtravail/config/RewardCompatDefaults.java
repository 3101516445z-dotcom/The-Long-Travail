package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.compat.apprenticecodex.ApprenticeCodexRewardDefaults;
import com.thelongtravail.compat.celestialcore.CelestialCoreRewardDefaults;
import com.thelongtravail.compat.curseofpandora.CurseOfPandoraRewardDefaults;
import com.thelongtravail.compat.enigmaticaddons.EnigmaticAddonsRewardDefaults;
import com.thelongtravail.compat.enigmaticdelicacy.EnigmaticDelicacyRewardDefaults;
import com.thelongtravail.compat.enigmaticlegacy.EnigmaticLegacyRewardDefaults;
import com.thelongtravail.compat.ironsspellbooks.IronsSpellbooksRewardDefaults;
import com.thelongtravail.compat.l2complements.L2ComplementsRewardDefaults;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class RewardCompatDefaults {
    public static final String FISH_POOL = "travail.witness.fishing.fishingSpecialItems";
    public static final String PHANTOM_POOL = "travail.witness.phantom.phantomDrops";

    record Preset(
            String namespace, String name, List<String> fishingItems, List<String> phantomDrops) {
        List<String> entries(TravailAspect aspect) {
            return switch (aspect) {
                case UNDERWORLD -> fishingItems;
                case BOUNDLESS -> phantomDrops;
                default -> List.of();
            };
        }
    }

    record Pool(TravailAspect aspect, String path, String marker) {}

    static final Pool FISHING = new Pool(TravailAspect.UNDERWORLD, FISH_POOL, "fishingItemsAdded");
    static final Pool PHANTOM =
            new Pool(TravailAspect.BOUNDLESS, PHANTOM_POOL, "phantomDropsAdded");

    private static final List<Pool> POOLS = List.of(FISHING, PHANTOM);
    private static final List<Preset> PRESETS =
            List.of(
                    new Preset(
                            "l2complements",
                            "L2 Complements",
                            L2ComplementsRewardDefaults.fishingItems(),
                            L2ComplementsRewardDefaults.phantomDrops()),
                    new Preset(
                            "curseofpandora",
                            "Curse of Pandora",
                            CurseOfPandoraRewardDefaults.fishingItems(),
                            CurseOfPandoraRewardDefaults.phantomDrops()),
                    new Preset(
                            "celestial_core",
                            "Celestial Core",
                            CelestialCoreRewardDefaults.fishingItems(),
                            CelestialCoreRewardDefaults.phantomDrops()),
                    new Preset(
                            "irons_spellbooks",
                            "Iron's Spells and Spellbooks",
                            IronsSpellbooksRewardDefaults.fishingItems(),
                            IronsSpellbooksRewardDefaults.phantomDrops()),
                    new Preset(
                            "apprenticecodex",
                            "Apprentice Codex",
                            ApprenticeCodexRewardDefaults.fishingItems(),
                            ApprenticeCodexRewardDefaults.phantomDrops()),
                    new Preset(
                            "enigmaticlegacy",
                            "Enigmatic Legacy",
                            EnigmaticLegacyRewardDefaults.fishingItems(),
                            EnigmaticLegacyRewardDefaults.phantomDrops()),
                    new Preset(
                            "enigmaticaddons",
                            "Enigmatic Addons",
                            EnigmaticAddonsRewardDefaults.fishingItems(),
                            EnigmaticAddonsRewardDefaults.phantomDrops()),
                    new Preset(
                            "enigmaticdelicacy",
                            "Enigmatic Delicacy",
                            EnigmaticDelicacyRewardDefaults.fishingItems(),
                            EnigmaticDelicacyRewardDefaults.phantomDrops()));
    private static final Map<TravailAspect, ManagedConfig> LOADED =
            new EnumMap<>(TravailAspect.class);

    private static String marker(Pool pool, Preset preset) {
        return "travail.witness.compatInitialization." + preset.namespace() + "." + pool.marker();
    }

    static void define(ForgeConfigSpec.Builder builder, TravailAspect aspect) {
        for (Pool pool : POOLS) {
            if (pool.aspect() != aspect) continue;
            for (Preset preset : PRESETS) {
                if (preset.entries(aspect).isEmpty()) continue;
                builder.comment(
                                ConfigFilesDescription.presetMarker(
                                        preset.name(),
                                        pool == FISHING ? "钓鱼物品" : "幻翼掉落物",
                                        pool == FISHING ? "fishing item" : "phantom drop"))
                        .define(marker(pool, preset), false);
            }
        }
    }

    static void loaded(ModConfigEvent.Loading event) {
        if (!(event.getConfig() instanceof ManagedConfig config)) return;
        for (Pool pool : POOLS) {
            if (config.getSpec()
                    == TravailConfig.SPECS.get("aspects/" + pool.aspect().id() + ".toml"))
                LOADED.put(pool.aspect(), config);
        }
    }

    static void starting(ServerStartingEvent event) {
        var namespaces = new HashSet<String>();
        ForgeRegistries.ITEMS.getKeys().forEach(id -> namespaces.add(id.getNamespace()));
        var presets =
                PRESETS.stream().filter(preset -> namespaces.contains(preset.namespace())).toList();
        if (presets.isEmpty()) return;
        Predicate<String> exists =
                id -> ForgeRegistries.ITEMS.containsKey(new ResourceLocation(id));
        for (Pool pool : POOLS) {
            ManagedConfig file = LOADED.get(pool.aspect());
            if (file == null) continue;
            file.update(
                    config -> {
                        boolean changed = false;
                        for (Preset preset : presets)
                            changed = apply(config, pool, preset, true, exists) || changed;
                        return changed;
                    });
        }
    }

    static boolean apply(
            CommentedConfig config,
            Pool pool,
            Preset preset,
            boolean present,
            Predicate<String> exists) {
        return apply(
                config,
                pool.path(),
                preset.entries(pool.aspect()),
                marker(pool, preset),
                present,
                exists);
    }

    public static boolean apply(
            CommentedConfig config,
            String pool,
            List<String> preset,
            String marker,
            boolean present,
            Predicate<String> exists) {
        return WeightedPresetEntries.apply(config, pool, preset, marker, present, exists);
    }

    private RewardCompatDefaults() {}
}

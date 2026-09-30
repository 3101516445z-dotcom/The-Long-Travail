package com.thelongtravail.data;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Server-thread snapshot. Invalid configurations are diagnosed once, never once per player tick. */
public final class RequirementPools {
    public enum Status { DISABLED, READY, INVALID }
    public record Pool(Status status, List<WeightedEntry> entries, int count) {
        public Pool { entries = List.copyOf(entries); }
    }
    public record AspectPools(Pool biomes, Pool structures) {}
    public record Snapshot(Map<TravailAspect, AspectPools> aspects, boolean valid) {
        public Snapshot { aspects = Map.copyOf(aspects); }
    }

    private static Registry<?> biomeRegistry, structureRegistry;
    private static Snapshot snapshot;

    public static void invalidate() {
        snapshot = null;
        biomeRegistry = null;
        structureRegistry = null;
    }

    public static Snapshot current(ServerPlayer player) {
        var registries = player.serverLevel().registryAccess();
        var biomes = registries.registryOrThrow(Registries.BIOME);
        var structures = registries.registryOrThrow(Registries.STRUCTURE);
        if (snapshot != null && biomeRegistry == biomes && structureRegistry == structures) return snapshot;
        Map<TravailAspect, AspectPools> result = new EnumMap<>(TravailAspect.class);
        boolean valid = true;
        for (TravailAspect aspect : TravailAspect.values()) {
            Pool biomePool = validate(TravailConfig.BIOME_POOLS.get(aspect).get(), TravailConfig.BIOME_DRAW_COUNT.get(),
                    biomes::containsKey, message -> warn(aspect, "biomes", message));
            Pool structurePool = validate(TravailConfig.STRUCTURE_POOLS.get(aspect).get(), TravailConfig.STRUCTURE_DRAW_COUNT.get(),
                    structures::containsKey, message -> warn(aspect, "structures", message));
            result.put(aspect, new AspectPools(biomePool, structurePool));
            valid &= biomePool.status() != Status.INVALID && structurePool.status() != Status.INVALID;
        }
        biomeRegistry = biomes;
        structureRegistry = structures;
        snapshot = new Snapshot(result, valid);
        return snapshot;
    }

    /** Empty lists and zero draws intentionally disable this category; broken nonempty lists do not. */
    public static Pool validate(List<? extends String> configured, int count,
                                Predicate<ResourceLocation> exists, Consumer<String> diagnostic) {
        if (count <= 0 || configured.isEmpty()) return new Pool(Status.DISABLED, List.of(), 0);
        Map<ResourceLocation, WeightedEntry> unique = new LinkedHashMap<>();
        for (String text : configured) {
            WeightedEntry entry = WeightedEntry.parse(text);
            if (entry == null || !exists.test(entry.id())) diagnostic.accept("Ignoring invalid/unavailable target: " + text);
            else if (unique.putIfAbsent(entry.id(), entry) != null) diagnostic.accept("Ignoring duplicate target: " + entry.id());
        }
        if (unique.isEmpty()) {
            diagnostic.accept("No valid targets remain; new journey generation is blocked until the pool is corrected.");
            return new Pool(Status.INVALID, List.of(), 0);
        }
        if (unique.size() < count) diagnostic.accept("Requested " + count + " unique targets, but only " + unique.size() + " are available; drawing all available targets.");
        return new Pool(Status.READY, List.copyOf(unique.values()), Math.min(count, unique.size()));
    }

    private static void warn(TravailAspect aspect, String category, String message) {
        TheLongTravail.LOGGER.warn("Journey pool {}.{}: {}", aspect.id(), category, message);
    }

    private RequirementPools() {}
}

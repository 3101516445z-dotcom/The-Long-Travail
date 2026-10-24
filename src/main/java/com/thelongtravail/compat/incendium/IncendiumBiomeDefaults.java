package com.thelongtravail.compat.incendium;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class IncendiumBiomeDefaults {
    public static List<String> biomes(TravailAspect aspect) {
        return aspect == TravailAspect.UNDERWORLD
                ? List.of(
                        "incendium:ash_barrens",
                        "incendium:infernal_dunes",
                        "incendium:inverted_forest",
                        "incendium:quartz_flats",
                        "incendium:toxic_heap",
                        "incendium:volcanic_deltas",
                        "incendium:weeping_valley",
                        "incendium:withered_forest")
                : List.of();
    }

    private IncendiumBiomeDefaults() {}
}

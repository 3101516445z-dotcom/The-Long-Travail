package com.thelongtravail.compat.terralith;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class TerralithBiomeDefaults {
    public static List<String> biomes(TravailAspect aspect) {
        return switch (aspect) {
            case FLOURISHING ->
                    List.of(
                            "terralith:blooming_plateau",
                            "terralith:blooming_valley",
                            "terralith:cloud_forest",
                            "terralith:lavender_forest",
                            "terralith:moonlight_grove",
                            "terralith:sakura_grove",
                            "terralith:sakura_valley",
                            "terralith:amethyst_rainforest",
                            "terralith:orchid_swamp",
                            "terralith:steppe",
                            "terralith:highlands",
                            "terralith:shield",
                            "terralith:amethyst_canyon",
                            "terralith:rocky_jungle",
                            "terralith:yosemite_lowlands",
                            "terralith:lush_valley");
            case ABYSS ->
                    List.of(
                            "terralith:skylands_winter",
                            "terralith:skylands_summer",
                            "terralith:skylands_autumn",
                            "terralith:skylands_spring",
                            "terralith:alpha_islands",
                            "terralith:warm_river",
                            "terralith:gravel_beach",
                            "terralith:ice_marsh",
                            "terralith:desert_oasis",
                            "terralith:mirage_isles");
            case FAR_REACH ->
                    List.of(
                            "terralith:warped_mesa",
                            "terralith:painted_mountains",
                            "terralith:volcanic_peaks",
                            "terralith:scarlet_mountains",
                            "terralith:white_mesa",
                            "terralith:haze_mountain",
                            "terralith:wintry_forest",
                            "terralith:savanna_badlands",
                            "terralith:lush_desert",
                            "terralith:gravel_desert",
                            "terralith:emerald_peaks",
                            "terralith:rocky_mountains",
                            "terralith:alpine_highlands",
                            "terralith:alpine_grove",
                            "terralith:moonlight_valley",
                            "terralith:jungle_mountains",
                            "terralith:windswept_spires",
                            "terralith:snowy_shield",
                            "terralith:glacial_chasm");
            case DEEP_VALLEY ->
                    List.of(
                            "terralith:cave/diorite_caves",
                            "terralith:cave/tuff_caves",
                            "terralith:cave/frostfire_caves",
                            "terralith:cave/mantle_caves",
                            "terralith:cave/andesite_caves",
                            "terralith:cave/thermal_caves",
                            "terralith:cave/underground_jungle",
                            "terralith:yellowstone",
                            "terralith:stony_spires");
            case UNDERWORLD ->
                    List.of(
                            "terralith:ashen_savanna",
                            "terralith:fractured_savanna",
                            "terralith:hot_shrubland",
                            "terralith:desert_spires");
            case BOUNDLESS ->
                    List.of(
                            "terralith:snowy_badlands",
                            "terralith:wintry_lowlands",
                            "terralith:ancient_sands",
                            "terralith:bryce_canyon",
                            "terralith:siberian_taiga",
                            "terralith:red_oasis",
                            "terralith:cold_shrubland");
        };
    }

    private TerralithBiomeDefaults() {}
}

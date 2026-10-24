package com.thelongtravail.compat.alexscaves;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class AlexsCavesBiomeDefaults {
    public static List<String> biomes(TravailAspect aspect) {
        return switch (aspect) {
            case FLOURISHING -> List.of("alexscaves:primordial_caves", "alexscaves:candy_cavity");
            case ABYSS -> List.of("alexscaves:abyssal_chasm");
            case DEEP_VALLEY -> List.of("alexscaves:toxic_caves", "alexscaves:magnetic_caves");
            case UNDERWORLD -> List.of("alexscaves:forlorn_hollows");
            default -> List.of();
        };
    }

    private AlexsCavesBiomeDefaults() {}
}

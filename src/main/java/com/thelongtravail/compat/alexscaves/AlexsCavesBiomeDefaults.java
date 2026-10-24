package com.thelongtravail.compat.alexscaves;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class AlexsCavesBiomeDefaults {
    public static List<String> biomes(TravailAspect aspect) {
        return aspect == TravailAspect.DEEP_VALLEY
                ? List.of("alexscaves:toxic_caves", "alexscaves:magnetic_caves")
                : List.of();
    }

    private AlexsCavesBiomeDefaults() {}
}

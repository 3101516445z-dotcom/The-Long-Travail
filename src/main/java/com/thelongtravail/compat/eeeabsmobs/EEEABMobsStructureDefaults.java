package com.thelongtravail.compat.eeeabsmobs;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class EEEABMobsStructureDefaults {
    public static List<String> structures(TravailAspect aspect) {
        return switch (aspect) {
            case DEEP_VALLEY -> List.of("eeeabsmobs:coreforge_ruins");
            case UNDERWORLD -> List.of("eeeabsmobs:bloody_altar");
            default -> List.of();
        };
    }

    private EEEABMobsStructureDefaults() {}
}

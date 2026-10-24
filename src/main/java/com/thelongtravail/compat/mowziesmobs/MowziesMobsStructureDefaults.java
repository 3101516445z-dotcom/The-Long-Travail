package com.thelongtravail.compat.mowziesmobs;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class MowziesMobsStructureDefaults {
    public static List<String> structures(TravailAspect aspect) {
        return switch (aspect) {
            case FLOURISHING -> List.of("mowziesmobs:umvuthana_grove");
            case FAR_REACH -> List.of("mowziesmobs:monastery");
            case DEEP_VALLEY -> List.of("mowziesmobs:wrought_chamber");
            default -> List.of();
        };
    }

    private MowziesMobsStructureDefaults() {}
}

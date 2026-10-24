package com.thelongtravail.compat.mowziesmobs;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class MowziesMobsStructureDefaults {
    public static List<String> structures(TravailAspect aspect) {
        return switch (aspect) {
            case FAR_REACH -> List.of("mowziesmobs:monastery");
            default -> List.of();
        };
    }

    private MowziesMobsStructureDefaults() {}
}

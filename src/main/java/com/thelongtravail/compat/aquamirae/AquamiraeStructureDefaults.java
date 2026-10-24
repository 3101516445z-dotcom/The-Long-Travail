package com.thelongtravail.compat.aquamirae;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class AquamiraeStructureDefaults {
    public static List<String> structures(TravailAspect aspect) {
        return switch (aspect) {
            case ABYSS -> List.of("aquamirae:pirate_shelter", "aquamirae:pirate_outpost");
            default -> List.of();
        };
    }

    private AquamiraeStructureDefaults() {}
}

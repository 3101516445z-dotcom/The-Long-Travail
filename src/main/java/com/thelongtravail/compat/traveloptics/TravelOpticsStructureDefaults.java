package com.thelongtravail.compat.traveloptics;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class TravelOpticsStructureDefaults {
    public static List<String> structures(TravailAspect aspect) {
        return switch (aspect) {
            case UNDERWORLD -> List.of("traveloptics:echo_shrine");
            default -> List.of();
        };
    }

    private TravelOpticsStructureDefaults() {}
}

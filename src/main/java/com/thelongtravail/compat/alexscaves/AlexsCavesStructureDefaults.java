package com.thelongtravail.compat.alexscaves;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class AlexsCavesStructureDefaults {
    public static List<String> structures(TravailAspect aspect) {
        return switch (aspect) {
            case DEEP_VALLEY -> List.of("alexscaves:gingerbread_town");
            default -> List.of();
        };
    }

    private AlexsCavesStructureDefaults() {}
}

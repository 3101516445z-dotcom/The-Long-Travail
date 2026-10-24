package com.thelongtravail.compat.arsnouveau;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class ArsNouveauStructureDefaults {
    public static List<String> structures(TravailAspect aspect) {
        return switch (aspect) {
            case DEEP_VALLEY ->
                    List.of(
                            "ars_nouveau:stalker_wilden_den",
                            "ars_nouveau:guardian_wilden_den",
                            "ars_nouveau:hunter_wilden_den");
            default -> List.of();
        };
    }

    private ArsNouveauStructureDefaults() {}
}

package com.thelongtravail.compat.cataclysm;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class CataclysmStructureDefaults {
    public static List<String> structures(TravailAspect aspect) {
        return switch (aspect) {
            case ABYSS -> List.of("cataclysm:acropolis", "cataclysm:sunken_city");
            case FAR_REACH -> List.of("cataclysm:cursed_pyramid", "cataclysm:frosted_prison");
            case DEEP_VALLEY -> List.of("cataclysm:ancient_factory");
            case UNDERWORLD -> List.of("cataclysm:burning_arena", "cataclysm:soul_black_smith");
            case BOUNDLESS -> List.of("cataclysm:ruined_citadel");
            default -> List.of();
        };
    }

    private CataclysmStructureDefaults() {}
}

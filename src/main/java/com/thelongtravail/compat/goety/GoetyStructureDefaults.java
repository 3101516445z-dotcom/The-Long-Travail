package com.thelongtravail.compat.goety;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class GoetyStructureDefaults {
    public static List<String> structures(TravailAspect aspect) {
        return switch (aspect) {
            case FLOURISHING -> List.of("goety:mangrove_tower");
            case FAR_REACH ->
                    List.of(
                            "goety:ominous_blacksmith",
                            "goety:buried_ritual",
                            "goety:secluded_igloo",
                            "goety:wind_shrine");
            case DEEP_VALLEY -> List.of("goety:crypt");
            case UNDERWORLD -> List.of("goety:dark_manor");
            case BOUNDLESS -> List.of("goety:royal_tribute");
            default -> List.of();
        };
    }

    private GoetyStructureDefaults() {}
}

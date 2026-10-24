package com.thelongtravail.compat.goetyawaken;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class GoetyAwakenStructureDefaults {
    public static List<String> structures(TravailAspect aspect) {
        return switch (aspect) {
            case FLOURISHING -> List.of("goetyawaken:rabbit_shed", "goetyawaken:pristine_shack");
            case ABYSS -> List.of("goetyawaken:tidal_tomb");
            case FAR_REACH ->
                    List.of(
                            "goetyawaken:gauntlet_of_howling",
                            "goetyawaken:boreal_spire",
                            "goetyawaken:abandoned_tower",
                            "goetyawaken:nocturnal_rite");
            case DEEP_VALLEY -> List.of("goetyawaken:the_mausoleum");
            case UNDERWORLD -> List.of("goetyawaken:temple_of_apollyon");
            default -> List.of();
        };
    }

    private GoetyAwakenStructureDefaults() {}
}

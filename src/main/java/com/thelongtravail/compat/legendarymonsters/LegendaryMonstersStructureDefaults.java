package com.thelongtravail.compat.legendarymonsters;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class LegendaryMonstersStructureDefaults {
    public static List<String> structures(TravailAspect aspect) {
        return switch (aspect) {
            case FLOURISHING -> List.of("legendary_monsters:mossy_temple");
            case BOUNDLESS ->
                    List.of("legendary_monsters:shulker_tower", "legendary_monsters:space_station");
            default -> List.of();
        };
    }

    private LegendaryMonstersStructureDefaults() {}
}

package com.thelongtravail.compat.townsandtowers;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class TownsAndTowersStructureDefaults {
    public static List<String> structures(TravailAspect aspect) {
        return switch (aspect) {
            case ABYSS ->
                    List.of(
                            "towns_and_towers:village_mushroom_fields",
                            "towns_and_towers:pillager_outpost_mushroom_fields",
                            "towns_and_towers:village_beach",
                            "towns_and_towers:pillager_outpost_beach",
                            "towns_and_towers:village_swamp",
                            "towns_and_towers:pillager_outpost_swamp",
                            "towns_and_towers:wreckage_ocean");
            case BOUNDLESS -> List.of("towns_and_towers:exclusives/village_wandering_trader_camp");
            default -> List.of();
        };
    }

    private TownsAndTowersStructureDefaults() {}
}

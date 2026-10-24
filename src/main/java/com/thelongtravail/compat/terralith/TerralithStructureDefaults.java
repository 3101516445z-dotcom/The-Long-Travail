package com.thelongtravail.compat.terralith;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class TerralithStructureDefaults {
    public static List<String> structures(TravailAspect aspect) {
        return switch (aspect) {
            case FLOURISHING ->
                    List.of(
                            "terralith:rubble_forest",
                            "terralith:rubble_jungle",
                            "terralith:rubble_taiga",
                            "terralith:mage_tower_summer",
                            "terralith:mage_tower_spring",
                            "terralith:mage_tower_autumn",
                            "terralith:mage_tower",
                            "terralith:mage_complex");
            case ABYSS -> List.of("terralith:witch_hut");
            case FAR_REACH ->
                    List.of("terralith:desert_outpost", "terralith:glacial_hut", "terralith:spire");
            case DEEP_VALLEY ->
                    List.of(
                            "terralith:underground/oak_cabin",
                            "terralith:underground/mining_outpost",
                            "terralith:underground/old_refinery",
                            "terralith:underground/sunken_tower",
                            "terralith:underground/frosted_dungeon");
            case UNDERWORLD -> List.of("terralith:underground/giant_bee_hive");
            default -> List.of();
        };
    }

    private TerralithStructureDefaults() {}
}

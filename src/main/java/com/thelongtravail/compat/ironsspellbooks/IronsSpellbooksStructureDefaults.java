package com.thelongtravail.compat.ironsspellbooks;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class IronsSpellbooksStructureDefaults {
    public static List<String> structures(TravailAspect aspect) {
        return switch (aspect) {
            case FLOURISHING -> List.of("irons_spellbooks:mangrove_hut");
            case ABYSS -> List.of("irons_spellbooks:impaled_icebreaker");
            case FAR_REACH ->
                    List.of("irons_spellbooks:ice_spider_den", "irons_spellbooks:mountain_tower");
            case DEEP_VALLEY -> List.of("irons_spellbooks:catacombs");
            case UNDERWORLD -> List.of("irons_spellbooks:ancient_battleground");
            case BOUNDLESS -> List.of("irons_spellbooks:evoker_fort");
            default -> List.of();
        };
    }

    private IronsSpellbooksStructureDefaults() {}
}

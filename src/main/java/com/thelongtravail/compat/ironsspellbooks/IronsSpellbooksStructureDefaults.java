package com.thelongtravail.compat.ironsspellbooks;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class IronsSpellbooksStructureDefaults {
    public static List<String> structures(TravailAspect aspect) {
        return switch (aspect) {
            case ABYSS -> List.of("irons_spellbooks:impaled_icebreaker");
            case FAR_REACH -> List.of("irons_spellbooks:ice_spider_den");
            case UNDERWORLD -> List.of("irons_spellbooks:ancient_battleground");
            case BOUNDLESS -> List.of("irons_spellbooks:evoker_fort");
            default -> List.of();
        };
    }

    private IronsSpellbooksStructureDefaults() {}
}

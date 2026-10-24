package com.thelongtravail.compat.maidspell;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class MaidSpellStructureDefaults {
    public static List<String> structures(TravailAspect aspect) {
        return switch (aspect) {
            case FLOURISHING -> List.of("touhou_little_maid_spell:yin_yang_altar");
            case FAR_REACH -> List.of("touhou_little_maid_spell:starfall_garden");
            case BOUNDLESS ->
                    List.of(
                            "touhou_little_maid_spell:stellar_endshore",
                            "touhou_little_maid_spell:starwatch_tower");
            default -> List.of();
        };
    }

    private MaidSpellStructureDefaults() {}
}

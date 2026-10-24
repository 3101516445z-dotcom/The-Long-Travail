package com.thelongtravail.compat.maidspell;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class MaidSpellStructureDefaults {
    public static List<String> structures(TravailAspect aspect) {
        return switch (aspect) {
            case FLOURISHING ->
                    List.of(
                            "touhou_little_maid_spell:hidden_cherry_tree",
                            "touhou_little_maid_spell:elven_realm");
            case UNDERWORLD -> List.of("touhou_little_maid_spell:fallen_sanctum");
            default -> List.of();
        };
    }

    private MaidSpellStructureDefaults() {}
}

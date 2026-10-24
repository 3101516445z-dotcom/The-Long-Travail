package com.thelongtravail.compat.incendium;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class IncendiumStructureDefaults {
    public static List<String> structures(TravailAspect aspect) {
        return switch (aspect) {
            case UNDERWORLD ->
                    List.of(
                            "incendium:sanctum",
                            "incendium:ruined_lab",
                            "incendium:infernal_altar",
                            "incendium:forbidden_castle",
                            "incendium:nether_reactor");
            case BOUNDLESS -> List.of("incendium:quartz_kitchen");
            default -> List.of();
        };
    }

    private IncendiumStructureDefaults() {}
}

package com.thelongtravail.compat.celestialenchantments;

import java.util.List;

public final class CelestialEnchantmentsGoldenAgeDefaults {
    public static List<String> enchantmentBlacklist() {
        return List.of(
                "celestial_enchantments:death_blade", "celestial_enchantments:celestial_shelter");
    }

    private CelestialEnchantmentsGoldenAgeDefaults() {}
}

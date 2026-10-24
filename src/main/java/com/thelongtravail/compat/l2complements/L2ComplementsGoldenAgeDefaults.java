package com.thelongtravail.compat.l2complements;

import java.util.List;

public final class L2ComplementsGoldenAgeDefaults {
    public static List<String> enchantmentBlacklist() {
        return List.of(
                "l2complements:projectile_reject",
                "l2complements:fire_reject",
                "l2complements:environment_reject",
                "l2complements:explosion_reject",
                "l2complements:magic_reject",
                "l2complements:invincible");
    }

    private L2ComplementsGoldenAgeDefaults() {}
}

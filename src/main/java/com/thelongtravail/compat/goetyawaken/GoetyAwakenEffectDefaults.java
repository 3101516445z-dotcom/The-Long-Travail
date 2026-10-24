package com.thelongtravail.compat.goetyawaken;

import java.util.List;

public final class GoetyAwakenEffectDefaults {
    public static List<String> farReach() {
        return List.of("goetyawaken:echo|1|1", "goetyawaken:critical_hit|2|1");
    }

    public static List<String> deepValley() {
        return List.of("goetyawaken:echo|1|1", "goetyawaken:critical_hit|2|1");
    }

    private GoetyAwakenEffectDefaults() {}
}

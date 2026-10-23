package com.thelongtravail.compat.fluid;

import com.thelongtravail.TheLongTravail;
import java.util.HashMap;
import java.util.Map;

public final class FluidCopyFailures {
    private static final Map<String, Long> NEXT_WARNING = new HashMap<>();

    public static synchronized void report(String stage, Exception failure) {
        long now = System.nanoTime();
        Long next = NEXT_WARNING.get(stage);
        if (next != null && now - next < 0) return;
        NEXT_WARNING.put(stage, now + 60_000_000_000L);
        TheLongTravail.LOGGER.warn("Perfect Fluid copy rejected during {}", stage, failure);
    }

    private FluidCopyFailures() {}
}

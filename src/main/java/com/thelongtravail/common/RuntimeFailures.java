package com.thelongtravail.common;

import com.thelongtravail.TheLongTravail;
import java.util.LinkedHashMap;
import java.util.Map;

/** 外部数据与回调异常按来源限频，保留完整堆栈。 */
public final class RuntimeFailures {
    private static final Map<String, Long> NEXT = new LinkedHashMap<>();

    public static synchronized void report(String source, RuntimeException failure) {
        long now = System.nanoTime();
        Long next = NEXT.get(source);
        if (next != null && now - next < 0) return;
        if (NEXT.size() >= 128 && !NEXT.containsKey(source))
            NEXT.remove(NEXT.keySet().iterator().next());
        NEXT.put(source, now + 60_000_000_000L);
        TheLongTravail.LOGGER.warn("TLT operation cancelled or entry skipped: {}", source, failure);
    }

    private RuntimeFailures() {}
}

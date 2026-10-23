package com.thelongtravail.config;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

final class ConfigDiagnostics {
    private static final int CAPACITY = 1024;
    private static final int DETAIL_LIMIT = 20;
    private static final int FILE_LIMIT = 16;
    private static final long WINDOW = TimeUnit.MINUTES.toNanos(1);

    private final Set<String> problems = new LinkedHashSet<>();
    private final ArrayDeque<Long> detailed = new ArrayDeque<>();
    private final Set<String> files = new LinkedHashSet<>();
    private final LongSupplier clock;
    private final Consumer<String> output;
    private long suppressed;
    private long suppressedSince;
    private boolean moreFiles;

    ConfigDiagnostics(LongSupplier clock, Consumer<String> output) {
        this.clock = clock;
        this.output = output;
    }

    synchronized void report(Path file, String detail) {
        long now = clock.getAsLong();
        flush(now, false);
        String message = file + ": " + detail;
        if (problems.contains(message)) return;

        if (problems.size() == CAPACITY) {
            var oldest = problems.iterator();
            oldest.next();
            oldest.remove();
        }
        problems.add(message);

        while (!detailed.isEmpty() && now - detailed.peekFirst() >= WINDOW)
            detailed.removeFirst();
        if (detailed.size() < DETAIL_LIMIT) {
            detailed.addLast(now);
            output.accept(message);
        } else {
            if (suppressed == 0) suppressedSince = now;
            suppressed++;
            String name = file.toString();
            if (files.size() < FILE_LIMIT) files.add(name);
            else if (!files.contains(name)) moreFiles = true;
        }
    }

    synchronized void flush(boolean force) {
        flush(clock.getAsLong(), force);
    }

    private void flush(long now, boolean force) {
        if (suppressed == 0 || (!force && now - suppressedSince < WINDOW)) return;
        output.accept(
                "配置警告过多，已省略 "
                        + suppressed
                        + " 条详细警告 / Suppressed "
                        + suppressed
                        + " additional configuration warnings; files: "
                        + String.join(", ", files)
                        + (moreFiles ? " 等其他文件 / and other files" : ""));
        suppressed = 0;
        files.clear();
        moreFiles = false;
    }

    synchronized int size() {
        return problems.size();
    }
}

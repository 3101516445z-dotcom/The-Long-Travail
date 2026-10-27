package com.thelongtravail.config;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class ConfigDiagnosticsTest {
    private static int checks;
    private static final Path FILE = Path.of("general.toml");

    private static void expect(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }

    private static void capacityAndRecovery() {
        var clock = new AtomicLong();
        var output = new ArrayList<String>();
        var diagnostics = new ConfigDiagnostics(clock::get, output::add);
        for (int i = 0; i < 10000; i++) diagnostics.report(FILE, "same");
        expect(output.size() == 1 && diagnostics.size() == 1, "identical errors deduplicated");
        for (int i = 0; i < 50000; i++) diagnostics.report(FILE, "error-" + i);
        expect(diagnostics.size() == 1024, "capacity after 50000 distinct errors");
        expect(output.size() == 20, "initial flood limited to 20 details");
        diagnostics.report(FILE, "error-49999");
        diagnostics.report(FILE, "same");
        expect(diagnostics.size() == 1024, "evicted message cannot grow cache");
        clock.set(TimeUnit.SECONDS.toNanos(60) - 1);
        diagnostics.flush(false);
        expect(output.size() == 20, "summary waits for window");
        clock.incrementAndGet();
        diagnostics.flush(false);
        expect(output.size() == 21, "idle flush produces one summary");
        expect(
                output.get(20).contains("49982") && output.get(20).contains("general.toml"),
                "summary counts suppressed reports and identifies file");
        diagnostics.flush(false);
        expect(output.size() == 21, "idle flush does not repeat summary");
        diagnostics.report(FILE, "after-cooldown");
        expect(
                output.size() == 22 && output.get(21).endsWith("after-cooldown"),
                "new detail resumes after window");
        diagnostics.report(FILE, "error-49999");
        expect(output.size() == 22, "retained error remains deduplicated across window");
    }

    private static void slidingWindow() {
        var clock = new AtomicLong();
        var timestamps = new ArrayDeque<Long>();
        var diagnostics =
                new ConfigDiagnostics(
                        clock::get,
                        message -> {
                            if (!message.startsWith("general.toml:")) return;
                            long now = clock.get();
                            while (!timestamps.isEmpty()
                                    && now - timestamps.peekFirst() >= TimeUnit.MINUTES.toNanos(1))
                                timestamps.removeFirst();
                            timestamps.addLast(now);
                            if (timestamps.size() > 20)
                                throw new AssertionError("sliding window overflow");
                        });
        for (int i = 0; i < 20000; i++) {
            clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(100));
            diagnostics.report(FILE, "rolling-" + i);
            diagnostics.flush(false);
        }
        expect(
                diagnostics.size() == 1024,
                "continuous reports bounded over 2000 simulated seconds");
        expect(!timestamps.isEmpty(), "continuous reports continue to emit details");
    }

    private static void fileSummary() throws Exception {
        var clock = new AtomicLong();
        var output = new ArrayList<String>();
        var diagnostics = new ConfigDiagnostics(clock::get, output::add);
        for (int i = 0; i < 20; i++) diagnostics.report(FILE, "warmup-" + i);
        for (int i = 0; i < 10000; i++) diagnostics.report(Path.of("file-" + i + ".toml"), "bad");
        var field = ConfigDiagnostics.class.getDeclaredField("files");
        field.setAccessible(true);
        expect(
                ((java.util.Set<?>) field.get(diagnostics)).size() == 16,
                "summary file list bounded");
        diagnostics.flush(true);
        expect(
                output.size() == 21 && output.get(20).contains("10000"),
                "shutdown flushes pending count");
        expect(
                output.get(20).contains("file-0.toml")
                        && output.get(20).contains("file-15.toml")
                        && output.get(20).contains("and other files"),
                "summary reports truncated file list");
        diagnostics.flush(true);
        expect(output.size() == 21, "shutdown flush is idempotent");
        clock.set(TimeUnit.MINUTES.toNanos(1));
        for (int i = 0; i < 21; i++) diagnostics.report(FILE, "second-window-" + i);
        diagnostics.flush(true);
        expect(
                output.size() == 42 && output.get(41).contains("Suppressed 1 "),
                "summary counters reset between windows");
        expect(
                !output.get(41).contains("file-0.toml")
                        && !output.get(41).contains("and other files"),
                "summary files reset between windows");
    }

    private static void concurrentFlood() throws Exception {
        var clock = new AtomicLong();
        var output = new ArrayList<String>();
        var diagnostics = new ConfigDiagnostics(clock::get, output::add);
        var pool = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int worker = 0; worker < 8; worker++) {
                int id = worker;
                futures.add(
                        pool.submit(
                                () -> {
                                    for (int i = 0; i < 50000; i++) {
                                        diagnostics.report(
                                                Path.of("worker-" + id + ".toml"), "bad-" + i);
                                        if (diagnostics.size() > 1024)
                                            throw new AssertionError(
                                                    "concurrent capacity overflow");
                                    }
                                }));
            }
            for (var future : futures) future.get(60, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        expect(
                diagnostics.size() == 1024 && output.size() == 20,
                "400000 concurrent reports bounded");
        clock.set(TimeUnit.MINUTES.toNanos(1));
        diagnostics.flush(false);
        expect(
                output.size() == 21 && output.get(20).contains("399980"),
                "concurrent suppression count exact");
        for (int i = 0; i < 8; i++)
            expect(
                    output.get(20).contains("worker-" + i + ".toml"),
                    "concurrent file included " + i);
    }

    public static void main(String[] args) throws Exception {
        capacityAndRecovery();
        slidingWindow();
        fileSummary();
        concurrentFlood();
        System.out.println("CONFIG_DIAGNOSTICS_PASS checks=" + checks);
    }
}

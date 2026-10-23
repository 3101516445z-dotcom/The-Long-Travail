package com.thelongtravail.config;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class ConfigWatcher {
    private static final Map<Path, Runnable> callbacks = new HashMap<>();
    private static final Map<Path, WatchKey> directories = new HashMap<>();
    private static WatchService service;

    static synchronized void watch(Path file, Runnable callback) throws IOException {
        Path path = file.toAbsolutePath().normalize();
        if (service == null) {
            service = FileSystems.getDefault().newWatchService();
            Thread worker = new Thread(ConfigWatcher::run, "TLT configuration watcher");
            worker.setDaemon(true);
            worker.start();
        }
        Path directory = path.getParent();
        if (!directories.containsKey(directory)) {
            directories.put(
                    directory,
                    directory.register(
                            service,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_DELETE));
        }
        callbacks.put(path, callback);
    }

    static synchronized void remove(Path file) {
        Path path = file.toAbsolutePath().normalize();
        callbacks.remove(path);
        Path directory = path.getParent();
        if (callbacks.keySet().stream().noneMatch(p -> p.getParent().equals(directory))) {
            WatchKey key = directories.remove(directory);
            if (key != null) key.cancel();
        }
    }

    private static void run() {
        Map<Path, Long> pending = new HashMap<>();
        try {
            while (true) {
                WatchKey key = service.poll(250, TimeUnit.MILLISECONDS);
                if (key != null) {
                    Path directory = (Path) key.watchable();
                    long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(300);
                    synchronized (ConfigWatcher.class) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                                callbacks.keySet().stream()
                                        .filter(p -> p.getParent().equals(directory))
                                        .forEach(p -> pending.put(p, deadline));
                            } else if (event.context() instanceof Path relative) {
                                Path file = directory.resolve(relative);
                                if (callbacks.containsKey(file)) pending.put(file, deadline);
                            }
                        }
                        if (!key.reset()) directories.remove(directory);
                    }
                }
                ConfigProblems.flush();
                var iterator = pending.entrySet().iterator();
                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    if (System.nanoTime() < entry.getValue()) continue;
                    iterator.remove();
                    Runnable callback;
                    synchronized (ConfigWatcher.class) {
                        callback = callbacks.get(entry.getKey());
                    }
                    if (callback != null) {
                        try {
                            callback.run();
                        } catch (RuntimeException failure) {
                            ConfigProblems.report(
                                    entry.getKey(),
                                    "配置重载异常 / Configuration reload failed: " + failure);
                        }
                    }
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private ConfigWatcher() {}
}

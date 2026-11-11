package com.thelongtravail.compat.fluid;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

// 只接收已确认由本次复制创建的目录；不跟随符号链接，不自动重试。
final class UnifiedCopyCleanup {
    static void clean(UUID id, Path directory, Runnable removeProxy, Runnable removeEntry)
            throws IOException {
        IOException failures =
                new IOException(
                        "Incomplete copied unified disk cleanup: "
                                + id
                                + "; directory="
                                + directory);
        attempt("RS proxy " + id, removeProxy, failures);
        attempt("manager entry " + id, removeEntry, failures);
        try {
            Files.walkFileTree(
                    directory,
                    new SimpleFileVisitor<>() {
                        private void delete(Path path) {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException | RuntimeException failure) {
                                record("delete " + path, failure, failures);
                            }
                        }

                        @Override
                        public FileVisitResult visitFile(
                                Path file, BasicFileAttributes attributes) {
                            delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException failure) {
                            if (!(failure instanceof NoSuchFileException))
                                record("visit " + file, failure, failures);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException failure) {
                            if (failure != null) record("list " + dir, failure, failures);
                            delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException | RuntimeException failure) {
            record("walk " + directory, failure, failures);
        }
        if (failures.getSuppressed().length > 0) throw failures;
    }

    private static void attempt(String stage, Runnable action, IOException failures) {
        try {
            action.run();
        } catch (RuntimeException failure) {
            record(stage, failure, failures);
        }
    }

    private static void record(String stage, Exception failure, IOException failures) {
        failures.addSuppressed(new IOException(stage, failure));
    }

    private UnifiedCopyCleanup() {}
}

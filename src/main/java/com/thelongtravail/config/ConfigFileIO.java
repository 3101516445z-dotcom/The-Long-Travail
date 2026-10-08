package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class ConfigFileIO {
    public static CommentedConfig read(Path path) {
        CommentedConfig result = CommentedConfig.inMemory();
        if (Files.exists(path)) {
            try (CommentedFileConfig file = CommentedFileConfig.builder(path).sync().build()) {
                file.load();
                result.putAll(file);
                result.putAllComments(file);
            }
        }
        return result;
    }

    public static void write(Path path, CommentedConfig config, String header) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
        try {
            TomlWriter writer = new TomlWriter();
            writer.setIndentArrayElementsPredicate(list -> !list.isEmpty());
            try (var output = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                output.write("# " + header.replace("\n", "\n# ") + "\n\n");
                writer.write(config, output);
            }
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
    }

    private ConfigFileIO() {}
}

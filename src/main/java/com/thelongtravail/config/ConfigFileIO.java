package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class ConfigFileIO {
    public static CommentedConfig read(Path path) {
        CommentedConfig result =
                CommentedConfig.of(
                        java.util.LinkedHashMap::new,
                        com.electronwill.nightconfig.toml.TomlFormat.instance());
        if (Files.exists(path)) {
            try (CommentedFileConfig file =
                    CommentedFileConfig.builder(path).preserveInsertionOrder().sync().build()) {
                file.load();
                result.putAll(file);
                result.putAllComments(file);
            }
        }
        return result;
    }

    public static void write(Path path, CommentedConfig config) throws IOException {
        TomlWriter writer = new TomlWriter();
        writer.setIndentArrayElementsPredicate(list -> !list.isEmpty());
        writer.setIndent("    ");
        var output = new java.io.StringWriter();
        writer.write(config, output);
        String text = output.toString();
        // 避免重复写入触发文件监听，也让重复启动保持文件不变。
        if (Files.exists(path) && Files.readString(path, StandardCharsets.UTF_8).equals(text))
            return;
        Files.createDirectories(path.getParent());
        Path temporary =
                Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temporary, text, StandardCharsets.UTF_8);
            try {
                Files.move(
                        temporary,
                        path,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private ConfigFileIO() {}
}

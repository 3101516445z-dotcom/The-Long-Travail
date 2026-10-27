package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class ConfigFileIO {
    public static CommentedConfig read(Path path) {
        try {
            return parse(Files.exists(path) ? Files.readAllBytes(path) : new byte[0]);
        } catch (IOException failure) {
            throw new java.io.UncheckedIOException("Cannot read " + path, failure);
        }
    }

    static CommentedConfig parse(byte[] bytes) {
        try {
            String text =
                    StandardCharsets.UTF_8
                            .newDecoder()
                            .decode(java.nio.ByteBuffer.wrap(bytes))
                            .toString();
            if (text.startsWith("\uFEFF")) text = text.substring(1);
            CommentedConfig config =
                    CommentedConfig.of(
                            java.util.LinkedHashMap::new,
                            com.electronwill.nightconfig.toml.TomlFormat.instance());
            new TomlParser()
                    .parse(
                            new java.io.StringReader(text),
                            config,
                            com.electronwill.nightconfig.core.io.ParsingMode.REPLACE);
            return config;
        } catch (java.nio.charset.CharacterCodingException failure) {
            throw new com.electronwill.nightconfig.core.io.ParsingException(
                    "Configuration must use UTF-8 encoding", failure);
        }
    }

    public static void write(Path path, CommentedConfig config) throws IOException {
        String text = ConfigTomlWriter.write(path, config);
        if (Files.exists(path)
                && java.util.Arrays.equals(
                        Files.readAllBytes(path), text.getBytes(StandardCharsets.UTF_8))) return;
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

    // 核对磁盘版本后写入，并返回写入后读取的磁盘字节。
    // 活动配置的发布及失败处理由调用方负责。
    static byte[] writeChecked(Path path, CommentedConfig config, byte[] expected)
            throws IOException {
        ConfigRecovery.checkUnchanged(path, expected);
        write(path, config);
        return Files.readAllBytes(path);
    }

    private ConfigFileIO() {}
}

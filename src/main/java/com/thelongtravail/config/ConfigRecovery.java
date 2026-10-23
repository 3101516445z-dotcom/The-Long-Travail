package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.ParsingException;

import net.minecraftforge.common.ForgeConfigSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class ConfigRecovery {
    static CommentedConfig prepare(Path root, Path path, ForgeConfigSpec spec) throws IOException {
        byte[] original = Files.exists(path) ? Files.readAllBytes(path) : null;
        List<String> issues = new ArrayList<>();
        CommentedConfig config;
        try {
            config = ConfigFileIO.parse(original == null ? new byte[0] : original);
        } catch (ParsingException failure) {
            config = ConfigFileIO.parse(new byte[0]);
            issues.add(
                    "文件无法解析，恢复本文件默认值 / File could not be parsed; restored defaults: "
                            + failure.getMessage());
        }
        boolean upgraded =
                issues.isEmpty()
                        && path.getFileName().toString().equals("general.toml")
                        && ConfigUpgrade.boundless(path, config);
        issues.addAll(ConfigValidation.validate(config, spec, null).issues());
        if (original != null && !issues.isEmpty()) archive(root, path, original, issues);
        else if (original != null && upgraded) {
            Path backup = path.resolveSibling("general.toml.pre-boundless-relics.bak");
            if (!Files.exists(backup))
                Files.write(backup, original, java.nio.file.StandardOpenOption.CREATE_NEW);
        }
        checkUnchanged(path, original);
        ConfigFileIO.write(path, config);
        for (String issue : issues) ConfigProblems.report(path, issue);
        return config;
    }

    static Path archive(Path root, Path path, byte[] original, List<String> issues)
            throws IOException {
        Path base = root.toAbsolutePath().normalize();
        Path source = path.toAbsolutePath().normalize();
        if (!source.startsWith(base)) throw new IOException("Config outside directory: " + path);
        Path archiveRoot = base.resolve("invalid");
        Files.createDirectories(archiveRoot);
        Path directory =
                Files.createTempDirectory(
                        archiveRoot,
                        LocalDateTime.now()
                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
                                + "_");
        Path copy = directory.resolve(base.relativize(source));
        Files.createDirectories(copy.getParent());
        Files.write(copy, original, java.nio.file.StandardOpenOption.CREATE_NEW);
        Files.writeString(
                directory.resolve("errors.txt"),
                "File: " + base.relativize(source) + "\n" + String.join("\n", issues) + "\n",
                java.nio.charset.StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE_NEW);
        if (!Arrays.equals(original, Files.readAllBytes(copy)))
            throw new IOException("Config backup verification failed: " + copy);
        return copy;
    }

    static void checkUnchanged(Path path, byte[] expected) throws IOException {
        byte[] actual = Files.exists(path) ? Files.readAllBytes(path) : null;
        if (!Arrays.equals(expected, actual))
            throw new IOException("Config changed while reading: " + path);
    }

    private ConfigRecovery() {}
}

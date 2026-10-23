package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.ParsingException;

import net.minecraftforge.common.ForgeConfigSpec;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ConfigRecoveryTest {
    private static int checks;

    private static void expect(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }

    private static CommentedConfig parse(String text) {
        return ConfigFileIO.parse(text.getBytes(StandardCharsets.UTF_8));
    }

    private static ForgeConfigSpec spec() {
        var builder = new ForgeConfigSpec.Builder();
        builder.defineInRange("chance", .07, 0, 1);
        builder.defineInRange("count", 4, 1, 20);
        builder.define("group.enabled", true);
        return builder.build();
    }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory(Path.of(args[0]), "config-recovery-");
        ForgeConfigSpec spec = spec();
        for (String text :
                List.of(
                        "chance = \n",
                        "items = [\"a\", \"b\"\n",
                        "items = [\"a]\n",
                        "chance = hello\n",
                        "chance = 0.07\nchance = 0.1\n")) {
            try {
                parse(text);
                throw new AssertionError("Malformed input accepted");
            } catch (ParsingException expected) {
                checks++;
            }
        }
        expect((double) parse("\ufeffchance = 0.07\n").get("chance") == .07, "UTF8 BOM");
        for (byte[] bytes :
                List.of(
                        "# 概率\nchance = 0.07\n".getBytes(java.nio.charset.Charset.forName("GBK")),
                        "\ufeffchance = 0.07\n".getBytes(StandardCharsets.UTF_16LE))) {
            try {
                ConfigFileIO.parse(bytes);
                throw new AssertionError("Invalid encoding accepted");
            } catch (ParsingException expected) {
                checks++;
            }
        }
        Path file = root.resolve("aspects/test.toml");
        Files.createDirectories(file.getParent());
        byte[] broken = "chance = \n".getBytes(StandardCharsets.UTF_8);
        Files.write(file, broken);
        var recovered = ConfigRecovery.prepare(root, file, spec);
        expect((double) recovered.get("chance") == .07, "default recovery");
        try (var files = Files.walk(root.resolve("invalid"))) {
            var backups = files.filter(p -> p.endsWith("test.toml")).toList();
            expect(backups.size() == 1, "one backup per file");
            expect(
                    Arrays.equals(Files.readAllBytes(backups.get(0)), broken),
                    "backup byte identity");
        }
        long before;
        try (var dirs = Files.list(root.resolve("invalid"))) {
            before = dirs.count();
        }
        ConfigRecovery.prepare(root, file, spec);
        try (var dirs = Files.list(root.resolve("invalid"))) {
            expect(before == dirs.count(), "valid launch no backup");
        }
        var old = parse("chance = 0.2\ncount = 9\n[group]\nenabled = false\n");
        var candidate = parse("chance = \"bad\"\ncount = 8\n[group]\nenabled = false\n");
        var result = ConfigValidation.validate(candidate, spec, old);
        expect(result.rejected(), "invalid value rejected");
        expect((double) candidate.get("chance") == .2, "runtime previous value");
        expect((int) candidate.get("count") == 8, "valid neighbor applied");
        candidate = parse("chance = \"0.08\"\ncount = \"12\"\n[group]\nenabled = false\n");
        result = ConfigValidation.validate(candidate, spec, old);
        expect(!result.rejected(), "numeric strings accepted");
        expect(
                (double) candidate.get("chance") == .08 && (int) candidate.get("count") == 12,
                "numeric types");
        for (String invalid : List.of("2", "-1", "nan", "inf", "\"hello\"")) {
            candidate = parse("chance = " + invalid + "\n");
            result = ConfigValidation.validate(candidate, spec, null);
            expect(
                    result.rejected() && (double) candidate.get("chance") == .07,
                    "no clamping " + invalid);
        }
        candidate = parse("count = \"1.5\"\n");
        expect(
                ConfigValidation.validate(candidate, spec, old).rejected()
                        && (int) candidate.get("count") == 9,
                "fractional integer rejected");
        candidate = parse("group = \"bad\"\n");
        ConfigValidation.validate(candidate, spec, old);
        expect(
                Boolean.FALSE.equals(candidate.get("group.enabled")),
                "invalid section preserves previous");
        Path blocked = root.resolve("blocked");
        Files.createDirectories(blocked);
        Files.writeString(blocked.resolve("invalid"), "not a directory");
        Path bad = blocked.resolve("test.toml");
        Files.write(bad, broken);
        try {
            ConfigRecovery.prepare(blocked, bad, spec);
            throw new AssertionError("Backup failure ignored");
        } catch (java.io.IOException expected) {
            checks++;
        }
        expect(Arrays.equals(broken, Files.readAllBytes(bad)), "backup failure leaves original");
        try {
            ConfigRecovery.checkUnchanged(file, broken);
            throw new AssertionError("Changed file accepted");
        } catch (java.io.IOException expected) {
            checks++;
        }
        Files.write(file, "chance = \"0.09\"\ncount = \"bad\"\n".getBytes(StandardCharsets.UTF_8));
        ConfigRecovery.prepare(root, file, spec);
        expect((double) ConfigFileIO.read(file).get("chance") == .09, "startup conversion saved");
        try (var dirs = Files.list(root.resolve("invalid"))) {
            expect(dirs.count() == before + 1, "multiple issues one archive");
        }
        System.out.println("CONFIG_RECOVERY_PASS checks=" + checks + " directory=" + root);
    }
}

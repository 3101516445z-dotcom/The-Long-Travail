package com.thelongtravail.config.storage;

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

    private static void presetMigration(Path root) throws Exception {
        var oldGroups =
                List.of(
                        "travail.requirements.compatInitialization",
                        "travail.witness.compatInitialization",
                        "items.golden_age.compatInitialization",
                        "items.perfect_fluid.compatInitialization");
        var newGroups = List.of("exploration", "witness", "golden_age", "perfect_fluid");
        var builder = new ForgeConfigSpec.Builder();
        var original = parse("");
        for (int i = 0; i < oldGroups.size(); i++) {
            String ordinary = oldGroups.get(i).replace("compatInitialization", "pool");
            builder.defineListAllowEmpty(ordinary, List.of(), value -> value instanceof String);
            original.set(ordinary, List.of("last", "first", "last"));
            builder.define("presets." + newGroups.get(i) + ".demo.done", false);
            builder.define("presets." + newGroups.get(i) + ".demo.pending", true);
            original.set(oldGroups.get(i) + ".demo.done", true);
            original.set(oldGroups.get(i) + ".demo.pending", false);
        }
        var definition = builder.build();
        Path file = root.resolve("preset-migration/aspects/demo.toml");
        ConfigFileIO.write(file, original);
        var migrated = ConfigRecovery.prepare(file.getParent(), file, definition);
        for (int i = 0; i < oldGroups.size(); i++) {
            String target = "presets." + newGroups.get(i);
            expect(
                    Boolean.TRUE.equals(migrated.get(target + ".demo.done")),
                    "Processed state survives: " + target);
            expect(
                    Boolean.FALSE.equals(migrated.get(target + ".demo.pending")),
                    "Pending state survives: " + target);
            expect(!migrated.contains(oldGroups.get(i)), "Old section removed: " + target);
            expect(
                    migrated.get(oldGroups.get(i).replace("compatInitialization", "pool"))
                            .equals(List.of("last", "first", "last")),
                    "Ordinary list order and duplicates unchanged");
        }
        expect(
                !Files.exists(file.getParent().resolve("invalid")),
                "Valid migration needs no recovery archive");
        byte[] once = Files.readAllBytes(file);
        ConfigRecovery.prepare(file.getParent(), file, definition);
        expect(
                Arrays.equals(once, Files.readAllBytes(file)),
                "Migration is stable on subsequent starts");

        for (int i = 0; i < oldGroups.size(); i++) {
            String target = "presets." + newGroups.get(i);
            original.set(target + ".demo.done", false);
            original.set(target + ".demo.pending", true);
        }
        var result = ConfigValidation.validate(original, definition, migrated);
        expect(
                !result.rejected() && result.issues().isEmpty(),
                "Mixed old/new paths accepted on reload");
        for (String group : newGroups) {
            expect(
                    Boolean.FALSE.equals(original.get("presets." + group + ".demo.done")),
                    "New false wins: " + group);
            expect(
                    Boolean.TRUE.equals(original.get("presets." + group + ".demo.pending")),
                    "New true wins: " + group);
        }

        var partial = parse("");
        partial.set(oldGroups.get(0) + ".demo.done", true);
        partial.set(oldGroups.get(0) + ".demo.pending", false);
        partial.setComment(oldGroups.get(0) + ".demo.done", "player marker note");
        partial.set("presets.exploration.demo.pending", true);
        ConfigPresetMigration.migrate(partial, definition);
        expect(
                Boolean.TRUE.equals(partial.get("presets.exploration.demo.done")),
                "Partial new group inherits missing old markers");
        expect(
                Boolean.TRUE.equals(partial.get("presets.exploration.demo.pending")),
                "Partial new group keeps explicit markers");
        expect(
                "player marker note".equals(partial.getComment("presets.exploration.demo.done")),
                "Migration retains marker comments");
        partial.set("presets.exploration.demo.done", "invalid");
        expect(
                ConfigValidation.validate(partial, definition, migrated).rejected(),
                "Invalid migrated record still validated");
        expect(
                Boolean.TRUE.equals(partial.get("presets.exploration.demo.done")),
                "Invalid reload record uses previous valid state");
    }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory(Path.of(args[0]), "config-recovery-");
        presetMigration(root);
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
        var loot =
                parse(
                        """
                [loot.tables."minecraft:chests/desert_pyramid"]
                chance = 0.15
                items = ["the_long_travail:book_of_the_dead|1|1|1", "the_long_travail:icarus|1|1|1"]
                [loot.tables."minecraft:chests/jungle_temple"]
                chance = 0.42
                items = ["the_long_travail:affection|1|1|1"]
                """);
        expect(ConfigUpgrade.sixThemeItems(file, loot), "six item upgrade applied");
        com.electronwill.nightconfig.core.Config tables = loot.get("loot.tables");
        com.electronwill.nightconfig.core.Config desert =
                tables.get(List.of("minecraft:chests/desert_pyramid"));
        com.electronwill.nightconfig.core.Config jungle =
                tables.get(List.of("minecraft:chests/jungle_temple"));
        expect(((Number) desert.get("chance")).doubleValue() == .2, "default loot total upgraded");
        expect(
                desert.get("items")
                        .equals(
                                List.of(
                                        "the_long_travail:book_of_the_dead|3|1|1",
                                        "the_long_travail:icarus|3|1|1",
                                        "the_long_travail:holy_fool|2|1|1")),
                "old per-item probabilities preserved");
        expect(
                ((Number) jungle.get("chance")).doubleValue() == .42
                        && jungle.get("items").equals(List.of("the_long_travail:affection|1|1|1")),
                "custom loot unchanged");
        expect(!ConfigUpgrade.sixThemeItems(file, loot), "upgrade idempotent");
        System.out.println("CONFIG_RECOVERY_PASS checks=" + checks + " directory=" + root);
    }
}

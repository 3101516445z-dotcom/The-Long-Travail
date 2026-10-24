package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlWriter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class ConfigLayoutTest {
    private static int checks;

    private static void expect(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }

    private static CommentedConfig parse(String value) {
        return ConfigFileIO.parse(value.getBytes(StandardCharsets.UTF_8));
    }

    private static Object values(Object value) {
        if (value instanceof UnmodifiableConfig config) {
            Map<String, Object> result = new LinkedHashMap<>();
            config.valueMap().forEach((key, child) -> result.put(key, values(child)));
            return result;
        }
        if (value instanceof List<?> list)
            return list.stream().map(ConfigLayoutTest::values).toList();
        return value;
    }

    private static void roundTrip(Path file, CommentedConfig source) throws Exception {
        Object expected = values(source);
        ConfigFileIO.write(file, source);
        byte[] first = Files.readAllBytes(file);
        var modified = Files.getLastModifiedTime(file);
        for (int i = 0; i < 4; i++) {
            CommentedConfig read = ConfigFileIO.read(file);
            expect(expected.equals(values(read)), "Types, values and list order: " + file);
            ConfigFileIO.write(file, read);
            expect(
                    java.util.Arrays.equals(first, Files.readAllBytes(file)),
                    "Stable output: " + file);
            expect(modified.equals(Files.getLastModifiedTime(file)), "No rewrite: " + file);
        }
        expect(expected.equals(values(source)), "Source not mutated: " + file);
    }

    private static List<String> codes(String guide) {
        return Pattern.compile("(?m)^\\[([CEPMT]\\d{2})] ")
                .matcher(guide)
                .results()
                .map(match -> match.group(1))
                .toList();
    }

    public static void main(String[] args) throws Exception {
        Path directory = Path.of(args[0]);
        Files.createDirectories(directory);
        var specs = new LinkedHashMap<String, net.minecraftforge.common.ForgeConfigSpec>();
        if (args.length > 1 && args[1].equals("--forge")) specs.putAll(TravailConfig.SPECS);
        else {
            var builder = new net.minecraftforge.common.ForgeConfigSpec.Builder();
            builder.define("travail.witness.enabled", true);
            builder.define("travail.malice.enabled", false);
            builder.defineListAllowEmpty(
                    "travail.requirements.biomes",
                    List.of("minecraft:plains|@biome.minecraft.plains|1"),
                    x -> x instanceof String);
            builder.defineListAllowEmpty(
                    "travail.requirements.structures", List.of(), x -> x instanceof String);
            builder.comment("Terralith")
                    .define(
                            "travail.requirements.compatInitialization.terralithBiomesAdded",
                            false);
            builder.comment("Goety")
                    .define(
                            "travail.witness.compatInitialization.goety.positiveEffectsAdded",
                            true);
            builder.comment("L2 Complements")
                    .define(
                            "travail.witness.compatInitialization.l2complements.fishingItemsAdded",
                            false);
            builder.defineInRange("items.book.spawnProtectionTicks", 60, 0, 12000);
            ConfigFilesDescription.describe(builder, "aspects/underworld.toml");
            specs.put("aspects/underworld.toml", builder.build());
        }
        specs.put("client.toml", TravailClientConfig.SPEC);
        for (var entry : specs.entrySet()) {
            var config = parse("");
            entry.getValue().correct(config);
            config = parse(new TomlWriter().writeToString(config));
            roundTrip(directory.resolve(entry.getKey()), config);
            String text = Files.readString(directory.resolve(entry.getKey()));
            expect(
                    !text.contains("旧版携带") && !text.contains("Legacy Eki"),
                    "Current carrying wording");
            if (entry.getKey().startsWith("aspects/")) {
                expect(
                        text.indexOf("[travail.requirements]") < text.indexOf("[travail.malice]"),
                        "Exploration first");
                expect(
                        text.lastIndexOf("compatInitialization]") > text.lastIndexOf("[items."),
                        "Records at end");
                expect(
                        text.split("Preset processing records \\(advanced\\)", -1).length == 2,
                        "One footer");
            }
        }
        var custom =
                parse(
                        """
                        root = 2
                        [travail.witness]
                        list = ["last", "first", "last"]
                        [travail.requirements]
                        biomes = []
                        [travail.witness.compatInitialization.demo]
                        #custom marker
                        positiveEffectsAdded = true
                        fishingItemsAdded = false
                        [unknown."a.b"]
                        #user note
                        number = 0.07
                        date = 2026-10-24
                        nested = [[1, 2], [3]]
                        [[unknown.entries]]
                        #array element
                        id = "first"
                        [unknown.entries.child]
                        value = 9
                        [[unknown.entries]]
                        id = "second"
                        """);
        custom.set(
                List.of("unknown", "a.b", "text"),
                "first\n[value]\r\n\"quotes\"\\slashes\tspaces\nlast");
        custom.set(List.of("unknown", "a.b", "texts"), List.of("one\ntwo", "\"quoted\"", ""));
        custom.set(List.of("unknown", "a.b", "quoted\"key"), "中文");
        roundTrip(directory.resolve("custom.toml"), custom);
        var read = ConfigFileIO.read(directory.resolve("custom.toml"));
        expect(
                "user note".equals(read.getComment(List.of("unknown", "a.b", "number"))),
                "Unknown comments retained");
        String customText = Files.readString(directory.resolve("custom.toml"));
        expect(
                customText.indexOf("demo.fishingItemsAdded")
                        < customText.indexOf("demo.positiveEffectsAdded"),
                "Rewards before effects");
        custom.setComment("travail.witness.compatInitialization.demo", "custom table comment");
        roundTrip(directory.resolve("table-comments.toml"), custom);
        read = ConfigFileIO.read(directory.resolve("table-comments.toml"));
        expect(
                "custom table comment"
                        .equals(read.getComment("travail.witness.compatInitialization.demo")),
                "Nested table comments retained");
        custom.set("travail.witness.compatInitialization.empty", parse(""));
        roundTrip(directory.resolve("empty-tables.toml"), custom);
        custom.set(
                "travail.witness.compatInitialization.extra", List.of(parse("name = 'entry'\n")));
        roundTrip(directory.resolve("preset-table-array.toml"), custom);
        var zh = codes(ConfigFilesDescription.GUIDE);
        expect(zh.size() == 24, "24 guide sections");
        expect(zh.equals(codes(ConfigFilesDescription.GUIDE_EN)), "Matching bilingual sections");
        expect(
                zh.indexOf("C04") < zh.indexOf("P00") && zh.indexOf("P00") < zh.indexOf("M02"),
                "Syntax, presets, mechanisms");
        expect(
                !ConfigFilesDescription.GUIDE.contains("排版审阅稿")
                        && !ConfigFilesDescription.GUIDE_EN.contains("Layout review draft"),
                "Production guide text");
        ConfigFiles.writeGuides(directory);
        System.out.println("ConfigLayoutTest PASS checks=" + checks + "; output=" + directory);
    }
}

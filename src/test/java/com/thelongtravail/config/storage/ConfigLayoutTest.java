package com.thelongtravail.config.storage;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.thelongtravail.config.ItemRegistrationConfig;
import com.thelongtravail.config.TravailClientConfig;
import com.thelongtravail.config.TravailConfig;

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

    private static void checkGuideIndex(String guide) {
        var indexed =
                Pattern.compile("(?m)^  ([CEPMT]\\d{2})  ")
                        .matcher(guide)
                        .results()
                        .map(match -> match.group(1))
                        .toList();
        expect(
                indexed.equals(codes(guide)),
                "Every guide section appears once in the index, in reading order");
    }

    private static void checkLegacyDescriptions(Path directory) throws Exception {
        var config =
                parse(
                        """
                #player note
                #── 联动预设处理记录（高级） / Preset processing records (advanced) ──
                #预设是内置的联动配置条目，满足条件时只补入池中缺少的内容；普通玩家通常无需修改这里。
                #Presets are built-in compatibility entries that add missing pool content when eligible. Most players can leave these records alone.
                #true：停止补入（已处理或手动跳过）；false：等待处理。不是功能启用开关。
                #true: stop additions (processed or manually skipped); false: pending. These are not feature switches.
                #上一级“配置说明.txt”：预设介绍 [P00]；重试与跳过 [P01]；配置键读法 [C02]。
                #See ../Configuration Guide.txt: preset basics [P00], retry/skip [P01], dotted keys [C02].
                [presets.witness]
                #── 增益效果预设 / Beneficial effect presets ──
                #详细规则：上一级“配置说明.txt”[P06]。
                #Details: ../Configuration Guide.txt [P06].
                #custom marker
                demo.positiveEffectsAdded = true
                """);
        Path file = directory.resolve("legacy-descriptions.toml");
        roundTrip(file, config);
        String text = Files.readString(file);
        expect(!text.contains("built-in compatibility entries"), "Old preset introduction removed");
        expect(
                !text.contains("Details: ../Configuration Guide.txt [P06]."),
                "Old group explanation removed");
        expect(
                text.contains("#player note") && text.contains("#custom marker"),
                "Custom notes survive generated-comment cleanup");
        expect(text.split("Preset injection", -1).length == 2, "Old banner replaced once");
    }

    private static void checkRegistration(Path directory) throws Exception {
        Path file =
                Files.createTempDirectory(directory, "registration-")
                        .resolve("item-registration.toml");
        expect(
                ItemRegistrationConfig.read(file).isEmpty(),
                "Fresh registration enables every item");
        var config = ConfigFileIO.read(file);
        int items = 0;
        for (var group : ItemRegistrationConfig.GROUPS.entrySet()) {
            for (String id : group.getValue()) {
                String key = group.getKey() + "." + id;
                expect(Boolean.TRUE.equals(config.get(key)), "Registration default: " + id);
                expect(
                        Pattern.compile("[\\p{IsHan}]").matcher(config.getComment(key)).find(),
                        "Chinese item description: " + id);
                items++;
            }
        }
        expect(items == 36, "All six theme groups have six items");
        config.set("deep_valley.last_affairs", false);
        config.setComment("deep_valley.last_affairs", "old description");
        config.set("deep_valley.extra", "custom");
        config.setComment("deep_valley.extra", "player note");
        ConfigFileIO.write(file, config);
        expect(
                ItemRegistrationConfig.read(file).equals(java.util.Set.of("last_affairs")),
                "Comment refresh keeps disabled items disabled");
        var read = ConfigFileIO.read(file);
        expect(
                values(config).equals(values(read)),
                "Comment refresh preserves all registration values");
        expect(
                "player note".equals(read.getComment("deep_valley.extra")),
                "Unknown registration notes retained");
        expect(
                read.getComment("deep_valley.last_affairs").contains("身后事"),
                "Existing registration gets current item name");
        var modified = Files.getLastModifiedTime(file);
        ItemRegistrationConfig.read(file);
        expect(
                modified.equals(Files.getLastModifiedTime(file)),
                "Unchanged registration is not rewritten");
        read.set("deep_valley.last_affairs", "false");
        ConfigFileIO.write(file, read);
        byte[] invalid = Files.readAllBytes(file);
        boolean rejected = false;
        try {
            ItemRegistrationConfig.read(file);
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        expect(rejected, "Non-boolean registration still rejects startup");
        expect(
                java.util.Arrays.equals(invalid, Files.readAllBytes(file)),
                "Invalid registration is not overwritten");
    }

    private static void checkLocalOrder(Path directory) throws Exception {
        var config =
                parse(
                        """
                        [upgrades]
                        internal = true
                        [effects]
                        enabled = true
                        [items.phosphophyllite]
                        witnessDamage = 0.2
                        maliceDamage = 0.4
                        witnessAttributes = 0.05
                        maliceAttributes = 0.1
                        healExcludedDamageTypes = ["last", "first", "last"]
                        [items.azrael]
                        damageAttempts = 3
                        maliceChance = 0.025
                        [items.boundless.star]
                        witnessDuration = 10.0
                        maliceDuration = 10.0
                        witnessChance = 0.1
                        maliceChance = 0.1
                        """);
        Path file = directory.resolve("ordering/general.toml");
        roundTrip(file, config);
        var result = ConfigFileIO.read(file);
        String text = Files.readString(file);
        expect(
                text.indexOf("[effects]") < text.indexOf("[upgrades]"),
                "Public effects before internal records");
        UnmodifiableConfig item = result.get("items.phosphophyllite");
        expect(
                List.copyOf(item.valueMap().keySet())
                        .subList(0, 4)
                        .equals(
                                List.of(
                                        "maliceAttributes",
                                        "maliceDamage",
                                        "witnessAttributes",
                                        "witnessDamage")),
                "Keep each Phosphophyllite state together");
        expect(
                text.indexOf("maliceChance = 0.025") < text.indexOf("damageAttempts = 3"),
                "Azrael chance before advanced execution settings");
        item = result.get("items.boundless.star");
        expect(
                List.copyOf(item.valueMap().keySet())
                        .equals(
                                List.of(
                                        "maliceChance",
                                        "maliceDuration",
                                        "witnessChance",
                                        "witnessDuration")),
                "Keep each Star Voice state together");
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
            builder.comment("Terralith").define("presets.exploration.terralithBiomesAdded", false);
            builder.comment("Goety").define("presets.witness.goety.positiveEffectsAdded", true);
            builder.comment("L2 Complements")
                    .define("presets.witness.l2complements.fishingItemsAdded", false);
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
                expect(text.indexOf("[presets]") > text.lastIndexOf("[items."), "Records at end");
                expect(text.split("Preset injection", -1).length == 2, "One footer");
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
                        [presets.witness.demo]
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
        custom.setComment("presets.witness.demo", "custom table comment");
        roundTrip(directory.resolve("table-comments.toml"), custom);
        read = ConfigFileIO.read(directory.resolve("table-comments.toml"));
        expect(
                "custom table comment".equals(read.getComment("presets.witness.demo")),
                "Nested table comments retained");
        custom.set("presets.witness.empty", parse(""));
        roundTrip(directory.resolve("empty-tables.toml"), custom);
        custom.set("presets.witness.extra", List.of(parse("name = 'entry'\n")));
        roundTrip(directory.resolve("preset-table-array.toml"), custom);
        custom.setComment("presets", "custom parent note");
        roundTrip(directory.resolve("preset-parent-comment.toml"), custom);
        expect(
                ConfigFileIO.read(directory.resolve("preset-parent-comment.toml"))
                        .getComment("presets")
                        .contains("custom parent note"),
                "Parent note retained");
        var zh = codes(ConfigFilesDescription.GUIDE);
        expect(zh.size() == 28, "28 guide sections");
        expect(zh.contains("M06"), "Theme item configuration section");
        expect(zh.equals(codes(ConfigFilesDescription.GUIDE_EN)), "Matching bilingual sections");
        checkGuideIndex(ConfigFilesDescription.GUIDE);
        checkGuideIndex(ConfigFilesDescription.GUIDE_EN);
        checkLocalOrder(directory);
        checkRegistration(directory);
        checkLegacyDescriptions(directory);
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

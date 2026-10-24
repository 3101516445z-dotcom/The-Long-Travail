package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

final class ConfigTomlWriter {
    private static final Gson JSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String PRESETS =
            """
            ── 联动预设处理记录（高级） / Preset processing records (advanced) ──
            预设是内置的联动配置条目，满足条件时只补入池中缺少的内容；普通玩家通常无需修改这里。
            Presets are built-in compatibility entries that add missing pool content when eligible. Most players can leave these records alone.
            true：停止补入（已处理或手动跳过）；false：等待处理。不是功能启用开关。
            true: stop additions (processed or manually skipped); false: pending. These are not feature switches.
            上一级“配置说明.txt”：预设介绍 [P00]；重试与跳过 [P01]；配置键读法 [C02]。
            See ../Configuration Guide.txt: preset basics [P00], retry/skip [P01], dotted keys [C02].
            """;
    private static final List<String> GROUPS =
            List.of(
                    "群系预设 / Biome presets|P02",
                    "结构预设 / Structure presets|P03",
                    "奖励预设 / Reward presets|P04",
                    "生物预设 / Entity presets|P05",
                    "增益效果预设 / Beneficial effect presets|P06",
                    "食物黑名单预设 / Food blacklist presets|P07",
                    "附魔黑名单预设 / Enchantment blacklist presets|P07",
                    "完美流体黑名单预设 / Perfect Fluid blacklist presets|P08");

    private record Table(List<String> path, UnmodifiableConfig values, String comment) {}

    private record Field(List<String> path, Object value, String comment) {}

    private final StringBuilder output = new StringBuilder();
    private final List<Table> presets = new ArrayList<>();

    static String write(Path file, UnmodifiableCommentedConfig config) {
        ConfigTomlWriter writer = new ConfigTomlWriter();
        writer.table(new Table(List.of(), ConfigLayout.arrange(file, config), null), false, true);
        writer.presets.sort(Comparator.comparingInt(t -> firstGroup(t.values())));
        boolean first = true;
        for (Table table : writer.presets) {
            writer.comments((first ? PRESETS : "") + clean(table.comment()), "");
            writer.output.append('[').append(key(table.path())).append("]\n");
            if (hasTableComments(table.values())) {
                writer.fields(table.values(), table.path(), false);
            } else {
                List<Field> fields = new ArrayList<>();
                flatten(table.values(), List.of(), fields);
                fields.sort(Comparator.comparingInt(f -> group(f.path().get(f.path().size() - 1))));
                int previous = -1;
                for (Field field : fields) {
                    int category = group(field.path().get(field.path().size() - 1));
                    if (category != previous && category < GROUPS.size()) {
                        writer.output.append('\n');
                        writer.comments(groupComment(category), "    ");
                    }
                    writer.field(field, false);
                    previous = category;
                }
            }
            writer.output.append('\n');
            first = false;
        }
        return writer.output.toString();
    }

    private void table(Table table, boolean array, boolean defer) {
        if (defer
                && !table.path().isEmpty()
                && table.path().get(table.path().size() - 1).equals("compatInitialization")) {
            presets.add(table);
            return;
        }
        comments(clean(table.comment()), "");
        if (!table.path().isEmpty()) {
            output.append(array ? "[[" : "[")
                    .append(key(table.path()))
                    .append(array ? "]]\n" : "]\n");
        }
        fields(table.values(), table.path(), defer);
    }

    private void fields(UnmodifiableConfig config, List<String> path, boolean defer) {
        boolean first = true;
        for (var entry : config.valueMap().entrySet()) {
            if (entry.getValue() instanceof UnmodifiableConfig || tables(entry.getValue()))
                continue;
            if (!first) output.append('\n');
            field(
                    new Field(
                            List.of(entry.getKey()),
                            entry.getValue(),
                            comment(config, entry.getKey())),
                    path.isEmpty());
            first = false;
        }
        if (!path.isEmpty() || !first) output.append('\n');
        for (var entry : config.valueMap().entrySet()) {
            List<String> childPath = new ArrayList<>(path);
            childPath.add(entry.getKey());
            if (entry.getValue() instanceof UnmodifiableConfig child) {
                table(new Table(childPath, child, comment(config, entry.getKey())), false, defer);
            } else if (tables(entry.getValue())) {
                boolean firstTable = true;
                for (Object child : (List<?>) entry.getValue()) {
                    table(
                            new Table(
                                    childPath,
                                    (UnmodifiableConfig) child,
                                    firstTable ? comment(config, entry.getKey()) : null),
                            true,
                            false);
                    firstTable = false;
                }
            }
        }
    }

    private void field(Field field, boolean root) {
        String indent = root ? "" : "    ";
        comments(clean(field.comment()), indent);
        CommentedConfig value = CommentedConfig.of(LinkedHashMap::new, TomlFormat.instance());
        value.set(List.of("value"), field.value());
        CommentedConfig container = CommentedConfig.of(LinkedHashMap::new, TomlFormat.instance());
        if (!root) container.set("section", value);
        TomlWriter writer = new TomlWriter();
        writer.setIndent("    ");
        writer.setNewline("\n");
        writer.setIndentArrayElementsPredicate(list -> !list.isEmpty());
        writer.setWriteTableInlinePredicate(child -> child != value);
        StringWriter text = new StringWriter();
        writer.write(root ? value : container, text);
        String serialized = text.toString();
        int start = serialized.indexOf("value = ");
        if (start < 0) throw new IllegalStateException("Missing serialized configuration value");
        output.append(indent)
                .append(key(field.path()))
                .append(serialized.substring(start + 5).stripTrailing())
                .append('\n');
    }

    private void comments(String comment, String indent) {
        if (comment == null || comment.isEmpty()) return;
        for (String line : comment.split("\n"))
            output.append(indent).append('#').append(line).append('\n');
    }

    private static String key(List<String> path) {
        return String.join(
                ".",
                path.stream().map(k -> k.matches("[A-Za-z0-9_-]+") ? k : JSON.toJson(k)).toList());
    }

    private static String comment(UnmodifiableConfig config, String key) {
        return config instanceof UnmodifiableCommentedConfig commented
                ? commented.getComment(List.of(key))
                : null;
    }

    private static boolean tables(Object value) {
        return value instanceof List<?> list
                && !list.isEmpty()
                && list.stream().allMatch(UnmodifiableConfig.class::isInstance);
    }

    private static void flatten(UnmodifiableConfig config, List<String> path, List<Field> fields) {
        for (var entry : config.valueMap().entrySet()) {
            List<String> child = new ArrayList<>(path);
            child.add(entry.getKey());
            if (entry.getValue() instanceof UnmodifiableConfig nested)
                flatten(nested, child, fields);
            else fields.add(new Field(child, entry.getValue(), comment(config, entry.getKey())));
        }
    }

    private static boolean hasTableComments(UnmodifiableConfig config) {
        for (var entry : config.valueMap().entrySet()) {
            if (tables(entry.getValue())) return true;
            if (entry.getValue() instanceof UnmodifiableConfig child
                    && (child.isEmpty()
                            || comment(config, entry.getKey()) != null
                            || hasTableComments(child))) return true;
        }
        return false;
    }

    private static int firstGroup(UnmodifiableConfig config) {
        List<Field> fields = new ArrayList<>();
        flatten(config, List.of(), fields);
        return fields.stream()
                .mapToInt(f -> group(f.path().get(f.path().size() - 1)))
                .min()
                .orElse(GROUPS.size());
    }

    private static int group(String key) {
        if (key.endsWith("BiomesAdded")) return 0;
        if (key.endsWith("StructuresAdded")) return 1;
        return switch (key) {
            case "fishingItemsAdded", "phantomDropsAdded" -> 2;
            case "fishingEntitiesAdded" -> 3;
            case "positiveEffectsAdded" -> 4;
            case "foodBlacklistAdded" -> 5;
            case "enchantmentBlacklistAdded" -> 6;
            case "maidSoulAdded", "monsterBattleDeviceAdded" -> 7;
            default -> GROUPS.size();
        };
    }

    private static String groupComment(int category) {
        String[] parts = GROUPS.get(category).split("\\|");
        return "── "
                + parts[0]
                + " ──\n详细规则：上一级“配置说明.txt”["
                + parts[1]
                + "]。\nDetails: ../Configuration Guide.txt ["
                + parts[1]
                + "].\n"
                + (category == 7
                        ? "完整重启游戏或服务器时处理；须安装对应模组，且该组预设物品全部已注册。\n"
                              + "Processed on full game/server startup, only with the mod installed"
                              + " and every item in that group registered.\n"
                        : "");
    }

    private static String clean(String comment) {
        if (comment == null) return "";
        if (comment.equals(PRESETS.stripTrailing())) return "";
        String result = comment.replace(PRESETS, "");
        for (int i = 0; i < GROUPS.size(); i++) {
            if (result.equals(groupComment(i).stripTrailing())) return "";
            result = result.replace(groupComment(i), "");
        }
        return result;
    }
}

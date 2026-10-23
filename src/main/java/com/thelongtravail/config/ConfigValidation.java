package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;

import net.minecraftforge.common.ForgeConfigSpec;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ConfigValidation {
    public record Result(List<String> issues, boolean rejected) {}

    public static Number number(Object value) {
        if (value instanceof Number number && Double.isFinite(number.doubleValue())) return number;
        if (value instanceof String text
                && text.trim()
                        .matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?")) {
            double number = Double.parseDouble(text.trim());
            if (Double.isFinite(number)) return number;
        }
        throw new IllegalArgumentException("必须为有限数值 / Expected a finite number");
    }

    static Result validate(CommentedConfig config, ForgeConfigSpec spec, CommentedConfig previous) {
        List<String> issues = new ArrayList<>();
        boolean rejected = values(config, spec.getSpec(), previous, List.of(), issues);
        Object raw = config.get("loot.tables");
        if (raw instanceof UnmodifiableConfig tables) {
            for (var entry : tables.entrySet()) {
                if (entry.getValue() instanceof CommentedConfig table
                        && table.get("chance") instanceof String text) {
                    try {
                        double number = number(text).doubleValue();
                        if (number >= 0 && number <= 1) {
                            table.set("chance", number);
                            issues.add(
                                    "loot.tables.\""
                                            + entry.getKey()
                                            + "\".chance: 数字字符串已转换 / Numeric string converted");
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
        spec.correct(
                config,
                (action, path, oldValue, newValue) -> {
                    if (oldValue != null)
                        issues.add(
                                String.join(".", path) + ": 未识别的配置已移除 / Unknown setting removed");
                });
        return new Result(List.copyOf(issues), rejected);
    }

    private static boolean values(
            CommentedConfig root,
            UnmodifiableConfig schema,
            CommentedConfig previous,
            List<String> parent,
            List<String> issues) {
        boolean rejected = false;
        for (var entry : schema.entrySet()) {
            List<String> path = new ArrayList<>(parent);
            path.add(entry.getKey());
            Object value = root.get(path);
            if (entry.getValue() instanceof UnmodifiableConfig child) {
                if (value != null && !(value instanceof CommentedConfig)) {
                    Object old = previous == null ? null : previous.get(path);
                    root.set(
                            path,
                            old instanceof CommentedConfig
                                    ? ConfigLayout.arrange(
                                            java.nio.file.Path.of("snapshot.toml"),
                                            (CommentedConfig) old)
                                    : CommentedConfig.inMemory());
                    issues.add(
                            String.join(".", path)
                                    + ": 配置分组无效，使用"
                                    + (old == null ? "默认值" : "上次有效值")
                                    + " / Invalid section, using fallback");
                    rejected = true;
                }
                rejected |= values(root, child, previous, path, issues);
            } else if (entry.getValue() instanceof ForgeConfigSpec.ValueSpec definition
                    && value != null) {
                Object normalized = value;
                if (definition.getDefault() instanceof Number && value instanceof String) {
                    try {
                        Number n = number(value);
                        Object sample = definition.getDefault();
                        if (sample instanceof Integer)
                            normalized = new BigDecimal(value.toString().trim()).intValueExact();
                        else if (sample instanceof Long)
                            normalized = new BigDecimal(value.toString().trim()).longValueExact();
                        else normalized = n.doubleValue();
                    } catch (IllegalArgumentException | ArithmeticException ignored) {
                    }
                }
                if (definition.test(normalized)) {
                    if (normalized != value) {
                        root.set(path, normalized);
                        issues.add(
                                String.join(".", path) + ": 数字字符串已转换 / Numeric string converted");
                    }
                } else {
                    Object old = previous == null ? null : previous.get(path);
                    root.set(
                            path,
                            old != null && definition.test(old) ? old : definition.getDefault());
                    issues.add(
                            String.join(".", path)
                                    + ": 值无效，使用"
                                    + (old == null ? "默认值" : "上次有效值")
                                    + " / Invalid value, using fallback");
                    rejected = true;
                }
            }
        }
        return rejected;
    }

    private ConfigValidation() {}
}

package com.thelongtravail.configsmoke;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.thelongtravail.config.TravailConfig;

import net.minecraftforge.common.ForgeConfigSpec;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** 保存实际 Forge 定义，供结构调整前后逐字比较。 */
final class ConfigDefinitionSnapshot {
    static void write() throws Exception {
        List<String> lines = new ArrayList<>();
        for (var entry : TravailConfig.SPECS.entrySet()) {
            lines.add("FILE " + entry.getKey());
            walk(entry.getValue().getSpec(), "", lines);
        }
        var fields = TravailConfig.class.getFields();
        Arrays.sort(fields, java.util.Comparator.comparing(java.lang.reflect.Field::getName));
        for (var field : fields) {
            lines.add(
                    "FIELD "
                            + Modifier.toString(field.getModifiers())
                            + " "
                            + field.getGenericType().getTypeName()
                            + " "
                            + field.getName());
            Object value = field.get(null);
            if (value instanceof ForgeConfigSpec.ConfigValue<?> handle) handle(handle, lines);
            else if (value instanceof Map<?, ?> map) {
                for (var entry : map.entrySet()) {
                    if (entry.getValue() instanceof ForgeConfigSpec.ConfigValue<?> handle) {
                        lines.add("POOL " + entry.getKey());
                        handle(handle, lines);
                    }
                }
            }
        }
        Files.write(Path.of("config-definition-snapshot.txt"), lines);
    }

    private static void handle(ForgeConfigSpec.ConfigValue<?> handle, List<String> lines) {
        // 按实例身份查找所属 Spec，避免同名路径掩盖文件归属变化。
        List<String> owners = new ArrayList<>();
        TravailConfig.SPECS.forEach(
                (file, spec) -> {
                    if (spec.getValues().get(handle.getPath()) == handle) owners.add(file);
                });
        if (owners.size() != 1) throw new AssertionError("Config owner: " + handle.getPath());
        lines.add("HANDLE " + owners.get(0) + " " + handle.getPath());
    }

    private static void walk(UnmodifiableConfig config, String prefix, List<String> lines) {
        for (var entry : config.entrySet()) {
            String path = prefix + entry.getKey();
            if (entry.getValue() instanceof UnmodifiableConfig child)
                walk(child, path + ".", lines);
            else if (entry.getValue() instanceof ForgeConfigSpec.ValueSpec spec) {
                lines.add(
                        "KEY "
                                + path
                                + " DEFAULT "
                                + spec.getDefault()
                                + " RANGE "
                                + spec.getRange()
                                + " COMMENT "
                                + spec.getComment());
                for (Object probe :
                        List.of(
                                "",
                                "a",
                                "a|b",
                                "a|b|c",
                                "a|b|c|d",
                                -1,
                                0,
                                1,
                                Integer.MAX_VALUE,
                                -0.5,
                                0.5,
                                Double.NaN,
                                true,
                                List.of(),
                                List.of("a"),
                                List.of("a|b"),
                                List.of("a|b|c"),
                                List.of("a|b|c|d"))) {
                    try {
                        lines.add("PROBE " + probe + " " + spec.test(probe));
                    } catch (RuntimeException e) {
                        lines.add("PROBE " + probe + " " + e.getClass().getName());
                    }
                }
            }
        }
    }

    private ConfigDefinitionSnapshot() {}
}

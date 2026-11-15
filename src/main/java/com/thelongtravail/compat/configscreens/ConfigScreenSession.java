package com.thelongtravail.compat.configscreens;

import com.thelongtravail.config.storage.ManagedConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.IConfigEvent;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// 编辑页保存事务；文件提交成功后才发布运行值。
public final class ConfigScreenSession {
    private record Entry(
            List<String> path,
            Object data,
            Method getter,
            ForgeConfigSpec.ValueSpec definition,
            Object original) {}

    private record Edit(List<String> path, Object value) {}

    private final ManagedConfig config;
    private final Screen parent;
    private final List<Entry> entries = new ArrayList<>();
    private byte[] openedFile;
    private boolean ready;

    public ConfigScreenSession(ManagedConfig config, Screen parent, Map<?, ?> values) {
        this.config = config;
        this.parent = parent;
        try {
            synchronized (config) {
                openedFile = Files.readAllBytes(config.getFullPath());
                var spec = (ForgeConfigSpec) config.getSpec();
                for (var pair : values.entrySet()) {
                    if (!(pair.getKey() instanceof ForgeConfigSpec.ConfigValue<?> value)) continue;
                    Object data = pair.getValue();
                    Method getter = data.getClass().getMethod("getCurrentValue");
                    entries.add(
                            new Entry(
                                    List.copyOf(value.getPath()),
                                    data,
                                    getter,
                                    spec.getSpec().get(value.getPath()),
                                    copy(value.get())));
                }
                ready = true;
            }
        } catch (IOException | ReflectiveOperationException failure) {
            report(failure);
        }
    }

    public void save(Screen editor) {
        boolean saved = false;
        boolean changed = false;
        try {
            if (ready) {
                List<Edit> edits = new ArrayList<>();
                for (Entry entry : entries) {
                    Object value = copy(entry.getter().invoke(entry.data()));
                    if (!entry.definition().test(value)) {
                        failed(editor);
                        return;
                    }
                    if (!Objects.equals(value, entry.original()))
                        edits.add(new Edit(entry.path(), value));
                }
                changed = !edits.isEmpty();
                if (!changed) saved = true;
                else
                    synchronized (config) {
                        // 即使文件监听器已完成重载，也拒绝旧编辑页覆盖新文件。
                        if (Arrays.equals(openedFile, Files.readAllBytes(config.getFullPath()))) {
                            saved =
                                    config.update(
                                            candidate -> {
                                                for (Edit edit : edits)
                                                    candidate.set(edit.path(), edit.value());
                                                // 发布前统一注释，避免 acceptConfig 再次绕过事务写盘。
                                                return ((ForgeConfigSpec) config.getSpec())
                                                                .correct(candidate)
                                                        == 0;
                                            });
                        }
                    }
            }
        } catch (IOException | ReflectiveOperationException failure) {
            report(failure);
        }
        if (!saved) {
            failed(editor);
            return;
        }
        if (changed)
            ModList.get()
                    .getModContainerById(config.getModId())
                    .orElseThrow()
                    .dispatchConfigEvent(IConfigEvent.reloading(config));
        Minecraft.getInstance().setScreen(parent);
    }

    private void failed(Screen editor) {
        Minecraft.getInstance()
                .setScreen(
                        new AlertScreen(
                                () -> Minecraft.getInstance().setScreen(editor),
                                Component.translatable(
                                        "message.the_long_travail.configscreens.failed"),
                                Component.translatable(
                                        "message.the_long_travail.configscreens.reopen")));
    }

    private void report(Exception failure) {
        org.slf4j.LoggerFactory.getLogger("the_long_travail.compat")
                .warn("Config screen operation failed for {}", config.getFileName(), failure);
    }

    private static Object copy(Object value) {
        if (value instanceof List<?> list)
            return list.stream()
                    .map(ConfigScreenSession::copy)
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        return value;
    }
}

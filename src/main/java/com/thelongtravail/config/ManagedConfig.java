package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.ParsingException;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;
import net.minecraftforge.fml.config.IConfigEvent;
import net.minecraftforge.fml.config.ModConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Function;

final class ManagedConfig extends ModConfig {
    private final ForgeConfigSpec definition;
    private final ModContainer owner;
    private final Path root;
    private volatile CommentedFileConfig active;
    private byte[] disk;
    private boolean writable;
    private boolean watching;
    private final ConfigFileTypeHandler handler =
            new ConfigFileTypeHandler() {
                @Override
                public Function<ModConfig, CommentedFileConfig> reader(Path directory) {
                    return ignored -> open(directory.resolve(getFileName()));
                }

                @Override
                public void unload(Path directory, ModConfig config) {
                    synchronized (ManagedConfig.this) {
                        watching = false;
                        ConfigWatcher.remove(directory.resolve(getFileName()));
                    }
                }
            };

    ManagedConfig(Type type, ForgeConfigSpec spec, ModContainer owner, String file, Path root) {
        super(type, spec, owner, "the_long_travail/" + file);
        this.definition = spec;
        this.owner = owner;
        this.root = root;
    }

    @Override
    public ConfigFileTypeHandler getHandler() {
        return handler;
    }

    @Override
    public CommentedConfig getConfigData() {
        return active == null ? super.getConfigData() : active;
    }

    @Override
    public Path getFullPath() {
        return root.getParent().resolve(getFileName());
    }

    private synchronized CommentedFileConfig open(Path path) {
        try {
            var prepared = ConfigRecovery.prepare(root, path, definition);
            active = snapshot(path, prepared);
            disk = Files.readAllBytes(path);
            writable = true;
            return active;
        } catch (IOException failure) {
            throw new UncheckedIOException("Cannot prepare " + path, failure);
        }
    }

    void watch() {
        synchronized (this) {
            try {
                watching = true;
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                ConfigWatcher.watch(
                        getFullPath(),
                        () -> {
                            Thread thread = Thread.currentThread();
                            ClassLoader previous = thread.getContextClassLoader();
                            try {
                                thread.setContextClassLoader(loader);
                                reload();
                            } finally {
                                thread.setContextClassLoader(previous);
                            }
                        });
            } catch (IOException failure) {
                throw new UncheckedIOException("Cannot watch " + getFullPath(), failure);
            }
        }
    }

    synchronized void reload() {
        if (!watching) return;
        Path path = getFullPath();
        try {
            byte[] bytes = Files.readAllBytes(path);
            if (Arrays.equals(bytes, disk)) return;
            CommentedConfig candidate = ConfigFileIO.parse(bytes);
            var result = ConfigValidation.validate(candidate, definition, active);
            if (!result.rejected()) {
                if (!result.issues().isEmpty())
                    ConfigRecovery.archive(root, path, bytes, result.issues());
                disk = ConfigFileIO.writeChecked(path, candidate, bytes);
            } else {
                ConfigRecovery.checkUnchanged(path, bytes);
                disk = bytes;
            }
            publish(path, candidate);
            writable = !result.rejected();
            for (String issue : result.issues()) ConfigProblems.report(path, issue);
            owner.dispatchConfigEvent(IConfigEvent.reloading(this));
        } catch (ParsingException | IOException failure) {
            writable = false;
            ConfigProblems.report(
                    path,
                    "重载失败，保留本次运行的有效配置；原文件未改写 / Reload rejected; kept active configuration: "
                            + failure.getMessage());
        }
    }

    @Override
    public synchronized void save() {
        if (active == null || !writable) return;
        Path path = getFullPath();
        try {
            disk = ConfigFileIO.writeChecked(path, active, disk);
        } catch (IOException failure) {
            writable = false;
            ConfigProblems.report(
                    path, "配置保存未完成，磁盘文件已保留 / Configuration was not saved: " + failure.getMessage());
        }
    }

    synchronized boolean update(java.util.function.Predicate<CommentedConfig> edit) {
        if (active == null || !writable) return false;
        Path path = getFullPath();
        try {
            ConfigRecovery.checkUnchanged(path, disk);
            CommentedConfig candidate = ConfigFileIO.parse(disk);
            if (!edit.test(candidate)) return false;
            disk = ConfigFileIO.writeChecked(path, candidate, disk);
            publish(path, candidate);
            return true;
        } catch (IOException failure) {
            ConfigProblems.report(
                    path, "预设保存未完成，原配置已保留 / Preset was not saved: " + failure.getMessage());
            return false;
        }
    }

    // 调用方须持有本实例锁；加载和更新流程在此发布活动配置。
    private void publish(Path path, CommentedConfig candidate) {
        active = snapshot(path, candidate);
        definition.acceptConfig(active);
        definition.afterReload();
    }

    private static CommentedFileConfig snapshot(Path path, CommentedConfig config) {
        var file = CommentedFileConfig.builder(path).preserveInsertionOrder().sync().build();
        file.putAll(config);
        file.putAllComments(config);
        return file;
    }
}

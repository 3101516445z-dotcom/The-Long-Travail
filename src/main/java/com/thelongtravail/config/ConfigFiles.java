package com.thelongtravail.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

public final class ConfigFiles {
    private static final String DIRECTORY = "the_long_travail";

    public static void register(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener(ConfigFiles::afterConfigLoading);
        Path configDirectory = FMLPaths.CONFIGDIR.get();
        try {
            writeGuides(configDirectory.resolve(DIRECTORY));
            for (var entry : TravailConfig.SPECS.entrySet()) {
                prepare(configDirectory, entry.getKey(), entry.getValue());
                context.registerConfig(
                        ModConfig.Type.COMMON, entry.getValue(), DIRECTORY + "/" + entry.getKey());
            }
            prepare(configDirectory, "client.toml", TravailClientConfig.SPEC);
            context.registerConfig(
                    ModConfig.Type.CLIENT, TravailClientConfig.SPEC, DIRECTORY + "/client.toml");
        } catch (IOException | RuntimeException failure) {
            throw new IllegalStateException(
                    "苦旅配置读取失败。请检查 config/the_long_travail 和日志，修复后重启。", failure);
        }
    }

    private static void prepare(Path directory, String file, ForgeConfigSpec spec)
            throws IOException {
        Path path = directory.resolve(DIRECTORY).resolve(file);
        var config = ConfigFileIO.read(path);
        repairAzraelPaths(file, config);
        // 同时刷新说明；已有合法值由 Forge 原样保留。
        spec.correct(config);
        ConfigFileIO.write(path, config);
    }

    public static void repairAzraelPaths(
            String file, com.electronwill.nightconfig.core.CommentedConfig config) {
        if (!file.equals("aspects/deep_valley.toml")) return;
        // 只修复已发布版本多出的一层 items；新路径已有值时以新值为准。
        for (String key :
                java.util.List.of(
                        "selfExecutionAffectsCreativeAndSpectator",
                        "targetExecutionAffectsCreativeAndSpectator")) {
            String oldPath = "items.items.azrael." + key;
            String newPath = "items.azrael." + key;
            if (!config.contains(newPath) && config.contains(oldPath)) {
                config.set(newPath, config.get(oldPath));
            }
            config.remove(oldPath);
        }
    }

    private static void afterConfigLoading(
            net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        // Forge 在加载阶段最后一次保存会压平数组，故在加载完成后统一整理。
        event.enqueueWork(
                () -> {
                    try {
                        Path directory = FMLPaths.CONFIGDIR.get().resolve(DIRECTORY);
                        for (String file : TravailConfig.SPECS.keySet()) {
                            Path path = directory.resolve(file);
                            ConfigFileIO.write(path, ConfigFileIO.read(path));
                        }
                        Path client = directory.resolve("client.toml");
                        ConfigFileIO.write(client, ConfigFileIO.read(client));
                    } catch (IOException failure) {
                        throw new IllegalStateException("苦旅配置排版保存失败。", failure);
                    }
                });
    }

    public static void writeGuides(Path directory) throws IOException {
        Files.createDirectories(directory);
        writeGuide(directory.resolve("配置说明.txt"), ConfigFilesDescription.GUIDE);
        writeGuide(directory.resolve("Configuration Guide.txt"), ConfigFilesDescription.GUIDE_EN);
    }

    private static void writeGuide(Path path, String text) throws IOException {
        if (!Files.exists(path)
                || !Files.readString(path, java.nio.charset.StandardCharsets.UTF_8).equals(text))
            Files.writeString(path, text, java.nio.charset.StandardCharsets.UTF_8);
    }

    public static boolean isCommonSpec(Object spec) {
        return TravailConfig.SPECS.containsValue(spec);
    }

    private ConfigFiles() {}
}

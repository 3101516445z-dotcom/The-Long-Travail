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
        Path configDirectory = FMLPaths.CONFIGDIR.get();
        try {
            writeGuides(configDirectory.resolve(DIRECTORY));
            for (var entry : TravailConfig.SPECS.entrySet()) {
                prepare(configDirectory, entry.getKey(), entry.getValue());
                context.registerConfig(ModConfig.Type.COMMON, entry.getValue(), DIRECTORY + "/" + entry.getKey());
            }
            prepare(configDirectory, "client.toml", TravailClientConfig.SPEC);
            context.registerConfig(ModConfig.Type.CLIENT, TravailClientConfig.SPEC, DIRECTORY + "/client.toml");
        } catch (IOException | RuntimeException failure) {
            throw new IllegalStateException("苦旅配置读取失败。请检查 config/the_long_travail 和日志，修复后重启。", failure);
        }
    }

    private static void prepare(Path directory, String file, ForgeConfigSpec spec) throws IOException {
        Path path = directory.resolve(DIRECTORY).resolve(file);
        var config = ConfigFileIO.read(path);
        if (!Files.exists(path) || !spec.isCorrect(config)) {
            spec.correct(config);
            ConfigFileIO.write(path, config, ConfigFilesDescription.header(file));
        }
    }

    public static void writeGuides(Path directory) throws IOException {
        Files.createDirectories(directory);
        writeGuide(directory.resolve("配置说明.txt"), ConfigFilesDescription.GUIDE);
        writeGuide(directory.resolve("Configuration Guide.txt"), ConfigFilesDescription.GUIDE_EN);
    }

    private static void writeGuide(Path path, String text) throws IOException {
        if (!Files.exists(path) || !Files.readString(path, java.nio.charset.StandardCharsets.UTF_8).equals(text))
            Files.writeString(path, text, java.nio.charset.StandardCharsets.UTF_8);
    }

    public static boolean isCommonSpec(Object spec) { return TravailConfig.SPECS.containsValue(spec); }
    private ConfigFiles() {}
}

package com.thelongtravail.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigFiles {
    private static final String DIRECTORY = "the_long_travail";

    public static void register(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener(ConfigFiles::afterConfigLoading);
        context.getModEventBus().addListener(PerfectFluidCompatDefaults::loaded);
        context.getModEventBus()
                .addListener(
                        (net.minecraftforge.fml.event.config.ModConfigEvent.Loading event) -> {
                            if (event.getConfig() instanceof ManagedConfig config) config.watch();
                        });
        Path configDirectory = FMLPaths.CONFIGDIR.get();
        try {
            writeGuides(configDirectory.resolve(DIRECTORY));
            for (var entry : TravailConfig.SPECS.entrySet()) {
                register(
                        context,
                        ModConfig.Type.COMMON,
                        entry.getKey(),
                        entry.getValue(),
                        configDirectory);
            }
            register(
                    context,
                    ModConfig.Type.CLIENT,
                    "client.toml",
                    TravailClientConfig.SPEC,
                    configDirectory);
        } catch (IOException | RuntimeException failure) {
            throw new IllegalStateException(
                    "苦旅配置读取失败。请检查 config/the_long_travail 和日志，修复后重启。", failure);
        }
    }

    private static void register(
            FMLJavaModLoadingContext context,
            ModConfig.Type type,
            String file,
            ForgeConfigSpec spec,
            Path directory) {
        context.getContainer()
                .addConfig(
                        new ManagedConfig(
                                type,
                                spec,
                                context.getContainer(),
                                file,
                                directory.resolve(DIRECTORY)));
    }

    private static void afterConfigLoading(
            net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(PerfectFluidCompatDefaults::initialize);
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

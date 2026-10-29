package travail.smoke;

import com.thelongtravail.config.*;

import java.nio.file.*;
import java.util.*;
import net.minecraftforge.fml.config.ConfigTracker;

/** 只在隔离测试服务器中操作实际托管配置。 */
public final class TremorConfigChecks {
    private static int checks;
    private static final String KEY = "items.high_frequency_tremor.witnessBreakBlacklist";

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }

    public static int run() throws Exception {
        checks = 0;
        var managed =
                ConfigTracker.INSTANCE.fileMap().get("the_long_travail/aspects/deep_valley.toml");
        var reload = managed.getClass().getDeclaredMethod("reload");
        reload.setAccessible(true);
        var prepare =
                Class.forName("com.thelongtravail.config.ConfigRecovery")
                        .getDeclaredMethod(
                                "prepare",
                                Path.class,
                                Path.class,
                                net.minecraftforge.common.ForgeConfigSpec.class);
        prepare.setAccessible(true);
        var file = managed.getFullPath();
        byte[] original = Files.readAllBytes(file);
        var spec = TravailConfig.SPECS.get("aspects/deep_valley.toml");
        var valid =
                List.of(
                        "minecraft:stone",
                        "missing_tremor_probe:optional_block",
                        "minecraft:stone");
        try {
            var config = ConfigFileIO.read(file);
            config.set("items.underground_velvet.cooldownTicks", 17.0);
            config.set(KEY, valid);
            ConfigFileIO.write(file, config);
            reload.invoke(managed);
            check(
                    DeepValleyItemsConfig.TREMOR_BLACKLIST.get().equals(valid),
                    "unknown and duplicate complete IDs retained");
            DeepValleyItemsConfig.checkTremorBlacklist();
            DeepValleyItemsConfig.checkTremorBlacklist();
            for (Object bad :
                    List.of(
                            List.of("minecraft:stone", "bedrock"),
                            List.of(4),
                            "minecraft:bedrock",
                            List.of(":bedrock"),
                            List.of("minecraft:"),
                            List.of("Minecraft:bedrock"),
                            List.of("minecraft:bad block"))) {
                config.set(KEY, bad);
                ConfigFileIO.write(file, config);
                byte[] rejected = Files.readAllBytes(file);
                reload.invoke(managed);
                check(
                        DeepValleyItemsConfig.TREMOR_BLACKLIST.get().equals(valid),
                        "whole invalid list retains previous: " + bad);
                managed.save();
                check(
                        Arrays.equals(rejected, Files.readAllBytes(file)),
                        "invalid disk edit not overwritten");

                Path root = Files.createTempDirectory(Path.of("."), "tremor-config-recovery-");
                Path startup = root.resolve("deep_valley.toml");
                Files.write(startup, rejected);
                var restored =
                        (com.electronwill.nightconfig.core.CommentedConfig)
                                prepare.invoke(null, root, startup, spec);
                check(
                        restored.get(KEY).equals(List.of("minecraft:barrier")),
                        "startup invalid list uses default");
                check(
                        restored.get("items.underground_velvet.cooldownTicks")
                                .equals(config.get("items.underground_velvet.cooldownTicks")),
                        "valid neighbor preserved");
                try (var files = Files.walk(root.resolve("invalid"))) {
                    var backups =
                            files.filter(p -> p.getFileName().toString().equals("deep_valley.toml"))
                                    .toList();
                    check(
                            backups.size() == 1
                                    && Arrays.equals(rejected, Files.readAllBytes(backups.get(0))),
                            "startup archive preserves exact invalid bytes");
                }
            }
            byte[] broken = "items = [\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Files.write(file, broken);
            reload.invoke(managed);
            managed.save();
            check(
                    DeepValleyItemsConfig.TREMOR_BLACKLIST.get().equals(valid),
                    "TOML parse failure retains active list");
            check(Arrays.equals(broken, Files.readAllBytes(file)), "TOML failure retained on disk");
            config.set(KEY, List.of());
            ConfigFileIO.write(file, config);
            reload.invoke(managed);
            check(
                    DeepValleyItemsConfig.TREMOR_BLACKLIST.get().isEmpty(),
                    "empty blacklist accepted without bedrock insertion");
        } finally {
            Files.write(file, original);
            reload.invoke(managed);
        }
        return checks;
    }
}

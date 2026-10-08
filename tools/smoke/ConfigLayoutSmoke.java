package travail.smoke;

import com.thelongtravail.config.*;
import com.thelongtravail.network.TooltipConfigSync;
import java.util.*;

// 在 Forge 环境中运行，须先加载全部九份配置。
public final class ConfigLayoutSmoke {
    public static void run() throws Exception {
        if (TravailConfig.SPECS.size() != 8) throw new AssertionError("Expected eight common specs");
        var guideDirectory = java.nio.file.Files.createTempDirectory("travail-guide-test-");
        ConfigFiles.writeGuides(guideDirectory);
        ConfigFiles.writeGuides(guideDirectory);
        try (var files = java.nio.file.Files.list(guideDirectory)) {
            if (files.count() != 2) throw new AssertionError("Guide generation created extra files");
        }
        if (!java.nio.file.Files.readString(guideDirectory.resolve("配置说明.txt")).equals(ConfigFilesDescription.GUIDE)
                || !java.nio.file.Files.readString(guideDirectory.resolve("Configuration Guide.txt")).equals(ConfigFilesDescription.GUIDE_EN))
            throw new AssertionError("Guide content mismatch");
        int count = count(TravailClientConfig.SPEC.getSpec());
        for (var entry : TravailConfig.SPECS.entrySet()) {
            if (!entry.getValue().isLoaded()) throw new AssertionError("Config not loaded: " + entry.getKey());
            if (!entry.getKey().equals("general.toml") && entry.getValue().getSpec().contains("loot")) throw new AssertionError("Loot config retained: " + entry.getKey());
            count += count(entry.getValue().getSpec());
        }
        if (count != 240) throw new AssertionError("Expected 240 settings: " + count);
        if (!FarReachItemsConfig.IGNITE_SECONDS.getPath().equals(List.of("items", "icarus", "igniteSeconds"))) throw new AssertionError("Icarus ignition binding");
        if (!TravailClientConfig.DREAM_MESSAGES.getPath().equals(List.of("messages", "dreamSkillMessages"))) throw new AssertionError("Dream message client binding");
        if (!RainConfig.DAMAGE.getPath().equals(List.of("items", "eki", "damageBonus"))) throw new AssertionError("Abyss item binding");
        for (String methodName : List.of("serverValues", "serverPools")) {
            var method = TooltipConfigSync.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            if (((Map<?, ?>) method.invoke(null)).isEmpty()) throw new AssertionError("Tooltip snapshot lost config");
        }
        for (var spec : TravailConfig.SPECS.values()) if (!ConfigFiles.isCommonSpec(spec)) throw new AssertionError("Reload routing");
        if (ConfigFiles.isCommonSpec(TravailClientConfig.SPEC)) throw new AssertionError("Client config routed as common");
        System.out.println("CONFIG_LAYOUT_PASS: " + count + " settings, all common files loaded, rain bindings, tooltip snapshot and reload routing");
    }
    private static int count(com.electronwill.nightconfig.core.UnmodifiableConfig config) {
        int total = 0;
        for (var entry : config.entrySet()) {
            Object value = entry.getValue();
            total += value instanceof com.electronwill.nightconfig.core.UnmodifiableConfig nested ? count(nested) : 1;
        }
        return total;
    }
    private ConfigLayoutSmoke() {}
}

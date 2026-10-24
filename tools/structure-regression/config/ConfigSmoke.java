package com.thelongtravail.configsmoke;

import com.thelongtravail.config.ConfigFileIO;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.config.ConfigProblems;

import com.thelongtravail.loot.UnifiedLoot;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

@Mod("travail_smoke")
@Mod.EventBusSubscriber(modid="travail_smoke")
public class ConfigSmoke {
    private static int checks;
    private static class ProbePlayer extends net.minecraft.server.level.ServerPlayer {
        private final int permission;
        private final List<net.minecraft.network.chat.Component> messages = new ArrayList<>();
        ProbePlayer(MinecraftServer server, int permission) {
            super(server, server.overworld(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "ConfigProbe"));
            this.permission = permission;
        }
        @Override protected int getPermissionLevel() { return permission; }
        @Override public void sendSystemMessage(net.minecraft.network.chat.Component message) { messages.add(message); }
    }
    private static void expect(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }
    private static void until(BooleanSupplier condition, String label) throws Exception {
        long end = System.nanoTime() + 8_000_000_000L;
        while (!condition.getAsBoolean() && System.nanoTime() < end) Thread.sleep(50);
        expect(condition.getAsBoolean(), label);
    }
    private static Map<?, ?> rules() throws Exception {
        var field = UnifiedLoot.class.getDeclaredField("rules");
        field.setAccessible(true);
        return (Map<?, ?>) field.get(null);
    }
    private static void reload(MinecraftServer server) {
        server.submit(UnifiedLoot::reload).join();
    }
    private static com.electronwill.nightconfig.core.CommentedConfig table(Object chance, String item) {
        var table = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        table.set("chance", chance);
        table.set("items", item == null ? List.of() : List.of(item));
        return table;
    }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        var server = event.getServer();
        CompletableFuture.runAsync(() -> {
            try {
                ConfigTransitions.run();
                Path root = Path.of("config/the_long_travail");
                Path general = root.resolve("general.toml");
                expect(TravailConfig.BIOME_DRAW_COUNT.get() == 3, "malformed startup defaults loaded");
                expect(!ConfigFileIO.read(root.resolve("aspects/abyss.toml")).isEmpty(), "encoding recovery");
                expect((int) ConfigFileIO.read(root.resolve("aspects/far_reach.toml")).get("items.icarus.igniteSeconds") == 12, "numeric startup conversion");
                try (var files = Files.walk(root.resolve("invalid"))) {
                    var copies = files.filter(p -> p.toString().endsWith(".toml")).toList();
                    expect(copies.size() == 3, "three damaged/converted files backed up once");
                    Path original = copies.stream().filter(p -> p.getFileName().toString().equals("general.toml")).findFirst().orElseThrow();
                    expect(Arrays.equals(Files.readAllBytes(original), "chance = \n".getBytes(StandardCharsets.UTF_8)), "actual startup archive identity");
                }
                var managed = ConfigTracker.INSTANCE.fileMap().get("the_long_travail/general.toml");
                var config = ConfigFileIO.read(general);
                var previousPools = server.submit(com.thelongtravail.data.RuntimePools::current).join();
                config.set("requirements.biomeDrawCount", 7);
                ConfigFileIO.write(general, config);
                until(() -> TravailConfig.BIOME_DRAW_COUNT.get() == 7, "real watcher valid change");
                until(() -> server.submit(() -> com.thelongtravail.data.RuntimePools.current() != previousPools).join(),
                        "real watcher publishes runtime pools through registered reload coordinator");
                byte[] malformed = "items = [\"a\", \"b\"\n".getBytes(StandardCharsets.UTF_8);
                Files.write(general, malformed);
                Thread.sleep(1500);
                expect(TravailConfig.BIOME_DRAW_COUNT.get() == 7, "watcher syntax failure preserves live value");
                managed.save();
                expect(Arrays.equals(malformed, Files.readAllBytes(general)), "save does not overwrite rejected edit");
                config.set("requirements.biomeDrawCount", "bad");
                config.set("starter.giveStarterItem", false);
                ConfigFileIO.write(general, config);
                until(() -> !TravailConfig.GIVE_STARTER_ITEM.get(), "good neighbor applied");
                expect(TravailConfig.BIOME_DRAW_COUNT.get() == 7, "bad field retains previous");
                expect("bad".equals(ConfigFileIO.read(general).get("requirements.biomeDrawCount")), "invalid field remains on disk");
                config.set("requirements.biomeDrawCount", "9");
                ConfigFileIO.write(general, config);
                until(() -> TravailConfig.BIOME_DRAW_COUNT.get() == 9, "subsequent repair accepted");
                expect(ConfigFileIO.read(general).get("requirements.biomeDrawCount") instanceof Number, "valid numeric string saved");
                Files.delete(general);
                Thread.sleep(1200);
                expect(TravailConfig.BIOME_DRAW_COUNT.get() == 9, "deleted runtime file keeps active values");
                managed.save();
                expect(!Files.exists(general), "save does not recreate deleted runtime file");
                config.set("requirements.biomeDrawCount", 10);
                ConfigFileIO.write(general, config);
                until(() -> TravailConfig.BIOME_DRAW_COUNT.get() == 10, "recreated file reloads");
                String direct = Files.readString(general).replace("biomeDrawCount = 10", "biomeDrawCount = 11");
                Files.writeString(general, direct);
                until(() -> TravailConfig.BIOME_DRAW_COUNT.get() == 11, "direct editor save reloads");
                config.set("requirements.biomeDrawCount", 11);
                Files.write(general, "# 概率\n".getBytes(java.nio.charset.Charset.forName("GBK")));
                Thread.sleep(1200);
                expect(TravailConfig.BIOME_DRAW_COUNT.get() == 11, "runtime invalid encoding preserves active values");
                var tables = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
                tables.set(List.of("minecraft:chests/simple_dungeon"), table(.2, "minecraft:diamond|1|1|1"));
                tables.set(List.of("minecraft:chests/desert_pyramid"), table(.3, "minecraft:emerald|1|1|1"));
                config.set("loot.tables", tables);
                config.set("loot.perfect_fluid.fishingChance", .003);
                ConfigFileIO.write(general, config);
                reload(server);
                expect(rules().size() == 2, "two custom loot tables loaded");
                var dungeon = new ResourceLocation("minecraft:chests/simple_dungeon");
                var desert = new ResourceLocation("minecraft:chests/desert_pyramid");
                tables.set(List.of(dungeon.toString()), table("bad", "minecraft:diamond|1|1|1"));
                tables.set(List.of(desert.toString()), table("0.6", "minecraft:emerald|1|1|1"));
                tables.set(List.of("minecraft:chests/woodland_mansion"), table(.5, "missing:item|1|1|1"));
                config.set("loot.perfect_fluid.fishingChance", .004);
                ConfigFileIO.write(general, config);
                reload(server);
                expect(rules().size() == 2, "invalid new table skipped");
                expect(((UnifiedLoot.Rule) rules().get(dungeon)).chance() == .2f, "bad table keeps old rule");
                expect(((UnifiedLoot.Rule) rules().get(desert)).chance() == .6f, "other table updated with quoted number");
                expect(UnifiedLoot.fluidFishingChance == .004, "fishing independent of bad table");
                tables.remove(List.of(dungeon.toString()));
                tables.set(List.of(desert.toString()), table(0, "missing:item|1|1|1"));
                ConfigFileIO.write(general, config);
                reload(server);
                expect(rules().isEmpty(), "deletion and explicit zero respected");
                tables.set(List.of(desert.toString()), table(.8, null));
                ConfigFileIO.write(general, config);
                reload(server);
                expect(rules().isEmpty(), "empty list disables table");
                tables.set(List.of(desert.toString()), table(.8, "minecraft:emerald|1|1|1"));
                config.set("loot.enabled", false);
                ConfigFileIO.write(general, config);
                reload(server);
                expect(rules().isEmpty() && UnifiedLoot.fluidFishingChance == 0, "global disable respected");
                config.set("loot.enabled", true);
                config.set("loot.perfect_fluid.fishingChance", "bad");
                ConfigFileIO.write(general, config);
                reload(server);
                expect(rules().size() == 1 && UnifiedLoot.fluidFishingChance == .004, "bad fishing value independent of table");
                config.set("loot.perfect_fluid.fishingChance", .004);
                ConfigFileIO.write(general, config);
                server.reloadResources(server.getPackRepository().getSelectedIds()).join();
                expect(server.getLootData().getLootTable(desert).getPool(UnifiedLoot.POOL) != null, "actual resource reload injects pool");
                byte[] corrupt = "chance = \n".getBytes(StandardCharsets.UTF_8);
                Files.write(general, corrupt);
                reload(server);
                expect(rules().size() == 1 && UnifiedLoot.fluidFishingChance == .004, "syntax error retains whole active loot state");
                managed.save();
                expect(Arrays.equals(corrupt, Files.readAllBytes(general)), "pending watcher cannot overwrite new disk edit");
                server.submit(() -> {
                    var ordinary = new ProbePlayer(server, 0);
                    ConfigProblems.login(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(ordinary));
                    expect(ordinary.messages.isEmpty(), "non-admin does not receive server diagnostics");
                    var admin = new ProbePlayer(server, 2);
                    ConfigProblems.login(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(admin));
                    ConfigProblems.login(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(admin));
                    expect(admin.messages.size() == 1, "admin notification delivered once");
                    expect(admin.messages.get(0).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents,
                            "notification is localized");
                }).join();
                Files.writeString(Path.of("config-result.txt"), "PASS checks=" + checks + " actual Forge watcher, recovery, loot reload");
            } catch (Throwable failure) {
                failure.printStackTrace();
                try { Files.writeString(Path.of("config-result.txt"), "FAIL checks=" + checks + " " + failure); } catch (Exception ignored) {}
            } finally {
                server.execute(() -> server.halt(false));
            }
        });
    }
}

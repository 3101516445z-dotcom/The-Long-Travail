package travail.lanterntest;

import com.thelongtravail.client.lighting.LanternLighting;
import com.thelongtravail.client.lighting.LanternLightingClient;

import com.thelongtravail.client.*;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

@Mod("travail_lantern_client")
public final class LanternClientSmoke {
    private int ticks, idle;
    private boolean launched, finished;
    private volatile Throwable failure;
    private volatile boolean ready;
    private double sourceX;
    private UUID remoteId = UUID.randomUUID();
    private net.minecraft.server.level.ServerPlayer remote;
    private long start = System.nanoTime();

    public LanternClientSmoke() {
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }

    private static void check(boolean ok, String text) {
        if (!ok) throw new AssertionError(text);
    }

    private void server(
            java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> action) {
        var mc = Minecraft.getInstance();
        UUID id = mc.player.getUUID();
        mc.getSingleplayerServer()
                .execute(
                        () -> {
                            try {
                                action.accept(
                                        mc.getSingleplayerServer().getPlayerList().getPlayer(id));
                            } catch (Throwable t) {
                                failure = t;
                            }
                        });
    }

    private static void wear(net.minecraft.server.level.ServerPlayer p, boolean active) {
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p)
                .resolve()
                .orElseThrow()
                .getStacksHandler("belt")
                .orElseThrow()
                .getStacks()
                .setStackInSlot(
                        0,
                        active
                                ? new ItemStack(ModRegistry.SWORD_AND_LANTERN.get())
                                : ItemStack.EMPTY);
    }

    private void shot(String name) {
        var mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> {});
    }

    private void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            var mc = Minecraft.getInstance();
            if (failure != null) throw new AssertionError("server action", failure);
            if (System.nanoTime() - start > 180_000_000_000L)
                throw new AssertionError("client test timeout");
            if (mc.player == null) {
                if (!launched && mc.getOverlay() == null && ++idle > 40) {
                    launched = true;
                    mc.createWorldOpenFlows()
                            .loadLevel(
                                    new net.minecraft.client.gui.screens.TitleScreen(),
                                    "visual-world");
                }
                return;
            }
            if (ticks == 0) {
                ticks = 1;
                mc.setScreen(null);
                mc.options.hideGui = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.renderDistance().set(4);
                mc.options.simulationDistance().set(5);
                mc.options.gamma().set(.5D);
                server(
                        p -> {
                            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                            p.getAbilities().flying = true;
                            p.onUpdateAbilities();
                            p.setNoGravity(true);
                            var inv =
                                    top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p)
                                            .resolve()
                                            .orElseThrow();
                            inv.getCurios()
                                    .values()
                                    .forEach(
                                            slot -> {
                                                for (int i = 0;
                                                        i < slot.getStacks().getSlots();
                                                        i++)
                                                    slot.getStacks()
                                                            .setStackInSlot(i, ItemStack.EMPTY);
                                            });
                            p.serverLevel().setDayTime(18000);
                            p.serverLevel().setWeatherParameters(6000, 0, false, false);
                            for (int x = -12; x <= 12; x++)
                                for (int z = -12; z <= 12; z++)
                                    for (int y = 99; y <= 106; y++)
                                        p.serverLevel()
                                                .setBlock(
                                                        new BlockPos(x, y, z),
                                                        (Math.abs(x) == 12
                                                                                || Math.abs(z) == 12
                                                                                || y == 99
                                                                                || y == 106
                                                                        ? Blocks.STONE
                                                                        : Blocks.AIR)
                                                                .defaultBlockState(),
                                                        3);
                            p.teleportTo(p.serverLevel(), .5, 100, .5, 0, 30);
                            ready = true;
                        });
                return;
            }
            if (!ready) return;
            ticks++;
            if (ticks == 50) {
                check(LanternLightingClient.snapshot().length == 0, "unequipped is dark");
                shot("lantern-off");
                server(p -> wear(p, true));
            }
            if (ticks == 85) {
                check(
                        LanternLightingClient.snapshot().length == 1,
                        "server packet activates local light");
                var a = LanternLightingClient.snapshot()[0];
                sourceX = a.x();
                int rendered =
                        net.minecraft.client.renderer.LevelRenderer.getLightColor(
                                mc.level, new BlockPos(1, 100, 1));
                check((rendered & 255) > 0, "vanilla render light increased");
                check(
                        mc.level.getBrightness(
                                        net.minecraft.world.level.LightLayer.BLOCK,
                                        new BlockPos(1, 100, 1))
                                == 0,
                        "client world block light unchanged");
                server(
                        p ->
                                check(
                                        p.level()
                                                        .getBrightness(
                                                                net.minecraft.world.level.LightLayer
                                                                        .BLOCK,
                                                                new BlockPos(1, 100, 1))
                                                == 0,
                                        "server block light unchanged"));
                math();
                checkOptionalPipeline();
                shot("lantern-on");
                server(p -> p.teleportTo(p.serverLevel(), .85, 100, .5, 0, 30));
            }
            if (ticks == 105) {
                check(
                        Math.abs(LanternLightingClient.snapshot()[0].x() - sourceX - .35) < .06,
                        "sub-block motion updates light");
                shot("lantern-moved");
                server(
                        p -> {
                            wear(p, false);
                            remote =
                                    new net.minecraft.server.level.ServerPlayer(
                                            p.server,
                                            p.serverLevel(),
                                            new com.mojang.authlib.GameProfile(
                                                    remoteId, "LanternRemote"));
                            var connection =
                                    new net.minecraft.network.Connection(
                                            net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                                        @Override
                                        public void send(
                                                net.minecraft.network.protocol.Packet<?> packet) {}
                                    };
                            remote.connection =
                                    new net.minecraft.server.network.ServerGamePacketListenerImpl(
                                            p.server, connection, remote) {
                                        @Override
                                        public void send(
                                                net.minecraft.network.protocol.Packet<?> packet) {}
                                    };
                            remote.setPos(2, 100, 3);
                            remote.setNoGravity(true);
                            p.connection.send(
                                    net.minecraft.network.protocol.game
                                            .ClientboundPlayerInfoUpdatePacket
                                            .createPlayerInitializing(List.of(remote)));
                            p.serverLevel().addNewPlayer(remote);
                            var inv =
                                    top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(
                                                    remote)
                                            .resolve()
                                            .orElseThrow();
                            var slots =
                                    new HashMap<
                                            String,
                                            top.theillusivec4.curios.api.type.inventory
                                                    .ICurioStacksHandler>();
                            slots.put(
                                    "belt",
                                    new top.theillusivec4.curios.common.inventory
                                            .CurioStacksHandler(
                                            inv,
                                            "belt",
                                            1,
                                            true,
                                            false,
                                            true,
                                            top.theillusivec4.curios.api.type.capability.ICurio
                                                    .DropRule.DEFAULT));
                            inv.setCurios(slots);
                            wear(remote, true);
                            com.thelongtravail.valley.lantern.LanternSync.tracking(p, remote);
                        });
            }
            if (ticks == 145) {
                check(mc.level.getPlayerByUUID(remoteId) != null, "remote player is tracked");
                check(
                        LanternLightingClient.snapshot().length == 1,
                        "tracking packet lights remote player only");
                shot("lantern-remote");
                server(
                        p -> {
                            wear(remote, false);
                            com.thelongtravail.valley.lantern.LanternSync.tracking(p, remote);
                        });
            }
            if (ticks == 170) {
                check(
                        LanternLightingClient.snapshot().length == 0,
                        "remote unequip clears visual source");
                shot("lantern-cleared");
                System.out.println("LANTERN_VISUAL_CLIENT_PASS");
                finish(true);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            finish(false);
        }
    }

    private static void math() {
        var a = new LanternLighting.Source[] {new LanternLighting.Source(0, 0, 0)};
        var b = new LanternLighting.Source[] {a[0], a[0]};
        check(LanternLighting.brightness(a, 0, 0, 0) == 15, "light max 15");
        check(
                LanternLighting.brightness(a, LanternLighting.RADIUS + 1, 0, 0) == 0,
                "outside finite radius");
        check(
                LanternLighting.brightness(a, 2, 0, 0) == LanternLighting.brightness(b, 2, 0, 0),
                "lights use max not sum");
        var mixed =
                new LanternLighting.Source[] {
                    new LanternLighting.Source(7, 0, 0), new LanternLighting.Source(2, 0, 0)
                };
        check(
                Math.abs(LanternLighting.brightness(mixed, 0, 0, 0) - 15) < 1e-12,
                "nearest equal-strength source wins");
        check(
                LanternLighting.brightness(a, LanternLighting.RADIUS, 0, 0) == 0,
                "radius boundary is dark");
        check(
                LanternLighting.merge(a, 0, 0, 0, 0x00F0ABF0) == 0x00F0ABF0,
                "existing full block light preserved");
        check(
                LanternLighting.merge(new LanternLighting.Source[0], 0, 0, 0, 0x00F0AB42)
                        == 0x00F0AB42,
                "no source leaves packed light intact");
        check(
                (LanternLighting.merge(a, 0, 0, 0, 0x00F0AB42) & ~255) == 0x00F0AB00,
                "all non-block bits preserved");
        check(LanternLighting.brightness(a, 3, 0, 0) == 15, "three-block core is fully lit");
        check(
                Math.abs(LanternLighting.brightness(a, 7.5, 0, 0) - 7.5) < 1e-12,
                "outer ring fades to half brightness");
        int packed = LanternLighting.merge(a, 7.123, 0, 0, 12 << 20);
        check((packed >>> 20) == 12, "sky light preserved");
        check((packed & 15) != 0, "fractional block light preserved");
    }

    private static void checkOptionalPipeline() throws Exception {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("embeddium")) return;
        for (String name : List.of("smooth.SmoothLightPipeline", "flat.FlatLightPipeline")) {
            Class<?> c = Class.forName("me.jellysquid.mods.sodium.client.model.light." + name);
            check(
                    Arrays.stream(c.getDeclaredMethods())
                            .anyMatch(m -> m.getName().contains("vertexLantern")),
                    "Embeddium light adapter applied " + name);
        }
        if (Boolean.getBoolean("travail.lantern.shader")) {
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
            check(
                    (boolean) iris.getMethod("isPackInUseQuick").invoke(null),
                    "shader pack actually active");
            System.out.println(
                    "LANTERN_SHADER_ACTIVE: " + iris.getMethod("getCurrentPackName").invoke(null));
        }
        System.out.println("LANTERN_EMBEDDIUM_MIXINS_PASS");
    }

    private void finish(boolean ok) {
        finished = true;
        try {
            Files.writeString(
                    Path.of(
                            Minecraft.getInstance().gameDirectory.toString(),
                            "lantern-client-result.txt"),
                    ok ? "PASS" : "FAIL");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Minecraft.getInstance().stop();
    }
}

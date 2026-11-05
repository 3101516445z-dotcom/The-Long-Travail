package travail.boundlesstest;

import com.thelongtravail.boundless.dream.DreamState;

import com.thelongtravail.boundless.*;
import com.thelongtravail.boundless.timestop.TimeStopManager;
import com.thelongtravail.client.*;
import com.thelongtravail.client.timestop.TimeStopClient;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.client.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

import org.lwjgl.glfw.GLFW;

import java.nio.file.*;
import java.util.*;

@Mod("travail_boundless_client")
public final class BoundlessClientSmoke {
    int ticks, idle, freezeAge, logoutTicks, logoutEvents;
    boolean launched, finished, disconnected;
    volatile Throwable failure;
    volatile UUID allayId;

    public BoundlessClientSmoke() {
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(
                net.minecraftforge.eventbus.api.EventPriority.LOWEST, this::logout);
    }

    private void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        logoutEvents++;
        System.out.println("BOUNDLESS_LOGOUT_EVENT count=" + logoutEvents);
        System.out.println(
                "BOUNDLESS_LOGOUT_EVENT_STATE dream="
                        + BoundlessClient.dreaming
                        + " cooldown="
                        + BoundlessClient.cooldown
                        + " fields="
                        + TimeStopClient.fields.size());
        try {
            var f = net.minecraftforge.eventbus.EventBus.class.getDeclaredField("busID");
            f.setAccessible(true);
            for (var listener :
                    event.getListenerList().getListeners(f.getInt(MinecraftForge.EVENT_BUS)))
                if (listener.toString().contains("thelongtravail"))
                    System.out.println("BOUNDLESS_LOGOUT_LISTENER " + listener);
        } catch (Exception diagnostic) {
            diagnostic.printStackTrace();
        }
    }

    private static void check(boolean ok, String s) {
        if (!ok) throw new AssertionError(s);
    }

    private List<net.minecraft.network.chat.Component> starTooltip;

    private static String plain(net.minecraft.network.chat.Component c) {
        return net.minecraft.ChatFormatting.stripFormatting(c.getString());
    }

    private static void present(
            List<net.minecraft.network.chat.Component> lines, String key, Object... args) {
        String expected =
                plain(
                        net.minecraft.network.chat.Component.translatable(
                                "tooltip.the_long_travail." + key, args));
        var line =
                lines.stream()
                        .filter(c -> c.getString().equals(expected))
                        .findFirst()
                        .orElseThrow(
                                () -> new AssertionError("missing actual tooltip: " + expected));
        check(!line.getString().contains("§"), "legacy formatting must be resolved");
        line.visit(
                (style, segment) -> {
                    if (segment.chars().anyMatch(Character::isDigit) || segment.contains("%"))
                        check(
                                style.getColor() != null
                                        && style.getColor().getValue()
                                                == com.thelongtravail.abyss.rain.RainTooltips
                                                        .ACCENT_COLOR,
                                "number/percent/unit accent: " + segment);
                    return Optional.empty();
                },
                net.minecraft.network.chat.Style.EMPTY);
    }

    private static void slotGap(List<net.minecraft.network.chat.Component> lines) {
        for (int i = 0; i < lines.size(); i++)
            if (lines.get(i).getContents()
                            instanceof net.minecraft.network.chat.contents.TranslatableContents t
                    && t.getKey().equals("curios.tooltip.slot")) {
                check(
                        i + 1 < lines.size() && lines.get(i + 1).getString().isEmpty(),
                        "one blank immediately after default slot");
                check(
                        i + 2 < lines.size() && !lines.get(i + 2).getString().isEmpty(),
                        "description follows a single blank");
                return;
            }
        throw new AssertionError("default Curios slot line missing");
    }

    private void tooltipChecks() {
        var mc = Minecraft.getInstance();
        var dream = new ItemStack(ModRegistry.FREE_AND_EASY_WANDERING.get());
        var star = new ItemStack(ModRegistry.STAR_VOICE.get());
        for (var stack : List.of(dream, star)) {
            var lines =
                    stack.getTooltipLines(
                            mc.player, net.minecraft.world.item.TooltipFlag.Default.NORMAL);
            check(lines.size() >= 3, "actual tooltip contains summary and catalog entry");
            check(
                    lines.stream()
                            .noneMatch(c -> c.getString().contains("tooltip.the_long_travail.")),
                    "tooltip translated");
            var entry =
                    com.thelongtravail.client.catalog.CatalogEntries.forStack(stack).orElseThrow();
            var detail = com.thelongtravail.client.catalog.CatalogEntries.details(entry, stack);
            check(!detail.isEmpty(), "catalog details available");
            check(
                    detail.stream()
                            .noneMatch(c -> c.getString().contains("tooltip.the_long_travail.")),
                    "catalog translated");
        }
        var key = BoundlessClient.DREAM.getKey();
        try {
            BoundlessClient.DREAM.setKey(
                    com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(
                            GLFW.GLFW_KEY_G));
            var entry =
                    com.thelongtravail.client.catalog.CatalogEntries.forStack(dream).orElseThrow();
            check(
                    com.thelongtravail.client.catalog.CatalogEntries.details(entry, dream).stream()
                            .anyMatch(
                                    c ->
                                            c.getString()
                                                    .contains(
                                                            BoundlessClient.DREAM
                                                                    .getTranslatedKeyMessage()
                                                                    .getString())),
                    "catalog uses rebound dream key");
        } finally {
            BoundlessClient.DREAM.setKey(key);
        }
        starTooltip =
                star.getTooltipLines(
                        mc.player, net.minecraft.world.item.TooltipFlag.Default.NORMAL);
        mc.setScreen(
                tooltipScreen(
                        dream.getTooltipLines(
                                mc.player, net.minecraft.world.item.TooltipFlag.Default.NORMAL)));
        System.out.println("BOUNDLESS_CURRENT_TOOLTIP_PASS: summaries, catalog and rebound key");
    }

    private static net.minecraft.client.gui.screens.Screen tooltipScreen(
            List<net.minecraft.network.chat.Component> lines) {
        return new net.minecraft.client.gui.screens.Screen(
                net.minecraft.network.chat.Component.literal("Tooltip review")) {
            @Override
            public void render(
                    net.minecraft.client.gui.GuiGraphics graphics, int x, int y, float partial) {
                renderBackground(graphics);
                graphics.renderTooltip(
                        Minecraft.getInstance().font, lines, Optional.empty(), 12, 28);
            }
        };
    }

    private void server(
            java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> action) {
        var mc = Minecraft.getInstance();
        var id = mc.player.getUUID();
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

    private void key() throws Exception {
        key(GLFW.GLFW_PRESS);
    }

    private void key(int action) throws Exception {
        var mc = Minecraft.getInstance();
        mc.setWindowActive(true);
        var m =
                KeyboardHandler.class.getDeclaredMethod(
                        "keyPress", long.class, int.class, int.class, int.class, int.class);
        m.setAccessible(true);
        m.invoke(mc.keyboardHandler, mc.getWindow().getWindow(), GLFW.GLFW_KEY_T, 0, action, 0);
    }

    private void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || finished) return;
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        GLFW.glfwHideWindow(mc.getWindow().getWindow());
        try {
            if (failure != null) throw new AssertionError("server action", failure);
            if (disconnected) {
                logoutTicks++;
                if (logoutTicks == 1 || logoutTicks == 40)
                    System.out.println(
                            "BOUNDLESS_LOGOUT_STATE tick="
                                    + logoutTicks
                                    + " events="
                                    + logoutEvents
                                    + " player="
                                    + (mc.player != null)
                                    + " dreaming="
                                    + BoundlessClient.dreaming
                                    + " cooling="
                                    + BoundlessClient.cooling
                                    + " cooldown="
                                    + BoundlessClient.cooldown
                                    + " fields="
                                    + TimeStopClient.fields.size());
                // 整包可能异步离开世界，等待真实退出事件及队列排空后再检查状态。
                if (logoutTicks < 40) return;
                check(mc.player == null && logoutEvents > 0, "actual disconnect completed");
                check(
                        !BoundlessClient.dreaming
                                && !BoundlessClient.cooling
                                && BoundlessClient.cooldown == 0,
                        "real logout clears dream state");
                check(
                        TimeStopClient.fields.isEmpty() && !TimeStopClient.valid(mc.level),
                        "real logout clears time stop snapshot");
                System.out.println("BOUNDLESS_LOGOUT_PASS dream and time stop");
                System.out.println("BOUNDLESS_CLIENT_PASS");
                finish(true);
                return;
            }
            if (mc.player == null) {
                if (!launched && mc.getOverlay() == null && ++idle > 40) {
                    launched = true;
                    mc.createWorldOpenFlows()
                            .loadLevel(
                                    new net.minecraft.client.gui.screens.TitleScreen(),
                                    "visual-world");
                }
                if (idle > 1600) throw new AssertionError("world load timeout");
                return;
            }
            ticks++;
            if (ticks == 20) {
                com.thelongtravail.config.TravailClientConfig.SPEC.setConfig(
                        com.electronwill.nightconfig.core.CommentedConfig.inMemory());
                mc.setScreen(null);
                mc.options.hideGui = true;
                server(
                        p -> {
                            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                            p.getPersistentData().remove(DreamState.RECORD);
                            for (int x = -6; x <= 6; x++)
                                for (int z = -6; z <= 6; z++) {
                                    p.serverLevel()
                                            .setBlock(
                                                    new net.minecraft.core.BlockPos(x, 80, z),
                                                    net.minecraft.world.level.block.Blocks.STONE
                                                            .defaultBlockState(),
                                                    3);
                                    for (int y = 81; y <= 84; y++)
                                        p.serverLevel()
                                                .setBlock(
                                                        new net.minecraft.core.BlockPos(x, y, z),
                                                        net.minecraft.world.level.block.Blocks.AIR
                                                                .defaultBlockState(),
                                                        3);
                                }
                            p.teleportTo(p.serverLevel(), .5, 81, .5, 0, 0);
                            p.serverLevel().setWeatherParameters(6000, 0, false, false);
                            p.getPersistentData().putInt(DreamState.COOLDOWN, 0);
                            com.thelongtravail.network.BoundlessPacket.sendDream(p, false);
                            var inv =
                                    top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p)
                                            .resolve()
                                            .orElseThrow();
                            inv.getStacksHandler("head")
                                    .orElseThrow()
                                    .getStacks()
                                    .setStackInSlot(
                                            0,
                                            new ItemStack(
                                                    ModRegistry.FREE_AND_EASY_WANDERING.get()));
                        });
                com.thelongtravail.network.BoundlessPacket.receiver.accept(
                        new com.thelongtravail.network.BoundlessPacket(false, 1));
                for (int i = 0; i < 40; i++)
                    BoundlessClientEvents.BoundlessForgeBus.tick(
                            new TickEvent.ClientTickEvent(TickEvent.Phase.END));
                check(
                        BoundlessClient.cooling && BoundlessClient.cooldown == 1,
                        "client cannot predict cooldown unlock before server");
                com.thelongtravail.network.BoundlessPacket.receiver.accept(
                        new com.thelongtravail.network.BoundlessPacket(false, 0));
                check(!BoundlessClient.cooling, "server confirms cooldown unlock");
                for (var item :
                        List.of(
                                ModRegistry.FREE_AND_EASY_WANDERING.get(),
                                ModRegistry.STAR_VOICE.get()))
                    check(
                            mc.getItemRenderer()
                                            .getModel(new ItemStack(item), mc.level, mc.player, 0)
                                    != mc.getModelManager().getMissingModel(),
                            "model missing");
            }
            if (ticks == 35) tooltipChecks();
            if (ticks == 40)
                Screenshot.grab(
                        mc.gameDirectory,
                        "boundless-free_and_easy_wandering-tooltip.png",
                        mc.getMainRenderTarget(),
                        c -> {});
            if (ticks == 41) mc.setScreen(tooltipScreen(starTooltip));
            if (ticks == 44)
                Screenshot.grab(
                        mc.gameDirectory,
                        "boundless-star-voice-tooltip.png",
                        mc.getMainRenderTarget(),
                        c -> {});
            if (ticks == 45) mc.setScreen(null);
            if (ticks == 50) {
                key();
                check(mc.screen == null, "dream T must consume chat key");
            }
            if (ticks == 55) {
                key(GLFW.GLFW_REPEAT);
                check(mc.screen == null, "holding dream T must not open chat");
            }
            if (ticks == 60) key(GLFW.GLFW_RELEASE);
            if (ticks == 65) {
                check(
                        BoundlessClient.dreaming && mc.player.isSpectator(),
                        "C2S dream and S2C spectator sync");
                key();
            }
            if (ticks == 70) key(GLFW.GLFW_RELEASE);
            if (ticks == 80) {
                check(
                        !BoundlessClient.dreaming
                                && !mc.player.isSpectator()
                                && BoundlessClient.cooldown
                                        > com.thelongtravail.config.BoundlessItemsConfig.ticks(
                                                        "dream.cooldown")
                                                - 80
                                && BoundlessClient.cooldown
                                        <= com.thelongtravail.config.BoundlessItemsConfig.ticks(
                                                "dream.cooldown"),
                        "early wake and configured cooldown: "
                                + BoundlessClient.cooldown
                                + " dreaming="
                                + BoundlessClient.dreaming
                                + " spectator="
                                + mc.player.isSpectator());
                mc.player.setYRot(0);
                mc.player.setXRot(0);
                server(
                        p -> {
                            var pos = p.position().add(0, 1.6, 22);
                            // 创造模式玩家默认豁免时停。
                            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                            p.getAbilities().mayfly = true;
                            p.getAbilities().flying = true;
                            p.onUpdateAbilities();
                            var m = TimeStopManager.get(p.serverLevel());
                            check(
                                    m.start(
                                            p,
                                            false,
                                            p.level().getGameTime(),
                                            pos,
                                            8,
                                            400,
                                            Set.of(p.getUUID())),
                                    "visual field cast");
                            var allay =
                                    new net.minecraft.world.entity.animal.allay.Allay(
                                            net.minecraft.world.entity.EntityType.ALLAY,
                                            p.serverLevel());
                            allay.setPos(pos);
                            allay.getBrain()
                                    .setMemory(
                                            net.minecraft.world.entity.ai.memory.MemoryModuleType
                                                    .LIKED_PLAYER,
                                            p.getUUID());
                            allay.setNoAi(true);
                            allayId = allay.getUUID();
                            p.serverLevel().addFreshEntity(allay);
                            var q =
                                    new net.minecraft.server.level.ServerPlayer(
                                            p.server,
                                            p.serverLevel(),
                                            new com.mojang.authlib.GameProfile(
                                                    UUID.randomUUID(), "BoundaryEnemy"));
                            check(
                                    m.start(
                                            q,
                                            false,
                                            p.level().getGameTime(),
                                            pos.add(12, 0, 0),
                                            8,
                                            400,
                                            Set.of(q.getUUID())),
                                    "enemy boundary cast");
                        });
            }
            if (ticks == 90)
                com.thelongtravail.config.TravailClientConfig.TIME_STOP_BOUNDARY_LEVEL.set(1);
            if (ticks == 95)
                Screenshot.grab(
                        mc.gameDirectory,
                        "boundless-glass-sphere.png",
                        mc.getMainRenderTarget(),
                        c -> {});
            if (ticks == 99)
                com.thelongtravail.config.TravailClientConfig.TIME_STOP_BOUNDARY_LEVEL.set(2);
            if (ticks == 120)
                com.thelongtravail.config.TravailClientConfig.TIME_STOP_BOUNDARY_LEVEL.set(1);
            if (ticks == 100) {
                check(TimeStopClient.fields.size() == 2, "server field snapshot received");
                check(
                        TimeStopClient.fields.get(0).allies().contains(allayId),
                        "server authoritative Allay exemption snapshot");
                check(!TimeStopManager.frozen(mc.player), "outside fields player not frozen");
                check(
                        TimeStopClient.boundaryDanger(TimeStopClient.fields.get(1).center()),
                        "red enemy boundary");
                check(
                        !TimeStopClient.boundaryDanger(TimeStopClient.fields.get(0).center()),
                        "purple exempt boundary");
                Screenshot.grab(
                        mc.gameDirectory,
                        "boundless-boundaries.png",
                        mc.getMainRenderTarget(),
                        c -> {});
            }
            if (ticks == 102)
                server(
                        p -> {
                            var f =
                                    TimeStopManager.fields(p.serverLevel()).stream()
                                            .filter(v -> !v.allies.contains(p.getUUID()))
                                            .findFirst()
                                            .orElseThrow();
                            var pos = f.center.add(-10, 0, 0);
                            p.teleportTo(p.serverLevel(), pos.x, pos.y, pos.z, 0, 0);
                        });
            if (ticks == 108) {
                var pos = mc.player.position();
                mc.getConnection()
                        .send(
                                new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
                                        .PosRot(pos.x + 3, pos.y, pos.z, 0, 0, false));
            }
            if (ticks == 114)
                server(
                        p -> {
                            check(
                                    TimeStopManager.frozen(p),
                                    "real movement packet enters and freezes at boundary");
                            TimeStopManager.get(p.serverLevel()).expire(Long.MAX_VALUE);
                            var q =
                                    new net.minecraft.server.level.ServerPlayer(
                                            p.server,
                                            p.serverLevel(),
                                            new com.mojang.authlib.GameProfile(
                                                    UUID.randomUUID(), "Freezer"));
                            TimeStopManager.get(p.serverLevel())
                                    .start(
                                            q,
                                            false,
                                            p.level().getGameTime(),
                                            p.position(),
                                            8,
                                            100,
                                            Set.of(q.getUUID()));
                        });
            if (ticks == 125) {
                check(TimeStopManager.frozen(mc.player), "nonexempt local player frozen");
                freezeAge = mc.player.tickCount;
                Screenshot.grab(
                        mc.gameDirectory,
                        "boundless-glass-sphere-inside.png",
                        mc.getMainRenderTarget(),
                        c -> {});
            }
            if (ticks == 145) {
                check(mc.player.tickCount == freezeAge, "local player clock paused");
                server(
                        p -> {
                            TimeStopManager.get(p.serverLevel()).expire(Long.MAX_VALUE);
                            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p)
                                    .resolve()
                                    .orElseThrow()
                                    .getStacksHandler("head")
                                    .orElseThrow()
                                    .getStacks()
                                    .setStackInSlot(0, ItemStack.EMPTY);
                        });
            }
            if (ticks == 165) {
                check(
                        !TimeStopManager.frozen(mc.player) && TimeStopClient.fields.isEmpty(),
                        "unfreeze snapshot");
                key();
            }
            if (ticks == 170) {
                System.out.println("BOUNDLESS_PRE_DISCONNECT events=" + logoutEvents);
                check(
                        mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen,
                        "T opens chat without ready dream");
                mc.setScreen(null);
                com.thelongtravail.network.BoundlessPacket.receiver.accept(
                        new com.thelongtravail.network.BoundlessPacket(true, 20));
                com.thelongtravail.network.TimeStopPacket.receiver.accept(
                        new com.thelongtravail.network.TimeStopPacket(
                                mc.level.dimension().location(),
                                List.of(
                                        new com.thelongtravail.network.TimeStopPacket.View(
                                                999, mc.player.position(), 2, Set.of()))));
                check(
                        BoundlessClient.dreaming && !TimeStopClient.fields.isEmpty(),
                        "logout fixtures active");
                System.out.println("BOUNDLESS_SEEDED_LOGOUT events=" + logoutEvents);
                mc.level.disconnect();
                mc.clearLevel();
                System.out.println(
                        "BOUNDLESS_RETURNED_DISCONNECT events="
                                + logoutEvents
                                + " dream="
                                + BoundlessClient.dreaming
                                + " cooldown="
                                + BoundlessClient.cooldown
                                + " fields="
                                + TimeStopClient.fields.size());
                disconnected = true;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            finish(false);
        }
    }

    private void finish(boolean ok) {
        finished = true;
        try {
            Files.writeString(
                    Path.of(
                            Minecraft.getInstance().gameDirectory.toString(),
                            "boundless-client-result.txt"),
                    ok ? "PASS" : "FAIL");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Minecraft.getInstance().stop();
    }
}

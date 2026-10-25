package travail.smoke;

import com.thelongtravail.client.ColoringScreen;
import com.thelongtravail.client.DiaryFontEffects;
import com.thelongtravail.client.catalog.CatalogEntries;
import com.thelongtravail.client.catalog.CatalogSession;
import com.thelongtravail.item.NameColoring;
import com.thelongtravail.menu.ColoringMenu;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.lwjgl.glfw.GLFW;

import java.nio.file.*;

@Mod.EventBusSubscriber(modid = "travail_smoke", value = Dist.CLIENT)
public final class ColoringClientSmoke {
    private static int ticks, phase, wait, choice;
    private static long stressStart, stressFrames, lastFrame, peakHeap;
    private static int stressSelections;
    private static final java.util.ArrayList<Long> frameTimes = new java.util.ArrayList<>();
    private static boolean finished;
    private static volatile boolean reloaded;
    private static volatile Throwable reloadFailure;

    @SubscribeEvent
    public static void render(net.minecraftforge.client.event.ScreenEvent.Render.Post event) {
        var mc = Minecraft.getInstance();
        if (phase == 14) {
            event.getGuiGraphics()
                    .renderTooltip(
                            mc.font,
                            new ItemStack(ModRegistry.COLORING.get()),
                            mc.getWindow().getGuiScaledWidth() / 2 - 50,
                            mc.getWindow().getGuiScaledHeight() / 2 - 30);
        }
        if (phase == 11) {
            long now = System.nanoTime();
            if (lastFrame != 0) frameTimes.add(now - lastFrame);
            lastFrame = now;
            stressFrames++;
            peakHeap =
                    Math.max(
                            peakHeap,
                            Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        }
        if (choice < 10
                && event.getScreen() instanceof ColoringScreen
                && mc.player != null
                && mc.player.containerMenu instanceof ColoringMenu menu
                && menu.getSlot(1).hasItem()) {
            // Deterministic hover rendering even when the OS does not focus the test window.
            event.getGuiGraphics()
                    .renderTooltip(
                            mc.font,
                            menu.getSlot(1).getItem(),
                            (mc.getWindow().getGuiScaledWidth() - 284) / 2 + 195,
                            (mc.getWindow().getGuiScaledHeight() - 254) / 2 + 55);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        try {
            if (++ticks > 24000) throw new AssertionError("client fixture timeout phase=" + phase);
            if (ticks % 200 == 0)
                System.out.println(
                        "COLORING_CLIENT phase="
                                + phase
                                + " selections="
                                + stressSelections
                                + " seconds="
                                + (stressStart == 0
                                        ? 0
                                        : (System.nanoTime() - stressStart) / 1000000000L)
                                + " screen="
                                + mc.screen);
            if (phase == 0
                    && ticks > 100
                    && mc.getOverlay() == null
                    && mc.screen != null
                    && mc.screen
                            .getClass()
                            .getSimpleName()
                            .matches("AccessibilityOnboardingScreen|StartupAdviceScreen")) {
                mc.setScreen(new TitleScreen());
            }
            if (phase == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                phase = 1;
                mc.createWorldOpenFlows().loadLevel(new TitleScreen(), "coloring-test");
            } else if (phase == 1
                    && mc.player != null
                    && mc.level != null
                    && mc.getSingleplayerServer() != null) {
                if (mc.player.isDeadOrDying()) {
                    mc.player.respawn();
                    return;
                }
                phase = 2;
                wait = 0;
                mc.getSingleplayerServer()
                        .execute(
                                () -> {
                                    var player =
                                            mc.getSingleplayerServer()
                                                    .getPlayerList()
                                                    .getPlayer(mc.player.getUUID());
                                    mc.getSingleplayerServer()
                                            .setDifficulty(
                                                    net.minecraft.world.Difficulty.PEACEFUL, true);
                                    player.setHealth(player.getMaxHealth());
                                    player.getFoodData().setFoodLevel(20);
                                    player.getInventory().clearContent();
                                    player.getInventory()
                                            .setItem(
                                                    player.getInventory().selected,
                                                    new ItemStack(ModRegistry.COLORING.get()));
                                    player.containerMenu.broadcastChanges();
                                });
            } else if (phase == 2
                    && ++wait > 20
                    && mc.player.getMainHandItem().is(ModRegistry.COLORING.get())) {
                phase = 3;
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            } else if (phase == 3 && mc.screen instanceof ColoringScreen) {
                phase = 4;
                wait = 0;
                mc.getSingleplayerServer()
                        .execute(
                                () -> {
                                    var player =
                                            mc.getSingleplayerServer()
                                                    .getPlayerList()
                                                    .getPlayer(mc.player.getUUID());
                                    ItemStack sample = new ItemStack(Items.DIAMOND_SWORD);
                                    sample.setHoverName(Component.literal("施色 · 星河之剑"));
                                    if (net.minecraftforge.fml.ModList.get().isLoaded("apotheosis"))
                                        AffixColoringChecks.preview(sample);
                                    player.containerMenu.getSlot(0).set(sample);
                                    player.containerMenu.broadcastChanges();
                                });
            } else if (phase == 4 && ++wait > 15) {
                phase = 5;
                wait = 0;
                int bx =
                        choice == 0
                                ? 14
                                : choice < 7 ? 55 + (choice - 1) % 3 * 41 : choice < 9 ? 178 : 234;
                int by =
                        93
                                + (choice > 0 && choice < 7
                                        ? (choice - 1) / 3 * 29
                                        : choice == 8 ? 29 : 0);
                if (!mc.screen.mouseClicked(
                        (mc.getWindow().getGuiScaledWidth() - 284) / 2 + bx + 18,
                        (mc.getWindow().getGuiScaledHeight() - 254) / 2 + by + 12,
                        0)) throw new AssertionError("button mouse hit failed " + choice);
            } else if (phase == 5 && ++wait > 15) {
                var menu = (ColoringMenu) mc.player.containerMenu;
                if (!awaitSync(
                        menu.selection() == choice
                                && NameColoring.selection(menu.getSlot(1).getItem()) == choice,
                        "selection/output not synchronized " + choice)) return;
                int material =
                        DiaryFontEffects.material(
                                menu.getSlot(1).getItem().getHoverName().getVisualOrderText());
                int expected = choice == 0 ? 0 : choice <= 6 ? choice + 2 : choice < 9 ? 1 : -1;
                if (material != expected)
                    throw new AssertionError("wrong material " + choice + ": " + material);
                if (choice == 7 || choice == 8) {
                    float width =
                            DiaryFontEffects.sweepWidth(
                                    menu.getSlot(1).getItem().getHoverName().getVisualOrderText());
                    if ((choice == 7 && width <= 1) || (choice == 8 && width >= -1))
                        throw new AssertionError("prose width/sign");
                }
                double scale = mc.getWindow().getGuiScale();
                GLFW.glfwSetCursorPos(
                        mc.getWindow().getWindow(),
                        ((mc.getWindow().getGuiScaledWidth() - 284) / 2.0 + 195) * scale,
                        ((mc.getWindow().getGuiScaledHeight() - 254) / 2.0 + 55) * scale);
                phase = 6;
                wait = 0;
            } else if (phase == 6 && ++wait > 15) {
                Screenshot.grab(
                        mc.gameDirectory,
                        "coloring-" + choice + ".png",
                        mc.getMainRenderTarget(),
                        message -> {});
                if (++choice < 10) {
                    phase = 4;
                    wait = 0;
                } else {
                    GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 0, 0);
                    phase = 8;
                    wait = 0;
                    mc.reloadResourcePacks()
                            .whenComplete(
                                    (unused, failure) -> {
                                        reloadFailure = failure;
                                        reloaded = true;
                                    });
                }
            } else if (phase == 8 && reloaded && mc.getOverlay() == null) {
                if (reloadFailure != null)
                    throw new AssertionError("resource reload failed", reloadFailure);
                phase = 11;
                stressStart = System.nanoTime();
                choice = 8;
                wait = 0;
                mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId, 8);
            } else if (phase == 11 && ++wait > 5) {
                if (!(mc.player.containerMenu instanceof ColoringMenu))
                    throw new AssertionError(
                            "stress menu unexpectedly closed; health=" + mc.player.getHealth());
                var active = (ColoringMenu) mc.player.containerMenu;
                if (!awaitSync(
                        active.selection() == choice
                                && NameColoring.selection(active.getSlot(1).getItem()) == choice,
                        "stress synchronization " + choice)) return;
                int material =
                        DiaryFontEffects.material(
                                active.getSlot(1).getItem().getHoverName().getVisualOrderText());
                int expected = choice == 0 ? 0 : choice <= 6 ? choice + 2 : choice < 9 ? 1 : -1;
                if (material != expected) throw new AssertionError("stress material " + choice);
                stressSelections++;
                wait = 0;
                if (System.nanoTime() - stressStart
                        >= Long.getLong("coloring.stressNanos", 600_000_000_000L)) {
                    phase = 9;
                    java.util.Collections.sort(frameTimes);
                    Files.writeString(
                            Path.of("client-stress.txt"),
                            "PASS durationSeconds="
                                    + (System.nanoTime() - stressStart) / 1e9
                                    + " selections="
                                    + stressSelections
                                    + " frames="
                                    + stressFrames
                                    + " peakUsedHeapMiB="
                                    + peakHeap / 1048576
                                    + " frameP50Ms="
                                    + frameTimes.get(frameTimes.size() / 2) / 1e6
                                    + " frameP99Ms="
                                    + frameTimes.get(frameTimes.size() * 99 / 100) / 1e6
                                    + " maxFrameMs="
                                    + frameTimes.get(frameTimes.size() - 1) / 1e6);
                    mc.gameMode.handleInventoryButtonClick(active.containerId, 8);
                } else {
                    choice = (choice + 1) % 10;
                    clickChoice(mc, choice);
                }
            } else if (phase == 9 && ++wait > 20) {
                var menu = (ColoringMenu) mc.player.containerMenu;
                if (!awaitSync(
                        menu.selection() == 8
                                && NameColoring.selection(menu.getSlot(1).getItem()) == 8,
                        "final selection not synchronized")) return;
                if (DiaryFontEffects.material(
                                menu.getSlot(1).getItem().getHoverName().getVisualOrderText())
                        != 1) throw new AssertionError("material lost after reload");
                Screenshot.grab(
                        mc.gameDirectory,
                        "coloring-reloaded.png",
                        mc.getMainRenderTarget(),
                        message -> {});
                if (!mc.screen.mouseClicked(
                        (mc.getWindow().getGuiScaledWidth() - 284) / 2 + 195,
                        (mc.getWindow().getGuiScaledHeight() - 254) / 2 + 55,
                        0)) throw new AssertionError("output mouse hit failed");
                phase = 10;
                wait = 0;
            } else if (phase == 10 && ++wait > 20) {
                var menu = (ColoringMenu) mc.player.containerMenu;
                if (!awaitSync(
                        NameColoring.selection(menu.getCarried()) == 8
                                && menu.getCarried().getCount() == 1
                                && !menu.getSlot(0).hasItem()
                                && !menu.getSlot(1).hasItem(),
                        "network output take did not conserve item")) return;
                if (!mc.screen.mouseClicked(
                        (mc.getWindow().getGuiScaledWidth() - 284) / 2 + 250,
                        (mc.getWindow().getGuiScaledHeight() - 254) / 2 + 25,
                        0)) throw new AssertionError("close mouse hit failed");
                phase = 7;
                wait = 0;
            } else if (phase == 7 && ++wait > 20) {
                if (!awaitSync(
                        !(mc.player.containerMenu instanceof ColoringMenu)
                                && mc.player.getInventory().countItem(Items.DIAMOND_SWORD) == 1,
                        "close return not synchronized")) return;
                mc.setScreen(new InventoryScreen(mc.player));
                CatalogSession.open(mc.screen);
                phase = 12;
                wait = 0;
            } else if (phase == 12 && ++wait > 60) {
                var session = CatalogSession.active();
                var aids =
                        CatalogEntries.all().stream()
                                .filter(e -> e.category() == CatalogEntries.Category.AID)
                                .toList();
                var stones =
                        CatalogEntries.all().stream()
                                .filter(e -> e.category() == CatalogEntries.Category.STONE)
                                .toList();
                if (aids.size() != 4) throw new AssertionError("aid count");
                for (int i = 0; i < 4; i++)
                    if (session.parent().cardCenter(aids.get(i).id())[0]
                            != session.parent().cardCenter(stones.get(i + 1).id())[0])
                        throw new AssertionError("catalog column alignment " + i);
                captureCatalog(mc, "catalog-position");
                session.detail(
                        CatalogEntries.byId("coloring"), new ItemStack(ModRegistry.COLORING.get()));
                phase = 13;
                wait = 0;
            } else if (phase == 13 && ++wait > 60) {
                captureCatalog(mc, "catalog-description");
                CatalogSession.active().close();
                phase = 14;
                wait = 0;
            } else if (phase == 14 && ++wait > 60) {
                captureCatalog(mc, "catalog-tooltip");
                phase = 15;
                wait = 0;
            } else if (phase == 15 && ++wait > 40) {
                finish(
                        mc,
                        true,
                        "PASS: right-click open, 10 selections, rendered material checks,"
                            + " screenshots, resource reload, network output take and close return;"
                            + " integrated-server sync; catalog alignment, detail and tooltip"
                            + " screenshots. Environment: "
                                + (Boolean.getBoolean("coloring.fullpack")
                                        ? "full pack copy"
                                        : "isolated")
                                + ". Remote multiplayer NOT_RUN.");
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            finish(mc, false, failure.toString());
        }
    }

    /** 整合包启动可能阻塞服务器；等待确认，不重发操作或跳过断言。 */
    private static boolean awaitSync(boolean ready, String message) {
        if (!ready && wait > 600) throw new AssertionError(message + " after 600 client ticks");
        return ready;
    }

    private static void clickChoice(Minecraft mc, int choice) {
        int x = choice == 0 ? 14 : choice < 7 ? 55 + (choice - 1) % 3 * 41 : choice < 9 ? 178 : 234;
        int y = 93 + (choice > 0 && choice < 7 ? (choice - 1) / 3 * 29 : choice == 8 ? 29 : 0);
        double mx = (mc.getWindow().getGuiScaledWidth() - 284) / 2 + x + 18;
        double my = (mc.getWindow().getGuiScaledHeight() - 254) / 2 + y + 12;
        if (!mc.screen.mouseClicked(mx, my, 0))
            throw new AssertionError("stress button hit " + choice);
        mc.screen.mouseReleased(mx, my, 0);
    }

    private static void captureCatalog(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), ignored -> {});
    }

    private static void finish(Minecraft mc, boolean success, String report) {
        finished = true;
        try {
            Files.writeString(Path.of("client-checks.txt"), report);
            Files.writeString(Path.of("client-result.txt"), success ? "PASS" : "FAIL");
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        mc.stop();
    }
}

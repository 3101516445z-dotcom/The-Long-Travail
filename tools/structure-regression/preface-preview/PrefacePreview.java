package travail.prefacepreview;

import com.thelongtravail.client.TravelDiaryScreen;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.*;

@Mod("travail_smoke")
public class PrefacePreview {
    int phase, ticks, wait, saved, scaleIndex;
    boolean done, safe;
    TravelDiaryScreen page;
    ItemStack book;
    final int[] scales = {1, 2, 3, 0};
    final LayoutStress stress = new LayoutStress();

    public PrefacePreview() {
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::renderTick);
        MinecraftForge.EVENT_BUS.addListener(this::markCursor);
    }

    int hoverStep;

    void markCursor(net.minecraftforge.client.event.ScreenEvent.Render.Post e) {
        if (!hoverCheck() || e.getScreen() != page) return;
        var g = e.getGuiGraphics();
        int x = e.getMouseX(), y = e.getMouseY();
        g.pose().pushPose();
        g.pose().translate(0, 0, 800);
        g.fill(x - 3, y, x + 4, y + 1, 0xFFFF00FF);
        g.fill(x, y - 3, x + 1, y + 4, 0xFFFF00FF);
        g.pose().popPose();
    }

    boolean hoverCheck() {
        return Files.exists(
                Minecraft.getInstance().gameDirectory.toPath().resolve("bookmark-hover-check"));
    }

    void hoverProbe() throws Exception {
        var mc = Minecraft.getInstance();
        if (wait == 1) {
            PrefaceAcceptance.scale(hoverStep < 3 ? 2 : 3);
        }
        if (wait == 15) {
            var w = mc.getWindow();
            float scale = (float) field("bookScale");
            double x = (int) field("bookLeft") + (32 + (hoverStep % 3) * 7) * scale;
            double y = (int) field("bookTop") + (96 + (hoverStep % 3) * 7) * scale;
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(
                    w.getWindow(),
                    x * w.getScreenWidth() / w.getGuiScaledWidth(),
                    y * w.getScreenHeight() / w.getGuiScaledHeight());
            // 未聚焦的 GLFW 窗口可能忽略移动，改为调用 Minecraft 对应的输入回调。
            var move =
                    net.minecraftforge.fml.util.ObfuscationReflectionHelper.findMethod(
                            MouseHandler.class, "m_91561_", long.class, double.class, double.class);
            double nativeX = x * w.getScreenWidth() / w.getGuiScaledWidth();
            double nativeY = y * w.getScreenHeight() / w.getGuiScaledHeight();
            move.invoke(mc.mouseHandler, w.getWindow(), nativeX, nativeY);
            move.invoke(mc.mouseHandler, w.getWindow(), nativeX, nativeY);
            check(Math.abs(mc.mouseHandler.xpos() - nativeX) < 1, "Mouse callback X");
            check(Math.abs(mc.mouseHandler.ypos() - nativeY) < 1, "Mouse callback Y");
            Files.writeString(
                    mc.gameDirectory.toPath().resolve("hover-coordinates.txt"),
                    "step="
                            + hoverStep
                            + " guiScale="
                            + w.getGuiScale()
                            + " requested="
                            + x
                            + ","
                            + y
                            + "\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        }
        if (wait > 35) {
            shot("hover-" + hoverStep);
            if (++hoverStep == 6) {
                Files.writeString(
                        mc.gameDirectory.toPath().resolve("result.txt"),
                        "PASS hover screenshots=6; no long text injected");
                done = true;
                mc.stop();
            }
            wait = 0;
        }
    }

    // 部分菜单优化模组会在进入世界前暂停客户端 tick。
    void renderTick(TickEvent.RenderTickEvent event) {
        if (phase == 0 && event.phase == TickEvent.Phase.END)
            tick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
    }

    Object field(String key) throws Exception {
        var f = TravelDiaryScreen.class.getDeclaredField(key);
        f.setAccessible(true);
        return f.get(page);
    }

    void click(double x, double y) throws Exception {
        float scale = (float) field("bookScale");
        page.mouseClicked(
                (int) field("bookLeft") + x * scale, (int) field("bookTop") + y * scale, 0);
    }

    void bookmark(int index) throws Exception {
        click(
                PrefaceAcceptance.layout("BOOKMARK_LEFT") + 10,
                PrefaceAcceptance.layout("BOOKMARK_FIRST_Y")
                        + index * PrefaceAcceptance.layout("BOOKMARK_STEP")
                        + 12);
    }

    void open() {
        var mc = Minecraft.getInstance();
        org.lwjgl.glfw.GLFW.glfwRestoreWindow(mc.getWindow().getWindow());
        org.lwjgl.glfw.GLFW.glfwShowWindow(mc.getWindow().getWindow());
        page = new TravelDiaryScreen(() -> book, () -> mc.setScreen(null));
        mc.setScreen(page);
    }

    void shot(String name) {
        var mc = Minecraft.getInstance();
        check(mc.screen == page, "Diary must be visible");
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), m -> saved++);
    }

    void check(boolean v, String why) {
        if (!v) throw new AssertionError(why);
    }

    void tick(TickEvent.ClientTickEvent e) {
        if (done || e.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        try {
            if (mc.screen instanceof net.minecraftforge.client.gui.LoadingErrorScreen) {
                Screenshot.grab(
                        mc.gameDirectory, "loading-error.png", mc.getMainRenderTarget(), m -> {});
                throw new AssertionError(
                        "Forge LoadingErrorScreen: inspect console.log and loading-error.png");
            }
            if (ticks == 0) {
                org.lwjgl.glfw.GLFW.glfwRestoreWindow(mc.getWindow().getWindow());
                org.lwjgl.glfw.GLFW.glfwShowWindow(mc.getWindow().getWindow());
            }
            if (ticks % 200 == 0) {
                System.out.println(
                        "PREFACE_PROGRESS phase="
                                + phase
                                + " ticks="
                                + ticks
                                + " screen="
                                + (mc.screen == null ? "null" : mc.screen.getClass().getName()));
            }
            if (mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen
                    && mc.player != null) {
                mc.player.respawn();
                mc.setScreen(null);
                safe = false;
                return;
            }
            if (!safe && mc.player != null && mc.getSingleplayerServer() != null) {
                safe = true;
                mc.getSingleplayerServer()
                        .execute(
                                () ->
                                        mc.getSingleplayerServer()
                                                .getPlayerList()
                                                .getPlayers()
                                                .forEach(
                                                        p -> {
                                                            p.setGameMode(
                                                                    net.minecraft.world.level
                                                                            .GameType.CREATIVE);
                                                            p.setHealth(p.getMaxHealth());
                                                        }));
            }
            if (++ticks > 6000) throw new AssertionError("timeout");
            if (phase == 0
                    && mc.getOverlay() == null
                    && mc.screen != null
                    && mc.screen
                            .getClass()
                            .getSimpleName()
                            .matches("AccessibilityOnboardingScreen|StartupAdviceScreen"))
                mc.setScreen(new TitleScreen());
            if (phase == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                phase = 1;
                mc.createWorldOpenFlows().loadLevel(new TitleScreen(), "preview");
            } else if (phase == 1 && mc.player != null && ++wait > 80) {
                book = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
                open();
                check((boolean) field("preface"), "Initial preface");
                phase = 2;
                wait = 0;
            } else if (phase == 2 && hoverCheck()) {
                ++wait;
                hoverProbe();
            } else if (phase == 2 && ++wait > 40) {
                shot("01-preface");
                if (Boolean.getBoolean("diary.prefaceScreenshotOnly")) {
                    Files.writeString(
                            mc.gameDirectory.toPath().resolve("result.txt"),
                            "PASS preface screenshot only; no full regression requested");
                    done = true;
                    mc.stop();
                    return;
                }
                for (int i = 0; i < 6; i++) {
                    bookmark(i + 1);
                    check(!(boolean) field("preface"), "Theme switch");
                    check(((Enum<?>) field("selected")).ordinal() == i, "Theme index");
                    bookmark(0);
                    check((boolean) field("preface"), "Return preface");
                }
                bookmark(4);
                phase = 3;
                wait = 0;
            } else if (phase == 3 && ++wait > 40) {
                shot("02-theme-with-preface-bookmark");
                page.onClose();
                open();
                check(!(boolean) field("preface"), "Remember theme");
                check(((Enum<?>) field("selected")).ordinal() == 3, "Remember theme index");
                bookmark(0);
                phase = 4;
                wait = 0;
            } else if (phase == 4 && ++wait > 30) {
                page.onClose();
                open();
                check((boolean) field("preface"), "Remember preface");
                phase = 5;
                wait = 0;
            } else if (phase == 5 && saved == 2 && ++wait > 20) {
                org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 900, 640);
                phase = 6;
                wait = 0;
            } else if (phase == 6 && ++wait > 40) {
                shot("03-preface-small-window");
                phase = 7;
                wait = 0;
            } else if (phase == 7 && saved == 3 && ++wait > 15) {
                PrefaceAcceptance.installLongCopy();
                phase = 8;
                wait = 0;
            } else if (phase == 8 && ++wait > 20) {
                PrefaceAcceptance.verifyScroll(this);
                shot("04-long-copy-scroll");
                PrefaceAcceptance.restore();
                org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1920, 1080);
                PrefaceAcceptance.scale(scales[0]);
                phase = 9;
                wait = 0;
            } else if (phase == 9 && ++wait > 30) {
                if (scales[scaleIndex] == 3)
                    check(mc.getWindow().getGuiScale() >= 3, "Actual GUI scale 3");
                for (int i = 0; i < 6; i++) {
                    bookmark(i + 1);
                    check(!(boolean) field("preface"), "Scaled theme switch");
                    check(((Enum<?>) field("selected")).ordinal() == i, "Scaled theme index");
                    bookmark(0);
                    check((boolean) field("preface"), "Scaled preface return");
                }
                shot("05-scale-" + scales[scaleIndex]);
                scaleIndex++;
                wait = 0;
                if (scaleIndex < scales.length) PrefaceAcceptance.scale(scales[scaleIndex]);
                else phase = 10;
            } else if (phase == 10 && saved == 8 && ++wait > 15) {
                PrefaceAcceptance.installLongCopy();
                stress.begin();
                phase = 11;
            } else if (phase == 11 && stress.step(this)) {
                PrefaceAcceptance.restore();
                phase = 12;
                wait = 0;
            } else if (phase == 12 && saved == 32 && ++wait > 15) {
                Files.writeString(
                        mc.gameDirectory.toPath().resolve("result.txt"),
                        "PASS initial preface; six theme switches; preface return; reopen remembers"
                                + " theme and preface; independent long-copy wheel/key scrolling and"
                                + " bounds; GUI scales 1/2/3/auto navigation; layout stress 24"
                                + " scenarios; screenshots=32");
                done = true;
                mc.stop();
            }
        } catch (Throwable ex) {
            PrefaceAcceptance.restore();
            ex.printStackTrace();
            try {
                Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"), "FAIL " + ex);
            } catch (Exception ignored) {
            }
            done = true;
            mc.stop();
        }
    }
}

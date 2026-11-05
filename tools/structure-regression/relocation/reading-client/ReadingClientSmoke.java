package travail.readingtest;

import com.thelongtravail.client.diary.DiaryEntryHold;
import com.thelongtravail.client.diary.DiaryReadingTarget;
import com.thelongtravail.client.diary.DiarySession;
import com.thelongtravail.client.diary.TravelDiaryScreen;

import com.thelongtravail.client.*;
import com.thelongtravail.journey.LongTravailData;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

import org.lwjgl.glfw.GLFW;

import top.theillusivec4.curios.api.CuriosApi;

import java.nio.file.*;
import java.util.UUID;

@Mod("travail_reading_test")
public final class ReadingClientSmoke {
    private int ticks, idle, replacements;
    private boolean launched, finished, opened;
    private volatile Throwable failure;
    private ItemStack previous;
    private int lifecycleStage, lifecycleTicks;
    private net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> lifecycleDimension;

    private static final class EquippedDiaryScreen extends InventoryScreen {
        EquippedDiaryScreen() {
            super(Minecraft.getInstance().player);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            super.render(graphics, mouseX, mouseY, partial);
            var mc = Minecraft.getInstance();
            var s =
                    CuriosApi.getCuriosInventory(mc.player)
                            .resolve()
                            .orElseThrow()
                            .getStacksHandler("travel_diary")
                            .orElseThrow()
                            .getStacks()
                            .getStackInSlot(0);
            if (!s.isEmpty()) graphics.renderTooltip(mc.font, s, mouseX, mouseY);
        }
    }

    public ReadingClientSmoke() {
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }

    private static void check(boolean value, String description) {
        if (!value) throw new AssertionError(description);
    }

    private static void field(String name, Object value) throws Exception {
        var f =
                Class.forName("com.thelongtravail.client.diary.DiarySession")
                        .getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    private static Object field(String name) throws Exception {
        var f =
                Class.forName("com.thelongtravail.client.diary.DiarySession")
                        .getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }

    private static ItemStack diary() {
        var s = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        var root = new net.minecraft.nbt.CompoundTag();
        root.putBoolean("Initialized", true);
        root.putUUID("JourneyId", UUID.randomUUID());
        s.getOrCreateTag().put("LongTravail", root);
        return s;
    }

    private static void identities() {
        var mc = Minecraft.getInstance();
        var screen = mc.screen;
        var inv = mc.player.getInventory();
        inv.setItem(4, diary());
        var target =
                new DiaryReadingTarget(DiaryReadingTarget.observe(screen, inv.getItem(4), 10, 10));
        var updated = inv.getItem(4).copy();
        LongTravailData.putLong(updated, "ValleyWitnessCooldown", 200);
        LongTravailData.setWitness(updated, com.thelongtravail.TravailAspect.ABYSS, true);
        updated.setHoverName(net.minecraft.network.chat.Component.literal("renamed"));
        inv.setItem(4, updated);
        var before = updated.save(new net.minecraft.nbt.CompoundTag());
        check(
                target.matches(DiaryReadingTarget.observe(screen, updated, 11, 11)),
                "same slot/journey survives NBT/name/state changes");
        check(
                target.get() == updated
                        && before.equals(updated.save(new net.minecraft.nbt.CompoundTag())),
                "live read without NBT mutation");
        inv.setItem(5, updated.copy());
        check(
                !target.matches(DiaryReadingTarget.observe(screen, inv.getItem(5), 20, 10)),
                "same journey in another slot rejected");
        inv.setItem(4, diary());
        check(target.get().isEmpty(), "new journey rejected");
        inv.setItem(4, ItemStack.EMPTY);
        check(target.get().isEmpty(), "removed diary rejected");
        inv.setItem(4, new ItemStack(ModRegistry.LONG_TRAVAIL.get()));
        var fresh =
                new DiaryReadingTarget(DiaryReadingTarget.observe(screen, inv.getItem(4), 10, 10));
        inv.setItem(4, diary());
        check(
                fresh.matches(DiaryReadingTarget.observe(screen, inv.getItem(4), 10, 10)),
                "first initialization accepted");
        inv.setItem(4, diary());
        check(fresh.get().isEmpty(), "second journey rejected");
        var preview = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        var fallback = new DiaryReadingTarget(DiaryReadingTarget.observe(screen, preview, 30, 30));
        check(
                fallback.matches(DiaryReadingTarget.observe(screen, preview.copy(), 30, 30)),
                "copied preview accepted");
        check(
                !fallback.matches(DiaryReadingTarget.observe(screen, preview.copy(), 40, 30)),
                "preview pointer move rejected");
        var changed = preview.copy();
        changed.getOrCreateTag().putInt("changed", 1);
        check(
                !fallback.matches(DiaryReadingTarget.observe(screen, changed, 30, 30)),
                "unknown uninitialized preview change rejected");
        var h = CuriosApi.getCuriosInventory(mc.player).resolve().orElseThrow();
        var slots = h.getStacksHandler("travel_diary").orElseThrow();
        for (boolean cosmetic : new boolean[] {false, true}) {
            var stacks = cosmetic ? slots.getCosmeticStacks() : slots.getStacks();
            stacks.setStackInSlot(0, diary());
            var t =
                    new DiaryReadingTarget(
                            DiaryReadingTarget.observe(screen, stacks.getStackInSlot(0), 10, 10));
            stacks.setStackInSlot(0, stacks.getStackInSlot(0).copy());
            check(
                    t.matches(DiaryReadingTarget.observe(screen, stacks.getStackInSlot(0), 10, 10)),
                    "Curios equipment/cosmetic copied stack");
            stacks.setStackInSlot(0, ItemStack.EMPTY);
            check(t.get().isEmpty(), "Curios removed item");
        }
        slots.getStacks().setStackInSlot(0, diary());
        var removedSlot =
                new DiaryReadingTarget(
                        DiaryReadingTarget.observe(
                                screen, slots.getStacks().getStackInSlot(0), 10, 10));
        var originalSlots = new java.util.HashMap<>(h.getCurios());
        var without = new java.util.HashMap<>(originalSlots);
        without.remove("travel_diary");
        h.setCurios(without);
        check(removedSlot.get().isEmpty(), "removed Curios slot is safe");
        h.setCurios(originalSlots);
        var chest = new net.minecraft.world.SimpleContainer(27);
        chest.setItem(0, diary());
        var menu = net.minecraft.world.inventory.ChestMenu.threeRows(17, inv, chest);
        var chestScreen =
                new net.minecraft.client.gui.screens.inventory.ContainerScreen(
                        menu, inv, net.minecraft.network.chat.Component.literal("test"));
        var chestTarget =
                new DiaryReadingTarget(
                        DiaryReadingTarget.observe(chestScreen, chest.getItem(0), 10, 10));
        chest.setItem(0, chest.getItem(0).copy());
        check(
                chestTarget.matches(
                        DiaryReadingTarget.observe(chestScreen, chest.getItem(0), 10, 10)),
                "container copied stack");
        chest.setItem(0, diary());
        check(chestTarget.get().isEmpty(), "container replacement rejected");
        inv.setItem(4, ItemStack.EMPTY);
        inv.setItem(5, ItemStack.EMPTY);
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

    private void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        GLFW.glfwHideWindow(mc.getWindow().getWindow());
        try {
            if (failure != null) throw new AssertionError("server action", failure);
            if (lifecycleStage > 0) {
                lifecycle();
                return;
            }
            if (mc.player == null) {
                if (++idle > 1600) throw new AssertionError("world timeout");
                if (!launched && mc.getOverlay() == null && idle > 40) {
                    launched = true;
                    mc.createWorldOpenFlows()
                            .loadLevel(
                                    new net.minecraft.client.gui.screens.TitleScreen(),
                                    "visual-world");
                }
                return;
            }
            ticks++;
            if (ticks == 20) {
                mc.setScreen(new EquippedDiaryScreen());
                identities();
                server(
                        p -> {
                            var s = diary();
                            for (var a : com.thelongtravail.TravailAspect.values())
                                LongTravailData.setWitness(s, a, true);
                            CuriosApi.getCuriosInventory(p)
                                    .resolve()
                                    .orElseThrow()
                                    .getStacksHandler("travel_diary")
                                    .orElseThrow()
                                    .getStacks()
                                    .setStackInSlot(0, s);
                        });
            }
            if (ticks >= 50 && ticks <= 130) {
                mc.setWindowActive(true);
                var s =
                        CuriosApi.getCuriosInventory(mc.player)
                                .resolve()
                                .orElseThrow()
                                .getStacksHandler("travel_diary")
                                .orElseThrow()
                                .getStacks()
                                .getStackInSlot(0);
                check(!s.isEmpty(), "server equipment arrived");
                if (previous != null && previous != s) replacements++;
                previous = s;
                if (ticks % 3 == 0)
                    server(
                            p ->
                                    LongTravailData.putLong(
                                            CuriosApi.getCuriosInventory(p)
                                                    .resolve()
                                                    .orElseThrow()
                                                    .getStacksHandler("travel_diary")
                                                    .orElseThrow()
                                                    .getStacks()
                                                    .getStackInSlot(0),
                                            "ValleyWitnessCooldown",
                                            p.level().getGameTime() + 60));
                if (!TravailClientEvents.isReading()) {
                    var x =
                            mc.mouseHandler.xpos()
                                    * mc.getWindow().getGuiScaledWidth()
                                    / mc.getWindow().getScreenWidth();
                    var y =
                            mc.mouseHandler.ypos()
                                    * mc.getWindow().getGuiScaledHeight()
                                    / mc.getWindow().getScreenHeight();
                    field("hovered", s);
                    field("hoveredScreen", mc.screen);
                    field("hoverX", x);
                    field("hoverY", y);
                    field("hoverTime", System.nanoTime());
                    field("hoveredTarget", DiaryReadingTarget.observe(mc.screen, s, x, y));
                    if (ticks == 50)
                        TravailClientEvents.ForgeBus.keyPressed(
                                new ScreenEvent.KeyPressed.Pre(
                                        mc.screen, GLFW.GLFW_KEY_LEFT_SHIFT, 0, 0));
                }
                opened |= TravailClientEvents.isReading();
            }
            if (ticks == 135) {
                check(
                        opened && TravailClientEvents.isReading(),
                        "one Shift press opens and remains open through sync");
                check(replacements >= 5, "real Curios packets replace stacks repeatedly");
                try (var screenshot =
                        net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    screenshot.writeToFile(Path.of("reading-overlay.png"));
                }
                var reader = (TravelDiaryScreen) field("reading");
                reader.onClose();
                check(!TravailClientEvents.isReading(), "reader closes normally");
                closePaths();
                inputEdges();
                DiaryRoutingChecks.run();
                mc.setScreen(new InventoryScreen(mc.player));
                field("owner", mc.screen);
                field("reading", new TravelDiaryScreen(ReadingClientSmoke::diary, () -> {}));
                lifecycleStage = 1;
                lifecycleDimension =
                        mc.player.level().dimension() == net.minecraft.world.level.Level.NETHER
                                ? net.minecraft.world.level.Level.OVERWORLD
                                : net.minecraft.world.level.Level.NETHER;
                server(
                        player -> {
                            player.setInvulnerable(true);
                            player.teleportTo(
                                    player.server.getLevel(lifecycleDimension), 0.5, 80, 0.5, 0, 0);
                        });
            }
        } catch (Throwable error) {
            error.printStackTrace();
            finish(false, error.toString());
        }
    }

    private void lifecycle() throws Exception {
        var mc = Minecraft.getInstance();
        if (++lifecycleTicks > 1600)
            throw new AssertionError("lifecycle timeout stage=" + lifecycleStage);
        if (lifecycleStage == 1) {
            // 传送完成会重新建立玩家所在世界并关闭原界面。
            if (mc.player == null
                    || lifecycleTicks < 80
                    || mc.player.level().dimension() != lifecycleDimension) return;
            check(
                    !TravailClientEvents.isReading() && field("owner") == null,
                    "real dimension change closes reader");
            check(field("holdTarget") == null, "dimension change clears hold");
            System.out.println(
                    "DIARY_DIMENSION_PASS dimension=" + mc.player.level().dimension().location());
            mc.level.disconnect();
            mc.clearLevel();
            mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
            lifecycleStage = 2;
            lifecycleTicks = 0;
        } else if (lifecycleStage == 2) {
            if (mc.getSingleplayerServer() != null || lifecycleTicks < 20) return;
            check(
                    !TravailClientEvents.isReading() && field("holdTarget") == null,
                    "real disconnect clears session");
            mc.createWorldOpenFlows()
                    .loadLevel(new net.minecraft.client.gui.screens.TitleScreen(), "visual-world");
            lifecycleStage = 3;
            lifecycleTicks = 0;
        } else if (mc.player != null && lifecycleTicks > 30) {
            check(!TravailClientEvents.isReading(), "reconnect starts without old reader");
            DiaryRoutingChecks.run();
            System.out.println("DIARY_RECONNECT_PASS");
            finish(
                    true,
                    "identity/slot/preview, input/release/focus, dimension/reconnect, live Curios replacements="
                            + replacements);
        }
    }

    private static void closePaths() throws Exception {
        var mc = Minecraft.getInstance();
        var owner = mc.screen;
        var close =
                Class.forName("com.thelongtravail.client.diary.DiarySession")
                        .getDeclaredMethod("close");
        close.setAccessible(true);
        var layout = TravelDiaryScreen.class.getDeclaredMethod("updateLayout");
        layout.setAccessible(true);
        var left = TravelDiaryScreen.class.getDeclaredField("bookLeft");
        var top = TravelDiaryScreen.class.getDeclaredField("bookTop");
        var scale = TravelDiaryScreen.class.getDeclaredField("bookScale");
        left.setAccessible(true);
        top.setAccessible(true);
        scale.setAccessible(true);
        var mousePress =
                net.minecraft.client.MouseHandler.class.getDeclaredMethod(
                        "onPress", long.class, int.class, int.class, int.class);
        var mouseX = net.minecraft.client.MouseHandler.class.getDeclaredField("xpos");
        var mouseY = net.minecraft.client.MouseHandler.class.getDeclaredField("ypos");
        mousePress.setAccessible(true);
        mouseX.setAccessible(true);
        mouseY.setAccessible(true);
        int count = 0;
        for (int[] size :
                new int[][] {{320, 180}, {480, 270}, {640, 360}, {960, 540}, {1920, 1080}}) {
            for (int path = 0; path < 4; path++) {
                var reader =
                        new TravelDiaryScreen(
                                ReadingClientSmoke::diary,
                                () -> {
                                    try {
                                        close.invoke(null);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                reader.init(mc, size[0], size[1]);
                layout.invoke(reader);
                field("owner", owner);
                field("reading", reader);
                if (path == 0) {
                    double x = left.getInt(reader) + 482 * scale.getFloat(reader);
                    double y = top.getInt(reader) + 55 * scale.getFloat(reader);
                    var click =
                            new ScreenEvent.MouseButtonPressed.Pre(
                                    owner, x, y, GLFW.GLFW_MOUSE_BUTTON_LEFT);
                    MinecraftForge.EVENT_BUS.post(click);
                    check(click.isCanceled(), "X click consumed by reading overlay");
                    MinecraftForge.EVENT_BUS.post(
                            new ScreenEvent.MouseButtonReleased.Pre(
                                    owner, x, y, GLFW.GLFW_MOUSE_BUTTON_LEFT));
                } else if (path == 1) {
                    MinecraftForge.EVENT_BUS.post(
                            new ScreenEvent.MouseButtonPressed.Pre(
                                    owner, 0, 0, GLFW.GLFW_MOUSE_BUTTON_RIGHT));
                    MinecraftForge.EVENT_BUS.post(
                            new ScreenEvent.MouseButtonReleased.Pre(
                                    owner, 0, 0, GLFW.GLFW_MOUSE_BUTTON_RIGHT));
                } else if (path == 2) {
                    MinecraftForge.EVENT_BUS.post(
                            new ScreenEvent.KeyReleased.Pre(owner, GLFW.GLFW_KEY_ESCAPE, 0, 0));
                    MinecraftForge.EVENT_BUS.post(
                            new ScreenEvent.KeyPressed.Pre(owner, GLFW.GLFW_KEY_ESCAPE, 0, 0));
                    MinecraftForge.EVENT_BUS.post(
                            new ScreenEvent.KeyReleased.Pre(owner, GLFW.GLFW_KEY_ESCAPE, 0, 0));
                } else {
                    double x = left.getInt(reader) + 482 * scale.getFloat(reader);
                    double y = top.getInt(reader) + 55 * scale.getFloat(reader);
                    mouseX.setDouble(
                            mc.mouseHandler,
                            x
                                    * mc.getWindow().getScreenWidth()
                                    / mc.getWindow().getGuiScaledWidth());
                    mouseY.setDouble(
                            mc.mouseHandler,
                            y
                                    * mc.getWindow().getScreenHeight()
                                    / mc.getWindow().getGuiScaledHeight());
                    mc.setWindowActive(true);
                    mousePress.invoke(
                            mc.mouseHandler,
                            mc.getWindow().getWindow(),
                            GLFW.GLFW_MOUSE_BUTTON_LEFT,
                            GLFW.GLFW_PRESS,
                            0);
                    mousePress.invoke(
                            mc.mouseHandler,
                            mc.getWindow().getWindow(),
                            GLFW.GLFW_MOUSE_BUTTON_LEFT,
                            GLFW.GLFW_RELEASE,
                            0);
                }
                check(
                        !TravailClientEvents.isReading(),
                        "close path=" + path + " size=" + size[0] + "x" + size[1]);
                check(mc.screen == owner, "reading close preserves owner inventory");
                count++;
            }
        }
        System.out.println("DIARY_CLOSE_PATHS_PASS cases=" + count);
    }

    private static void inputEdges() throws Exception {
        var mc = Minecraft.getInstance();
        var screen = mc.screen;
        var key = TravailClientEvents.OPEN_DIARY;
        var original = key.getKey();
        TravailClientEvents.ForgeBus.keyReleased(
                new ScreenEvent.KeyReleased.Pre(screen, GLFW.GLFW_KEY_LEFT_SHIFT, 0, 0));
        var s =
                CuriosApi.getCuriosInventory(mc.player)
                        .resolve()
                        .orElseThrow()
                        .getStacksHandler("travel_diary")
                        .orElseThrow()
                        .getStacks()
                        .getStackInSlot(0);
        double x =
                mc.mouseHandler.xpos()
                        * mc.getWindow().getGuiScaledWidth()
                        / mc.getWindow().getScreenWidth();
        double y =
                mc.mouseHandler.ypos()
                        * mc.getWindow().getGuiScaledHeight()
                        / mc.getWindow().getScreenHeight();
        field("hovered", s);
        field("hoveredScreen", screen);
        field("hoverX", x);
        field("hoverY", y);
        field("hoverTime", System.nanoTime());
        field("hoveredTarget", DiaryReadingTarget.observe(screen, s, x, y));
        try {
            key.setKey(
                    com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(
                            GLFW.GLFW_KEY_G));
            TravailClientEvents.ForgeBus.keyPressed(
                    new ScreenEvent.KeyPressed.Pre(screen, GLFW.GLFW_KEY_G, 0, 0));
            check(
                    ((java.util.Set<?>) field("diaryInputs")).contains(key.getKey()),
                    "rebound keyboard starts hold");
            var progress = (DiaryEntryHold) field("holdProgress");
            progress.advance(.7, true, false, true);
            double before = progress.fraction();
            TravailClientEvents.ForgeBus.keyReleased(
                    new ScreenEvent.KeyReleased.Pre(screen, GLFW.GLFW_KEY_G, 0, 0));
            field("holdFrame", System.nanoTime() - 50_000_000L);
            var update =
                    Class.forName("com.thelongtravail.client.diary.DiarySession")
                            .getDeclaredMethod("updateHold", ScreenEvent.Render.Post.class);
            update.setAccessible(true);
            update.invoke(
                    null,
                    new ScreenEvent.Render.Post(
                            screen,
                            new GuiGraphics(mc, mc.renderBuffers().bufferSource()),
                            (int) x,
                            (int) y,
                            0));
            check(
                    progress.fraction() < before && progress.fraction() > 0,
                    "release retains gradual decay");
            key.setKey(com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(3));
            TravailClientEvents.ForgeBus.mousePressed(
                    new ScreenEvent.MouseButtonPressed.Pre(screen, x, y, 3));
            check(
                    ((java.util.Set<?>) field("diaryInputs")).contains(key.getKey()),
                    "rebound mouse starts hold");
            mc.setWindowActive(false);
            TravailClientEvents.ForgeBus.render(
                    new ScreenEvent.Render.Pre(
                            screen,
                            new GuiGraphics(mc, mc.renderBuffers().bufferSource()),
                            (int) x,
                            (int) y,
                            0));
            check(
                    field("holdTarget") == null && progress.fraction() == 0,
                    "focus loss cancels hold");
            mc.setWindowActive(true);
            mc.setScreen(null);
            check(field("holdTarget") == null, "screen close clears hold");
        } finally {
            key.setKey(original);
        }
    }

    private void finish(boolean pass, String description) {
        finished = true;
        System.out.println("READING_CLIENT_" + (pass ? "PASS" : "FAIL") + ": " + description);
        try {
            Files.writeString(
                    Path.of("reading-result.txt"), pass ? "PASS" : "FAIL: " + description);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Minecraft.getInstance().stop();
        }
    }
}

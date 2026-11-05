package travail.readingtest;

import com.thelongtravail.client.diary.DiaryEntryHold;
import com.thelongtravail.client.diary.DiaryInputRouter;
import com.thelongtravail.client.diary.DiaryReadingTarget;
import com.thelongtravail.client.diary.DiarySession;
import com.thelongtravail.client.diary.TravelDiaryScreen;

import com.mojang.blaze3d.platform.InputConstants;
import com.thelongtravail.client.*;
import com.thelongtravail.client.catalog.CatalogSession;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.glfw.GLFW;

import java.util.Set;

// 在真实客户端事件总线上检查输入消费与会话清理。
public final class DiaryRoutingChecks {
    private static int checks;

    private static Class<?> sessionType() throws Exception {
        return Class.forName("com.thelongtravail.client.diary.DiarySession");
    }

    private static Class<?> inputType() throws Exception {
        return Class.forName("com.thelongtravail.client.diary.DiaryInputRouter");
    }

    private static void resetHold() throws Exception {
        var method = sessionType().getDeclaredMethod("resetHold");
        method.setAccessible(true);
        method.invoke(null);
    }

    private static final class EditingScreen extends Screen {
        EditBox input;

        EditingScreen() {
            super(Component.literal("Diary routing"));
        }

        @Override
        protected void init() {
            input = addRenderableWidget(new EditBox(font, 5, 5, 100, 20, Component.empty()));
            setFocused(input);
            input.setFocused(true);
        }
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }

    private static Object state(Class<?> owner, String name) throws Exception {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static void state(String name, Object value) throws Exception {
        var field = sessionType().getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static void hover(Screen screen) throws Exception {
        var mc = Minecraft.getInstance();
        double x =
                mc.mouseHandler.xpos()
                        * mc.getWindow().getGuiScaledWidth()
                        / mc.getWindow().getScreenWidth();
        double y =
                mc.mouseHandler.ypos()
                        * mc.getWindow().getGuiScaledHeight()
                        / mc.getWindow().getScreenHeight();
        var stack = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        state("hovered", stack);
        state("hoveredScreen", screen);
        state("hoverX", x);
        state("hoverY", y);
        state("hoverTime", System.nanoTime());
        state("hoveredTarget", DiaryReadingTarget.observe(screen, stack, x, y));
    }

    private static ScreenEvent.KeyPressed.Pre press(Screen screen) {
        var event = new ScreenEvent.KeyPressed.Pre(screen, GLFW.GLFW_KEY_G, 0, 0);
        MinecraftForge.EVENT_BUS.post(event);
        return event;
    }

    private static void released(Screen screen, boolean canceled) {
        var event = new ScreenEvent.KeyReleased.Pre(screen, GLFW.GLFW_KEY_G, 0, 0);
        event.setCanceled(canceled);
        MinecraftForge.EVENT_BUS.post(event);
    }

    private static void emptyState() throws Exception {
        check(!TravailClientEvents.isReading(), "reading cleared");
        for (String name :
                new String[] {
                    "owner", "hoveredScreen", "hoveredTarget", "holdTarget", "holdScreen"
                }) check(state(sessionType(), name) == null, "session cleared: " + name);
        for (String name : new String[] {"diaryInputs", "catalogInputs"})
            check(((Set<?>) state(sessionType(), name)).isEmpty(), "entry cleared: " + name);
        for (String name : new String[] {"heldKeys", "consumedKeys", "consumedButtons"})
            check(((Set<?>) state(inputType(), name)).isEmpty(), "input cleared: " + name);
    }

    public static void run() throws Exception {
        var mc = Minecraft.getInstance();
        var diaryKey = TravailClientEvents.OPEN_DIARY;
        var catalogKey = CatalogSession.OPEN;
        var originalDiary = diaryKey.getKey();
        var originalCatalog = catalogKey.getKey();
        var key = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_G);
        try {
            mc.setWindowActive(true);
            var screen = new EditingScreen();
            mc.setScreen(screen);
            diaryKey.setKey(key);
            hover(screen);
            check(!press(screen).isCanceled(), "focused EditBox retains key input");
            check(state(sessionType(), "holdTarget") == null, "editing cannot start hold");
            released(screen, false);
            screen.input.setFocused(false);
            screen.setFocused(null);
            hover(screen);
            check(press(screen).isCanceled(), "entry press consumed");
            Object target = state(sessionType(), "holdTarget");
            check(press(screen).isCanceled(), "repeat press consumed");
            check(target == state(sessionType(), "holdTarget"), "repeat preserves hold identity");
            var character = new ScreenEvent.CharacterTyped.Pre(screen, 'g', 0);
            MinecraftForge.EVENT_BUS.post(character);
            check(character.isCanceled(), "consumed key suppresses character input");
            released(screen, true);
            check(
                    ((Set<?>) state(sessionType(), "diaryInputs")).isEmpty(),
                    "canceled key release clears entry");
            check(
                    ((Set<?>) state(inputType(), "consumedKeys")).isEmpty(),
                    "canceled key release clears consumption");

            resetHold();
            hover(screen);
            state("hoverTime", System.nanoTime() - 300_000_000L);
            check(!press(screen).isCanceled(), "stale hover cannot start hold");
            released(screen, false);
            hover(screen);
            state("hoverX", (double) state(sessionType(), "hoverX") + 2);
            check(!press(screen).isCanceled(), "moved cursor cannot start hold");
            released(screen, false);

            catalogKey.setKey(key);
            hover(screen);
            press(screen);
            check(
                    ((Set<?>) state(sessionType(), "diaryInputs")).contains(key),
                    "shared key arms diary");
            check(
                    ((Set<?>) state(sessionType(), "catalogInputs")).contains(key),
                    "shared key arms catalog");
            var progress = (DiaryEntryHold) state(sessionType(), "holdProgress");
            check(
                    progress.advance(2, true, true, true) == DiaryEntryHold.Entry.DIARY,
                    "shared key keeps diary priority");
            released(screen, false);
            resetHold();

            CatalogSession.open(screen);
            var catalogInput = new ScreenEvent.KeyPressed.Pre(screen, GLFW.GLFW_KEY_G, 0, 0);
            TravailClientEvents.ForgeBus.keyPressed(catalogInput);
            check(!catalogInput.isCanceled(), "diary router yields to active catalog");
            check(state(sessionType(), "holdTarget") == null, "catalog cannot start diary hold");
            mc.setScreen(null);
            emptyState();

            mc.setScreen(screen);
            screen.input.setFocused(false);
            screen.setFocused(null);
            diaryKey.setKey(InputConstants.Type.MOUSE.getOrCreate(3));
            hover(screen);
            var click = new ScreenEvent.MouseButtonPressed.Pre(screen, 0, 0, 3);
            MinecraftForge.EVENT_BUS.post(click);
            check(click.isCanceled(), "mouse entry consumed");
            var release = new ScreenEvent.MouseButtonReleased.Pre(screen, 0, 0, 3);
            release.setCanceled(true);
            MinecraftForge.EVENT_BUS.post(release);
            check(
                    ((Set<?>) state(inputType(), "consumedButtons")).isEmpty(),
                    "canceled mouse release clears consumption");
            check(
                    ((Set<?>) state(sessionType(), "diaryInputs")).isEmpty(),
                    "canceled mouse release clears entry");

            diaryKey.setKey(key);
            hover(screen);
            press(screen);
            state("owner", screen);
            state("reading", new TravelDiaryScreen(() -> ItemStack.EMPTY, () -> {}));
            TravailClientEvents.ForgeBus.logout(
                    new ClientPlayerNetworkEvent.LoggingOut(
                            mc.gameMode, mc.player, mc.getConnection().getConnection()));
            emptyState();
            hover(screen);
            check(press(screen).isCanceled(), "new session accepts key after logout cleanup");
            mc.setScreen(new EditingScreen());
            emptyState();
            check(
                    com.thelongtravail.network.PrinceFlightPacket.receiver != null
                            && com.thelongtravail.network.ExecutionTerminationPacket.receiver
                                    != null
                            && com.thelongtravail.network.LanternPacket.receiver != null
                            && com.thelongtravail.network.EffectNotice.receiver != null
                            && com.thelongtravail.network.ItemSoundCue.receiver != null
                            && com.thelongtravail.network.StiffPacket.receiver != null
                            && com.thelongtravail.network.VisualDeprivationPacket.receiver != null,
                    "client setup receiver bindings");
            System.out.println("DIARY_ROUTING_PASS checks=" + checks);
        } finally {
            diaryKey.setKey(originalDiary);
            catalogKey.setKey(originalCatalog);
            mc.setScreen(null);
        }
    }
}

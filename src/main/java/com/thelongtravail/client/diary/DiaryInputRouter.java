package com.thelongtravail.client.diary;

import com.mojang.blaze3d.platform.InputConstants;
import com.thelongtravail.client.TravailClientEvents;
import com.thelongtravail.client.catalog.CatalogSession;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyModifier;

import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

// 保存按下与已消费输入，按宿主会话分发键鼠事件并拦截对应释放事件。
public final class DiaryInputRouter {
    private static final Set<Integer> heldKeys = new HashSet<>();
    private static final Set<Integer> consumedKeys = new HashSet<>();
    private static final Set<Integer> consumedButtons = new HashSet<>();

    public static void keyPressed(ScreenEvent.KeyPressed.Pre event) {
        boolean firstPress = heldKeys.add(event.getKeyCode());
        if (CatalogSession.active() != null) return;
        if (DiarySession.owns(event.getScreen())) {
            event.setCanceled(true);
            consumedKeys.add(event.getKeyCode());
            if (firstPress)
                DiarySession.keyPressed(
                        event.getKeyCode(), event.getScanCode(), event.getModifiers());
        } else if (consumedKeys.contains(event.getKeyCode())) {
            event.setCanceled(true);
        } else if (firstPress
                && DiarySession.canRead(event.getScreen())
                && !editing(event.getScreen())) {
            InputConstants.Key key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
            boolean diary =
                    DiarySession.hoveredDiary()
                            && matchesEntryKey(TravailClientEvents.OPEN_DIARY, key);
            boolean catalog = matchesEntryKey(CatalogSession.OPEN, key);
            if (diary || catalog) {
                DiarySession.beginHold(event.getScreen(), key, diary, catalog);
                consumedKeys.add(event.getKeyCode());
                event.setCanceled(true);
            }
        }
    }

    public static void keyReleased(ScreenEvent.KeyReleased.Pre event) {
        InputConstants.Key key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
        DiarySession.releaseEntry(key);
        heldKeys.remove(event.getKeyCode());
        if (consumedKeys.remove(event.getKeyCode()) || DiarySession.isReading())
            event.setCanceled(true);
    }

    public static void character(ScreenEvent.CharacterTyped.Pre event) {
        if (com.thelongtravail.client.catalog.CatalogSession.active() != null) return;
        if (DiarySession.isReading() || !consumedKeys.isEmpty()) event.setCanceled(true);
    }

    public static void mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (DiarySession.owns(event.getScreen())) {
            consumedButtons.add(event.getButton());
            event.setCanceled(true);
            DiarySession.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton());
        } else if (DiarySession.canRead(event.getScreen()) && !editing(event.getScreen())) {
            boolean diary =
                    DiarySession.hoveredDiary()
                            && TravailClientEvents.OPEN_DIARY.matchesMouse(event.getButton());
            boolean catalog = CatalogSession.OPEN.matchesMouse(event.getButton());
            if (diary || catalog) {
                consumedButtons.add(event.getButton());
                event.setCanceled(true);
                DiarySession.beginHold(
                        event.getScreen(),
                        InputConstants.Type.MOUSE.getOrCreate(event.getButton()),
                        diary,
                        catalog);
            }
        }
    }

    public static void mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
        DiarySession.releaseEntry(key);
        if (consumedButtons.remove(event.getButton()) || DiarySession.isReading())
            event.setCanceled(true);
    }

    public static void mouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (DiarySession.isReading() || consumedButtons.contains(event.getMouseButton()))
            event.setCanceled(true);
    }

    public static void scroll(ScreenEvent.MouseScrolled.Pre event) {
        if (DiarySession.owns(event.getScreen())) {
            event.setCanceled(true);
            DiarySession.mouseScrolled(
                    event.getMouseX(), event.getMouseY(), event.getScrollDelta());
        }
    }

    public static Component holdKeyLabel(KeyMapping binding) {
        if (binding.getKeyModifier() == KeyModifier.NONE) {
            if (binding.getKey()
                    .equals(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_SHIFT)))
                return Component.literal("Shift");
            if (binding.getKey()
                    .equals(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_ALT)))
                return Component.literal("Alt");
        }
        return binding.getTranslatedKeyMessage();
    }

    static boolean matchesEntryKey(KeyMapping binding, InputConstants.Key key) {
        if (binding.isUnbound()) return false;
        if (binding.getKeyModifier() == KeyModifier.NONE
                && KeyModifier.isKeyCodeModifier(binding.getKey()))
            return key.equals(binding.getKey()) && binding.getKeyConflictContext().isActive();
        return binding.isActiveAndMatches(key);
    }

    static boolean editing(GuiEventListener listener) {
        if (listener instanceof EditBox box && box.isFocused()) return true;
        return listener instanceof ContainerEventHandler container
                && container.getFocused() != null
                && editing(container.getFocused());
    }

    public static void clear() {
        heldKeys.clear();
        consumedKeys.clear();
        consumedButtons.clear();
    }

    private DiaryInputRouter() {}
}

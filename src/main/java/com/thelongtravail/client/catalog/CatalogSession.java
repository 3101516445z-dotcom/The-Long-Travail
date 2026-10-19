package com.thelongtravail.client.catalog;

import com.mojang.blaze3d.platform.InputConstants;
import com.thelongtravail.TheLongTravail;
import com.thelongtravail.client.TravailClientEvents;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID, value = Dist.CLIENT)
public final class CatalogSession {
    public static final KeyMapping OPEN =
            new KeyMapping(
                    "key.the_long_travail.open_catalog",
                    KeyConflictContext.GUI,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_LEFT_ALT,
                    "key.categories.the_long_travail");
    private static CatalogSession active;
    private static final Set<Integer> consumedKeys = new HashSet<>(), consumedButtons = new HashSet<>();
    private final Screen owner;
    private CatalogScreen parent;
    private Screen page;
    private float scale = 1;

    private CatalogSession(Screen owner) {
        this.owner = owner;
    }

    public static CatalogSession active() {
        return active;
    }

    public Screen page() {
        return page;
    }

    public CatalogScreen parent() {
        return parent;
    }

    public static CatalogSession open(Screen owner) {
        active = new CatalogSession(owner);
        active.back();
        return active;
    }

    public static CatalogSession openDetail(Screen owner, CatalogEntries.Entry entry, ItemStack actual) {
        active = new CatalogSession(owner);
        active.detail(entry, actual);
        return active;
    }

    private void show(Screen screen) {
        page = screen;
        Minecraft mc = Minecraft.getInstance();
        layout(mc);
    }

    private void layout(Minecraft mc) {
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        float previousScale = scale;
        scale = 0.8f * Math.max(0.1f, Math.min(1.25f, Math.min((w - 12f) / 524f, (h - 12f) / 360f)));
        int lw = (int) (w / scale), lh = (int) (h / scale);
        if (page.width != lw || page.height != lh || previousScale != scale) page.init(mc, lw, lh);
    }

    float bodyTextScale() {
        var window = Minecraft.getInstance().getWindow();
        float originalScale =
                Math.min(1, Math.min(window.getGuiScaledWidth() / 420f, window.getGuiScaledHeight() / 260f));
        return originalScale / scale;
    }

    void scissor(net.minecraft.client.gui.GuiGraphics g, int x1, int y1, int x2, int y2) {
        g.enableScissor(
                (int) (x1 * scale), (int) (y1 * scale), (int) Math.ceil(x2 * scale), (int) Math.ceil(y2 * scale));
    }

    public void detail(CatalogEntries.Entry entry, ItemStack stack) {
        if (parent != null) parent.collapseSearch();
        ItemStack display = entry.stack();
        show(new CatalogDetailScreen(this, display, CatalogEntries.details(entry, display)));
    }

    public void back() {
        if (parent == null) parent = new CatalogScreen(this);
        parent.collapseSearch();
        show(parent);
    }

    public void close() {
        if (active == this) active = null;
    }

    @Mod.EventBusSubscriber(modid = TheLongTravail.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) {
            event.register(OPEN);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tooltip(ItemTooltipEvent event) {
        boolean diary = event.getItemStack().is(ModRegistry.LONG_TRAVAIL.get());
        if (!diary) return;
        Component prompt =
                Component.translatable(
                                "tooltip.the_long_travail.open_catalog",
                                com.thelongtravail.client.ThemeTooltips.keyName(OPEN)
                                        .copy()
                                        .withStyle(
                                                s -> s.withColor(com.thelongtravail.abyss.RainTooltips.ACCENT_COLOR)))
                        .withStyle(s -> s.withColor(com.thelongtravail.abyss.RainTooltips.BODY_COLOR));
        var lines = event.getToolTip();
        int at = lines.size();
        for (int i = 0; i < lines.size(); i++)
            if (lines.get(i).getContents() instanceof TranslatableContents t
                    && t.getKey().startsWith("tooltip.the_long_travail.open_diary")) {
                at = i + 1;
                break;
            }
        lines.add(at, prompt);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void renderPre(ScreenEvent.Render.Pre event) {
        if (active == null) return;
        if (active.owner != event.getScreen()) {
            active.close();
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderPost(ScreenEvent.Render.Post event) {
        if (active == null || active.owner != event.getScreen()) return;
        var s = active;
        Minecraft mc = Minecraft.getInstance();
        s.layout(mc);
        var g = event.getGuiGraphics();
        g.pose().pushPose();
        g.pose().translate(0, 0, 600);
        g.pose().scale(s.scale, s.scale, 1);
        try {
            s.page.render(
                    g,
                    (int) (event.getMouseX() / s.scale),
                    (int) (event.getMouseY() / s.scale),
                    event.getPartialTick());
        } finally {
            g.pose().popPose();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void key(ScreenEvent.KeyPressed.Pre event) {
        if (active != null) {
            event.setCanceled(true);
            boolean firstPress = consumedKeys.add(event.getKeyCode());
            if (event.getKeyCode() != GLFW.GLFW_KEY_ESCAPE || firstPress)
                active.page.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers());
            return;
        }
        if (consumedKeys.contains(event.getKeyCode())) {
            event.setCanceled(true);
            return;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void release(ScreenEvent.KeyReleased.Pre event) {
        if (consumedKeys.remove(event.getKeyCode()) || active != null) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void character(ScreenEvent.CharacterTyped.Pre event) {
        if (active != null) {
            event.setCanceled(true);
            active.page.charTyped(event.getCodePoint(), event.getModifiers());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        if (active != null) {
            event.setCanceled(true);
            consumedButtons.add(event.getButton());
            active.page.mouseClicked(
                    event.getMouseX() / active.scale, event.getMouseY() / active.scale, event.getButton());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void releaseMouse(ScreenEvent.MouseButtonReleased.Pre event) {
        if (active != null) {
            event.setCanceled(true);
            active.page.mouseReleased(
                    event.getMouseX() / active.scale, event.getMouseY() / active.scale, event.getButton());
        }
        if (consumedButtons.remove(event.getButton())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void drag(ScreenEvent.MouseDragged.Pre event) {
        if (active != null) {
            event.setCanceled(true);
            active.page.mouseDragged(
                    event.getMouseX() / active.scale,
                    event.getMouseY() / active.scale,
                    event.getMouseButton(),
                    event.getDragX() / active.scale,
                    event.getDragY() / active.scale);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void wheel(ScreenEvent.MouseScrolled.Pre event) {
        if (active != null) {
            event.setCanceled(true);
            active.page.mouseScrolled(
                    event.getMouseX() / active.scale, event.getMouseY() / active.scale, event.getScrollDelta());
        }
    }

    @SubscribeEvent
    public static void closed(ScreenEvent.Closing event) {
        if (active != null && active.owner == event.getScreen()) active.close();
        consumedKeys.clear();
        consumedButtons.clear();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && active != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != active.owner) {
                active.close();
                return;
            }
            if (!mc.isWindowActive()) {
                consumedKeys.clear();
                consumedButtons.clear();
            }
            active.page.tick();
        }
    }
}

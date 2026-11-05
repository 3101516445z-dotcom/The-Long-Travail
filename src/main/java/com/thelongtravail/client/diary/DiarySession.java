package com.thelongtravail.client.diary;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Either;
import com.thelongtravail.client.ThemeTooltips;
import com.thelongtravail.client.TravailClientEvents;
import com.thelongtravail.client.catalog.CatalogSession;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import java.util.HashSet;
import java.util.Set;

// 日记覆盖层、悬停目标与长按进度的状态所有者，宿主 Screen 保持在 Minecraft.screen。
public final class DiarySession {
    private static Screen owner;
    private static TravelDiaryScreen reading;
    private static boolean renderingReading;
    private static Screen hoveredScreen;
    private static ItemStack hovered = ItemStack.EMPTY;
    private static double hoverX, hoverY;
    private static long hoverTime;
    private static final DiaryEntryHold holdProgress = new DiaryEntryHold();
    private static Screen holdScreen;
    private static DiaryReadingTarget holdTarget;
    private static DiaryReadingTarget.Observation hoveredTarget;
    private static final Set<InputConstants.Key> diaryInputs = new HashSet<>();
    private static final Set<InputConstants.Key> catalogInputs = new HashSet<>();
    private static long holdFrame;

    public static void tooltipText(ItemTooltipEvent event) {
        if (event.getItemStack().is(ModRegistry.LONG_TRAVAIL.get()))
            DiaryDialogue.decorate(
                    event.getToolTip(), "tooltip.the_long_travail.dialogue.", 10, true);

        if (!event.getItemStack().is(ModRegistry.LONG_TRAVAIL.get())) return;
        event.getToolTip()
                .replaceAll(
                        line -> {
                            if (!(line.getContents() instanceof TranslatableContents contents)
                                    || !contents.getKey()
                                            .equals("tooltip.the_long_travail.open_diary"))
                                return line;
                            return Component.translatable(
                                            "tooltip.the_long_travail.open_diary",
                                            ThemeTooltips.keyName(TravailClientEvents.OPEN_DIARY)
                                                    .copy()
                                                    .withStyle(
                                                            style ->
                                                                    style.withColor(
                                                                            com.thelongtravail.abyss
                                                                                    .rain
                                                                                    .RainTooltips
                                                                                    .ACCENT_COLOR)))
                                    .setStyle(line.getStyle());
                        });
    }

    public static void render(ScreenEvent.Render.Pre event) {
        if (!Minecraft.getInstance().isWindowActive()) {
            resetHold();
            DiaryInputRouter.clear();
        }
        hovered = ItemStack.EMPTY;
        hoveredTarget = null;
        hoveredScreen = null;
        if (reading == null) return;
        if (owner != event.getScreen()) {
            close();
            return;
        }
        event.setCanceled(true);
    }

    public static void renderReadingLayer(ScreenEvent.Render.Post event) {
        if (reading == null) updateHold(event);
        if (reading == null || owner != event.getScreen()) return;
        Minecraft mc = Minecraft.getInstance();
        if (reading.width != owner.width || reading.height != owner.height)
            reading.init(mc, owner.width, owner.height);
        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().translate(0, 0, 600);
        renderingReading = true;
        try {
            reading.render(
                    event.getGuiGraphics(),
                    event.getMouseX(),
                    event.getMouseY(),
                    event.getPartialTick());
        } finally {
            renderingReading = false;
            event.getGuiGraphics().pose().popPose();
        }
    }

    public static void tooltip(RenderTooltipEvent.GatherComponents event) {
        Minecraft mc = Minecraft.getInstance();
        if (reading != null || mc.screen == null) return;
        hovered =
                (event.getItemStack().is(ModRegistry.LONG_TRAVAIL.get())
                                || com.thelongtravail.client.catalog.CatalogEntries.forStack(
                                                event.getItemStack())
                                        .isPresent())
                        ? event.getItemStack()
                        : ItemStack.EMPTY;
        hoveredScreen = mc.screen;
        hoverX = mouseX();
        hoverY = mouseY();
        hoverTime = System.nanoTime();
        hoveredTarget =
                hovered.isEmpty()
                        ? null
                        : DiaryReadingTarget.observe(mc.screen, hovered, hoverX, hoverY);
        if (!hovered.isEmpty()
                && ((hovered.is(ModRegistry.LONG_TRAVAIL.get())
                                && !TravailClientEvents.OPEN_DIARY.isUnbound())
                        || !CatalogSession.OPEN.isUnbound())) {
            var elements = event.getTooltipElements();
            int lastHint = -1, hintWidth = 0;
            var hintIndices = new java.util.ArrayList<Integer>();
            var hintLines = new java.util.ArrayList<Component>();
            for (int i = 0; i < elements.size(); i++) {
                var line = elements.get(i).left().orElse(null);
                Component prompt =
                        line instanceof Component component
                                ? DiaryHintTooltip.findPrompt(component)
                                : null;
                if (prompt != null) {
                    lastHint = i;
                    hintIndices.add(i);
                    hintLines.add(prompt);
                }
            }
            if (lastHint >= 0) {
                var aligned = DiaryHintTooltip.layout(mc.font, hintLines);
                for (int i = 0; i < aligned.size(); i++) {
                    if (hovered.is(ModRegistry.LONG_TRAVAIL.get()))
                        elements.set(hintIndices.get(i), Either.right(aligned.get(i)));
                    hintWidth = Math.max(hintWidth, aligned.get(i).width(mc.font));
                }
                double progress =
                        holdScreen == mc.screen && sameHoldTarget() ? holdProgress.fraction() : 0;
                elements.add(
                        lastHint + 1, Either.right(new DiaryProgressTooltip(progress, hintWidth)));
            }
        }
    }

    public static void screenClosed(ScreenEvent.Closing event) {
        resetHold();
        if (event.getScreen() == owner) close();
        hovered = ItemStack.EMPTY;
        hoveredTarget = null;
        hoveredScreen = null;
        DiaryInputRouter.clear();
    }

    static void beginHold(Screen screen, InputConstants.Key key, boolean diary, boolean catalog) {
        if (holdScreen != screen || !sameHoldTarget()) {
            resetHold();
            holdTarget = new DiaryReadingTarget(hoveredTarget);
            holdScreen = screen;
            holdFrame = System.nanoTime();
        }
        if (diary) diaryInputs.add(key);
        if (catalog) catalogInputs.add(key);
    }

    static void resetHold() {
        holdProgress.reset();
        holdScreen = null;
        holdTarget = null;
        diaryInputs.clear();
        catalogInputs.clear();
    }

    static void updateHold(ScreenEvent.Render.Post event) {
        if (holdScreen == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (holdScreen != event.getScreen() || !mc.isWindowActive()) {
            resetHold();
            return;
        }
        long now = System.nanoTime();
        double elapsed = Math.min(0.1, (now - holdFrame) / 1_000_000_000.0);
        holdFrame = now;
        boolean valid =
                canRead(holdScreen) && sameHoldTarget() && !DiaryInputRouter.editing(holdScreen);
        var completed =
                holdProgress.advance(
                        elapsed, !diaryInputs.isEmpty(), !catalogInputs.isEmpty(), valid);
        if (completed != null) {
            if (completed == DiaryEntryHold.Entry.DIARY) open(holdScreen);
            else if (hovered.is(ModRegistry.LONG_TRAVAIL.get())) CatalogSession.open(holdScreen);
            else
                com.thelongtravail.client.catalog.CatalogEntries.forStack(holdTarget.get())
                        .ifPresent(
                                entry ->
                                        CatalogSession.openDetail(
                                                holdScreen, entry, holdTarget.get()));
            resetHold();
            return;
        }
        if (holdProgress.fraction() <= 0) {
            if ((diaryInputs.isEmpty() && catalogInputs.isEmpty()) || !valid) resetHold();
            return;
        }
    }

    static boolean canRead(Screen screen) {
        return com.thelongtravail.client.catalog.CatalogSession.active() == null
                && hoveredScreen == screen
                && !hovered.isEmpty()
                && hoveredTarget != null
                && System.nanoTime() - hoverTime < 250_000_000L
                && Math.abs(mouseX() - hoverX) < 0.5
                && Math.abs(mouseY() - hoverY) < 0.5;
    }

    static double mouseX() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.xpos()
                * mc.getWindow().getGuiScaledWidth()
                / mc.getWindow().getScreenWidth();
    }

    static double mouseY() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.ypos()
                * mc.getWindow().getGuiScaledHeight()
                / mc.getWindow().getScreenHeight();
    }

    static void open(Screen screen) {
        owner = screen;
        reading = new TravelDiaryScreen(holdTarget, DiarySession::close);
        reading.init(Minecraft.getInstance(), screen.width, screen.height);
        DiaryActionSounds.open();
        hovered = ItemStack.EMPTY;
        hoveredTarget = null;
        hoveredScreen = null;
    }

    static boolean sameHoldTarget() {
        return holdTarget != null && holdTarget.matches(hoveredTarget);
    }

    public static boolean suppressBackgroundTooltip() {
        return reading != null && !renderingReading;
    }

    public static boolean isReading() {
        return reading != null;
    }

    static void close() {
        boolean wasReading = reading != null;
        reading = null;
        owner = null;
        hovered = ItemStack.EMPTY;
        hoveredScreen = null;
        hoveredTarget = null;
        if (wasReading) DiaryActionSounds.close();
    }

    static boolean owns(Screen screen) {
        return reading != null && owner == screen;
    }

    static boolean hoveredDiary() {
        return hovered.is(ModRegistry.LONG_TRAVAIL.get());
    }

    static void keyPressed(int keyCode, int scanCode, int modifiers) {
        reading.keyPressed(keyCode, scanCode, modifiers);
    }

    static void mouseClicked(double x, double y, int button) {
        reading.mouseClicked(x, y, button);
    }

    static void mouseScrolled(double x, double y, double delta) {
        reading.mouseScrolled(x, y, delta);
    }

    static void releaseEntry(InputConstants.Key key) {
        diaryInputs.remove(key);
        catalogInputs.remove(key);
    }

    public static void logout() {
        close();
        resetHold();
    }

    private DiarySession() {}
}

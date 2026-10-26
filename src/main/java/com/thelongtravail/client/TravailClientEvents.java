package com.thelongtravail.client;

import com.thelongtravail.compat.sounds.EffectSounds;
import com.thelongtravail.client.catalog.CatalogSession;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Either;
import com.thelongtravail.TheLongTravail;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.network.JourneySync;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public final class TravailClientEvents {
    public static final KeyMapping OPEN_DIARY =
            new KeyMapping(
                    "key.the_long_travail.open_diary",
                    KeyConflictContext.GUI,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_LEFT_SHIFT,
                    "key.categories.the_long_travail");
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
    private static final Set<Integer> heldKeys = new HashSet<>();
    private static final Set<Integer> consumedKeys = new HashSet<>();
    private static final Set<Integer> consumedButtons = new HashSet<>();

    @Mod.EventBusSubscriber(
            modid = TheLongTravail.MODID,
            value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void registerEntityRenderers(
                net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(
                    ModRegistry.WAYGUIDE_ENTITY.get(),
                    context ->
                            new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(
                                    context, 1.0F, true));
        }

        @SubscribeEvent
        public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
            event.enqueueWork(
                    () -> {
                        AbundanceUseClient.initialize();
                        net.minecraft.client.gui.screens.MenuScreens.register(
                                ModRegistry.COLORING_MENU.get(), ColoringScreen::new);
                        com.thelongtravail.network.PrinceFlightPacket.receiver =
                                PrinceFlightClient::receive;
                        com.thelongtravail.network.ExecutionTerminationPacket.receiver =
                                ExecutionTerminationClient::receive;
                        com.thelongtravail.network.LanternPacket.receiver =
                                LanternLightingClient::receive;
                        com.thelongtravail.network.EffectNotice.receiver = EffectSounds::receive;
                        com.thelongtravail.network.ItemSoundCue.receiver =
                                DiaryActionSounds::receive;
                        com.thelongtravail.network.StiffPacket.receiver =
                                packet -> {
                                    var player = Minecraft.getInstance().player;
                                    if (player != null)
                                        com.thelongtravail.data.StiffState.accept(
                                                player, packet.remaining());
                                };
                        com.thelongtravail.network.VisualDeprivationPacket.receiver =
                                packet -> {
                                    if (packet.progress())
                                        VisualDeprivationClient.progress(packet.remaining());
                                    else
                                        VisualDeprivationClient.accept(
                                                packet.remaining(),
                                                packet.total(),
                                                packet.immediate());
                                };
                    });
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_DIARY);
        }

        @SubscribeEvent
        public static void registerTooltipComponents(
                RegisterClientTooltipComponentFactoriesEvent event) {
            event.register(DiaryProgressTooltip.class, DiaryProgressTooltip.Renderer::new);
            event.register(DiaryHintTooltip.class, DiaryHintTooltip.Renderer::new);
        }
    }

    @Mod.EventBusSubscriber(
            modid = TheLongTravail.MODID,
            value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
            if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
                EffectSounds.tick();
                DiaryActionSounds.tick();
                VisualDeprivationClient.tick();
            }
        }

        @SubscribeEvent(
                priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST,
                receiveCanceled = true)
        public static void renderFog(
                net.minecraftforge.client.event.ViewportEvent.RenderFog event) {
            VisualDeprivationFog.renderFog(event);
        }

        @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
        public static void fogColor(
                net.minecraftforge.client.event.ViewportEvent.ComputeFogColor event) {
            VisualDeprivationFog.color(event);
        }

        @SubscribeEvent
        public static void logout(
                net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
            if (event.getPlayer() != null)
                com.thelongtravail.data.StiffState.accept(event.getPlayer(), 0);
            JourneySync.reset();
            DiaryDialogue.clear();
            EffectSounds.clear();
            DiaryActionSounds.clear();
            com.thelongtravail.network.TooltipConfigSync.reset();
            VisualDeprivationClient.reset();
            heldKeys.clear();
            consumedKeys.clear();
            consumedButtons.clear();
            close();
            resetHold();
        }

        @SubscribeEvent
        public static void tooltipText(ItemTooltipEvent event) {
            if (event.getItemStack().is(ModRegistry.LONG_TRAVAIL.get()))
                DiaryDialogue.decorate(
                        event.getToolTip(), "tooltip.the_long_travail.dialogue.", 10, true);
            else if (event.getItemStack().is(ModRegistry.HOMECOMING.get()))
                DiaryDialogue.decorate(
                        event.getToolTip(), "tooltip.the_long_travail.homecoming.prose.", 4, false);
            else if (event.getItemStack().is(ModRegistry.RENEWAL.get()))
                DiaryDialogue.decorate(
                        event.getToolTip(), "tooltip.the_long_travail.renewal.prose.", 3, false);
            else if (event.getItemStack().is(ModRegistry.WAYGUIDE.get()))
                DiaryDialogue.decorate(
                        event.getToolTip(), "tooltip.the_long_travail.wayguide.", 3, false);

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
                                                ThemeTooltips.keyName(OPEN_DIARY)
                                                        .copy()
                                                        .withStyle(
                                                                style ->
                                                                        style.withColor(
                                                                                com.thelongtravail
                                                                                        .abyss
                                                                                        .RainTooltips
                                                                                        .ACCENT_COLOR)))
                                        .setStyle(line.getStyle());
                            });
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void render(ScreenEvent.Render.Pre event) {
            if (!Minecraft.getInstance().isWindowActive()) {
                resetHold();
                heldKeys.clear();
                consumedKeys.clear();
                consumedButtons.clear();
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

        @SubscribeEvent(priority = EventPriority.LOWEST)
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

        @SubscribeEvent(priority = EventPriority.LOWEST)
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
                    && ((hovered.is(ModRegistry.LONG_TRAVAIL.get()) && !OPEN_DIARY.isUnbound())
                            || !CatalogSession.OPEN.isUnbound())) {
                var elements = event.getTooltipElements();
                int lastHint = -1, hintWidth = 0;
                var hintIndices = new java.util.ArrayList<Integer>();
                var hintLines = new java.util.ArrayList<Component>();
                for (int i = 0; i < elements.size(); i++) {
                    var line = elements.get(i).left().orElse(null);
                    if (line instanceof Component component
                            && component.getContents() instanceof TranslatableContents contents) {
                        String key = contents.getKey();
                        if (key.startsWith("tooltip.the_long_travail.open_diary")
                                || key.equals("tooltip.the_long_travail.open_catalog")
                                || key.equals("tooltip.the_long_travail.catalog_unbound")
                                || key.equals("tooltip.the_long_travail.summary.open")) {
                            lastHint = i;
                            hintIndices.add(i);
                            hintLines.add(component);
                        }
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
                            holdScreen == mc.screen && sameHoldTarget()
                                    ? holdProgress.fraction()
                                    : 0;
                    elements.add(
                            lastHint + 1,
                            Either.right(new DiaryProgressTooltip(progress, hintWidth)));
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void keyPressed(ScreenEvent.KeyPressed.Pre event) {
            boolean firstPress = heldKeys.add(event.getKeyCode());
            if (CatalogSession.active() != null) return;
            if (reading != null && owner == event.getScreen()) {
                event.setCanceled(true);
                consumedKeys.add(event.getKeyCode());
                if (firstPress)
                    reading.keyPressed(
                            event.getKeyCode(), event.getScanCode(), event.getModifiers());
            } else if (consumedKeys.contains(event.getKeyCode())) {
                event.setCanceled(true);
            } else if (firstPress && canRead(event.getScreen()) && !editing(event.getScreen())) {
                InputConstants.Key key =
                        InputConstants.getKey(event.getKeyCode(), event.getScanCode());
                boolean diary =
                        hovered.is(ModRegistry.LONG_TRAVAIL.get())
                                && matchesEntryKey(OPEN_DIARY, key);
                boolean catalog = matchesEntryKey(CatalogSession.OPEN, key);
                if (diary || catalog) {
                    beginHold(event.getScreen(), key, diary, catalog);
                    consumedKeys.add(event.getKeyCode());
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
        public static void keyReleased(ScreenEvent.KeyReleased.Pre event) {
            InputConstants.Key key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
            diaryInputs.remove(key);
            catalogInputs.remove(key);
            heldKeys.remove(event.getKeyCode());
            if (consumedKeys.remove(event.getKeyCode()) || reading != null) event.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void character(ScreenEvent.CharacterTyped.Pre event) {
            if (com.thelongtravail.client.catalog.CatalogSession.active() != null) return;
            if (reading != null || !consumedKeys.isEmpty()) event.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
            if (reading != null && owner == event.getScreen()) {
                consumedButtons.add(event.getButton());
                event.setCanceled(true);
                reading.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton());
            } else if (canRead(event.getScreen()) && !editing(event.getScreen())) {
                boolean diary =
                        hovered.is(ModRegistry.LONG_TRAVAIL.get())
                                && OPEN_DIARY.matchesMouse(event.getButton());
                boolean catalog = CatalogSession.OPEN.matchesMouse(event.getButton());
                if (diary || catalog) {
                    consumedButtons.add(event.getButton());
                    event.setCanceled(true);
                    beginHold(
                            event.getScreen(),
                            InputConstants.Type.MOUSE.getOrCreate(event.getButton()),
                            diary,
                            catalog);
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
        public static void mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
            InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
            diaryInputs.remove(key);
            catalogInputs.remove(key);
            if (consumedButtons.remove(event.getButton()) || reading != null)
                event.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void mouseDragged(ScreenEvent.MouseDragged.Pre event) {
            if (reading != null || consumedButtons.contains(event.getMouseButton()))
                event.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void scroll(ScreenEvent.MouseScrolled.Pre event) {
            if (reading != null && owner == event.getScreen()) {
                event.setCanceled(true);
                reading.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDelta());
            }
        }

        @SubscribeEvent
        public static void screenClosed(ScreenEvent.Closing event) {
            resetHold();
            if (event.getScreen() == owner) close();
            hovered = ItemStack.EMPTY;
            hoveredTarget = null;
            hoveredScreen = null;
            heldKeys.clear();
            consumedKeys.clear();
            consumedButtons.clear();
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

    private static boolean matchesEntryKey(KeyMapping binding, InputConstants.Key key) {
        if (binding.isUnbound()) return false;
        if (binding.getKeyModifier() == KeyModifier.NONE
                && KeyModifier.isKeyCodeModifier(binding.getKey()))
            return key.equals(binding.getKey()) && binding.getKeyConflictContext().isActive();
        return binding.isActiveAndMatches(key);
    }

    private static void beginHold(
            Screen screen, InputConstants.Key key, boolean diary, boolean catalog) {
        if (holdScreen != screen || !sameHoldTarget()) {
            resetHold();
            holdTarget = new DiaryReadingTarget(hoveredTarget);
            holdScreen = screen;
            holdFrame = System.nanoTime();
        }
        if (diary) diaryInputs.add(key);
        if (catalog) catalogInputs.add(key);
    }

    private static void resetHold() {
        holdProgress.reset();
        holdScreen = null;
        holdTarget = null;
        diaryInputs.clear();
        catalogInputs.clear();
    }

    private static void updateHold(ScreenEvent.Render.Post event) {
        if (holdScreen == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (holdScreen != event.getScreen() || !mc.isWindowActive()) {
            resetHold();
            return;
        }
        long now = System.nanoTime();
        double elapsed = Math.min(0.1, (now - holdFrame) / 1_000_000_000.0);
        holdFrame = now;
        boolean valid = canRead(holdScreen) && sameHoldTarget() && !editing(holdScreen);
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

    private static boolean editing(GuiEventListener listener) {
        if (listener instanceof EditBox box && box.isFocused()) return true;
        return listener instanceof ContainerEventHandler container
                && container.getFocused() != null
                && editing(container.getFocused());
    }

    private static boolean canRead(Screen screen) {
        return com.thelongtravail.client.catalog.CatalogSession.active() == null
                && hoveredScreen == screen
                && !hovered.isEmpty()
                && hoveredTarget != null
                && System.nanoTime() - hoverTime < 250_000_000L
                && Math.abs(mouseX() - hoverX) < 0.5
                && Math.abs(mouseY() - hoverY) < 0.5;
    }

    private static double mouseX() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.xpos()
                * mc.getWindow().getGuiScaledWidth()
                / mc.getWindow().getScreenWidth();
    }

    private static double mouseY() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.ypos()
                * mc.getWindow().getGuiScaledHeight()
                / mc.getWindow().getScreenHeight();
    }

    private static void open(Screen screen) {
        owner = screen;
        reading = new TravelDiaryScreen(holdTarget, TravailClientEvents::close);
        reading.init(Minecraft.getInstance(), screen.width, screen.height);
        DiaryActionSounds.open();
        hovered = ItemStack.EMPTY;
        hoveredTarget = null;
        hoveredScreen = null;
    }

    private static boolean sameHoldTarget() {
        return holdTarget != null && holdTarget.matches(hoveredTarget);
    }

    public static boolean suppressBackgroundTooltip() {
        return reading != null && !renderingReading;
    }

    public static boolean isReading() {
        return reading != null;
    }

    private static void close() {
        boolean wasReading = reading != null;
        reading = null;
        owner = null;
        hovered = ItemStack.EMPTY;
        hoveredScreen = null;
        hoveredTarget = null;
        if (wasReading) DiaryActionSounds.close();
    }

    private TravailClientEvents() {}
}

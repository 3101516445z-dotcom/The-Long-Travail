package com.thelongtravail.client;

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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public final class TravailClientEvents {
    public static final KeyMapping OPEN_DIARY = new KeyMapping("key.the_long_travail.open_diary",
            KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, "key.categories.the_long_travail");
    private static Screen owner;
    private static TravelDiaryScreen reading;
    private static boolean renderingReading;
    private static Screen hoveredScreen;
    private static ItemStack hovered = ItemStack.EMPTY;
    private static double hoverX, hoverY;
    private static long hoverTime;
    private static final DiaryHoldProgress holdProgress = new DiaryHoldProgress();
    private static Screen holdScreen;
    private static ItemStack holdTarget = ItemStack.EMPTY;
    private static int holdKey = -1, holdButton = -1;
    private static boolean holding;
    private static long holdFrame;
    private static final Set<Integer> heldKeys = new HashSet<>();
    private static final Set<Integer> consumedKeys = new HashSet<>();
    private static final Set<Integer> consumedButtons = new HashSet<>();

    @Mod.EventBusSubscriber(modid = TheLongTravail.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                com.thelongtravail.network.EffectNotice.receiver = EffectSounds::receive;
                com.thelongtravail.network.ItemSoundCue.receiver = DiaryActionSounds::receive;
                com.thelongtravail.network.VisualDeprivationPacket.receiver =
                        packet -> VisualDeprivationClient.accept(packet.remaining(), packet.total(), packet.immediate());
            });
        }
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) { event.register(OPEN_DIARY); }

        @SubscribeEvent
        public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
            event.register(DiaryProgressTooltip.class, DiaryProgressTooltip.Renderer::new);
        }
    }

    @Mod.EventBusSubscriber(modid = TheLongTravail.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
            if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
                EffectSounds.tick();
                DiaryActionSounds.tick();
                VisualDeprivationClient.tick();
            }
        }
        @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST, receiveCanceled = true)
        public static void renderFog(net.minecraftforge.client.event.ViewportEvent.RenderFog event) {
            VisualDeprivationFog.renderFog(event);
        }

        @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
        public static void fogColor(net.minecraftforge.client.event.ViewportEvent.ComputeFogColor event) {
            VisualDeprivationFog.color(event);
        }
        @SubscribeEvent
        public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
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
                DiaryDialogue.decorate(event.getToolTip(), "tooltip.the_long_travail.dialogue.", 12, true);
            else if (event.getItemStack().is(ModRegistry.HOMECOMING.get()))
                DiaryDialogue.decorate(event.getToolTip(), "tooltip.the_long_travail.homecoming.prose.", 4, false);
            else if (event.getItemStack().is(ModRegistry.RENEWAL.get()))
                DiaryDialogue.decorate(event.getToolTip(), "tooltip.the_long_travail.renewal.prose.", 3, false);
            if (!event.getItemStack().is(ModRegistry.LONG_TRAVAIL.get()) || !OPEN_DIARY.isUnbound()) return;
            event.getToolTip().replaceAll(line -> line.getContents() instanceof TranslatableContents contents
                    && contents.getKey().equals("tooltip.the_long_travail.open_diary")
                    ? Component.translatable("tooltip.the_long_travail.open_diary_unbound").withStyle(ChatFormatting.DARK_GRAY)
                    : line);
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
            hoveredScreen = null;
            if (reading == null) return;
            if (owner != event.getScreen()) { close(); return; }
            event.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void renderReadingLayer(ScreenEvent.Render.Post event) {
            if (reading == null) updateHold(event);
            if (reading == null || owner != event.getScreen()) return;
            Minecraft mc = Minecraft.getInstance();
            if (reading.width != owner.width || reading.height != owner.height)
                reading.init(mc, owner.width, owner.height);
            // 显示阅读层时保留原容器界面及其服务端菜单。
            event.getGuiGraphics().pose().pushPose();
            event.getGuiGraphics().pose().translate(0, 0, 600);
            renderingReading = true;
            try {
                reading.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            } finally {
                renderingReading = false;
                event.getGuiGraphics().pose().popPose();
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void tooltip(RenderTooltipEvent.GatherComponents event) {
            // 自定义提示框可能在 Forge 的 Color 事件前接管绘制；
            // 收集阶段仍可在接管前识别实际悬停的物品。
            Minecraft mc = Minecraft.getInstance();
            if (reading != null || mc.screen == null) return;
            hovered = event.getItemStack().is(ModRegistry.LONG_TRAVAIL.get()) ? event.getItemStack() : ItemStack.EMPTY;
            hoveredScreen = mc.screen;
            hoverX = mouseX();
            hoverY = mouseY();
            hoverTime = System.nanoTime();
            if (!hovered.isEmpty() && !OPEN_DIARY.isUnbound()) {
                var elements = event.getTooltipElements();
                for (int i = 0; i < elements.size(); i++) {
                    var line = elements.get(i).left().orElse(null);
                    if (line instanceof Component component
                            && component.getContents() instanceof TranslatableContents contents
                            && contents.getKey().equals("tooltip.the_long_travail.open_diary")) {
                        double progress = holdScreen == mc.screen && holdTarget == hovered ? holdProgress.fraction() : 0;
                        elements.add(i + 1, Either.right(new DiaryProgressTooltip(progress, mc.font.width(component))));
                        break;
                    }
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void keyPressed(ScreenEvent.KeyPressed.Pre event) {
            boolean firstPress = heldKeys.add(event.getKeyCode());
            if (reading != null && owner == event.getScreen()) {
                event.setCanceled(true);
                consumedKeys.add(event.getKeyCode());
                if (firstPress) reading.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers());
            } else if (consumedKeys.contains(event.getKeyCode())) {
                event.setCanceled(true);
            } else if (firstPress && matchesReadingKey(InputConstants.getKey(event.getKeyCode(), event.getScanCode()))
                    && canRead(event.getScreen()) && !editing(event.getScreen())) {
                beginHold(event.getScreen(), event.getKeyCode(), -1);
                consumedKeys.add(event.getKeyCode());
                event.setCanceled(true);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void keyReleased(ScreenEvent.KeyReleased.Pre event) {
            if (event.getKeyCode() == holdKey) holding = false;
            heldKeys.remove(event.getKeyCode());
            if (consumedKeys.remove(event.getKeyCode()) || reading != null) event.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void character(ScreenEvent.CharacterTyped.Pre event) {
            if (reading != null || !consumedKeys.isEmpty()) event.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
            if (reading != null && owner == event.getScreen()) {
                consumedButtons.add(event.getButton());
                event.setCanceled(true);
                reading.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton());
            } else if (OPEN_DIARY.matchesMouse(event.getButton()) && canRead(event.getScreen()) && !editing(event.getScreen())) {
                consumedButtons.add(event.getButton());
                event.setCanceled(true);
                beginHold(event.getScreen(), -1, event.getButton());
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
            if (event.getButton() == holdButton) holding = false;
            if (consumedButtons.remove(event.getButton()) || reading != null) event.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void mouseDragged(ScreenEvent.MouseDragged.Pre event) {
            if (reading != null || consumedButtons.contains(event.getMouseButton())) event.setCanceled(true);
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
            hoveredScreen = null;
            heldKeys.clear();
            consumedKeys.clear();
            consumedButtons.clear();
        }
    }

    private static boolean matchesReadingKey(InputConstants.Key key) {
        // Forge 的 NONE 修饰键会在 Shift 按下时拒绝匹配，即使 Shift 本身就是主绑定键。
        // 须允许修饰键作为独立按键绑定。
        if (OPEN_DIARY.getKeyModifier() == KeyModifier.NONE && KeyModifier.isKeyCodeModifier(OPEN_DIARY.getKey())) {
            return key.equals(OPEN_DIARY.getKey()) && OPEN_DIARY.getKeyConflictContext().isActive();
        }
        return OPEN_DIARY.isActiveAndMatches(key);
    }

    private static void beginHold(Screen screen, int key, int button) {
        if (holdScreen != screen || holdTarget != hovered) holdProgress.reset();
        holdScreen = screen;
        holdTarget = hovered;
        holdKey = key;
        holdButton = button;
        holding = true;
        holdFrame = System.nanoTime();
    }

    private static void resetHold() {
        holdProgress.reset();
        holdScreen = null;
        holdTarget = ItemStack.EMPTY;
        holdKey = holdButton = -1;
        holding = false;
    }

    private static void updateHold(ScreenEvent.Render.Post event) {
        if (holdScreen == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (holdScreen != event.getScreen() || !mc.isWindowActive()) { resetHold(); return; }
        long now = System.nanoTime();
        double elapsed = Math.min(0.1, (now - holdFrame) / 1_000_000_000.0);
        holdFrame = now;
        boolean valid = canRead(holdScreen) && hovered == holdTarget && !editing(holdScreen);
        if (holdProgress.advance(elapsed, holding && valid)) {
            open(holdScreen);
            resetHold();
            return;
        }
        if (holdProgress.fraction() <= 0) {
            if (!holding || !valid) resetHold();
            return;
        }
    }

    private static boolean editing(GuiEventListener listener) {
        if (listener instanceof EditBox box && box.isFocused()) return true;
        return listener instanceof ContainerEventHandler container && container.getFocused() != null
                && editing(container.getFocused());
    }

    private static boolean canRead(Screen screen) {
        return hoveredScreen == screen && !hovered.isEmpty()
                && System.nanoTime() - hoverTime < 250_000_000L
                && Math.abs(mouseX() - hoverX) < 0.5 && Math.abs(mouseY() - hoverY) < 0.5;
    }

    private static double mouseX() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
    }

    private static double mouseY() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
    }

    private static void open(Screen screen) {
        owner = screen;
        reading = new TravelDiaryScreen(source(screen, hovered), TravailClientEvents::close);
        reading.init(Minecraft.getInstance(), screen.width, screen.height);
        DiaryActionSounds.open();
        hovered = ItemStack.EMPTY;
        hoveredScreen = null;
    }

    private static Supplier<ItemStack> source(Screen screen, ItemStack target) {
        if (screen instanceof AbstractContainerScreen<?> container) {
            for (var slot : container.getMenu().slots) if (slot.getItem() == target) return slot::getItem;
        }
        var player = Minecraft.getInstance().player;
        if (player != null) {
            var inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                if (inventory.getItem(slot) == target) {
                    int index = slot;
                    return () -> inventory.getItem(index);
                }
            }
            var curios = CuriosApi.getCuriosInventory(player).resolve();
            if (curios.isPresent()) {
                var found = curios.get().findFirstCurio(stack -> stack == target);
                if (found.isPresent()) {
                    var context = found.get().slotContext();
                    return () -> curios.get().getStacksHandler(context.identifier())
                            .map(handler -> handler.getStacks().getStackInSlot(context.index())).orElse(ItemStack.EMPTY);
                }
            }
        }
        // 配方和创造模式预览应显示当前物品自身的状态。
        return () -> target;
    }

    public static boolean suppressBackgroundTooltip() {
        return reading != null && !renderingReading;
    }

    public static boolean isReading() { return reading != null; }

    private static void close() {
        boolean wasReading = reading != null;
        reading = null; owner = null; hovered = ItemStack.EMPTY; hoveredScreen = null;
        // 所有关闭路径汇集于此，重复清理不能重复播放音效。
        if (wasReading) DiaryActionSounds.close();
    }
    private TravailClientEvents() {}
}


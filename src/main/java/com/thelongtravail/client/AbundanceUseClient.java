package com.thelongtravail.client;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.abyss.LiquefiedAbundance;
import com.thelongtravail.network.TooltipConfigSync;
import com.thelongtravail.network.TravailNetwork;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID, value = Dist.CLIENT)
public final class AbundanceUseClient {
    private static boolean locked;
    private static ItemStack original = ItemStack.EMPTY;
    private static int duration;
    private static long revision;
    private static int selectedMode, chargingMode;
    private static Component displayed;
    private static boolean noticeStarted, noticeYielded;

    public static int selectedMode() {
        return selectedMode;
    }

    private static void clearNotice() {
        var gui = (com.thelongtravail.mixin.SkillNoticeGuiAccessor) Minecraft.getInstance().gui;
        if (displayed != null && gui.travail$getOverlayMessage() == displayed) {
            gui.travail$setOverlayMessage(Component.empty());
            gui.travail$setOverlayMessageTime(0);
        }
        displayed = null;
    }

    public static void initialize() {
        // 随统一客户端初始化安装回调，避免事件注册时序差异。
        com.thelongtravail.network.AbundanceModePacket.receiver = mode -> selectedMode = mode;
        LiquefiedAbundance.clientBegin =
                player -> {
                    clearNotice();
                    chargingMode = selectedMode;
                    noticeStarted = false;
                    noticeYielded = false;
                    locked = true;
                    duration = LiquefiedAbundance.useTicks();
                    revision = TooltipConfigSync.revision();
                    original = ItemStack.EMPTY;
                };
    }

    @SubscribeEvent
    public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (locked && event.isUseItem()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
        if (locked && event.isAttack()) cancel();
    }

    private static void cancel() {
        clearNotice();
        noticeYielded = true;
        var mc = Minecraft.getInstance();
        if (mc.player != null
                && mc.player.isUsingItem()
                && mc.player.getUseItem().is(ModRegistry.LIQUEFIED_ABUNDANCE.get())) {
            TravailNetwork.cancelAbundance();
            mc.player.stopUsingItem();
        }
        original = ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void screen(ScreenEvent.Opening event) {
        if (locked) cancel();
    }

    @SubscribeEvent
    public static void scroll(InputEvent.MouseScrollingEvent event) {
        if (locked) cancel();
    }

    @SubscribeEvent
    public static void key(InputEvent.Key event) {
        if (!locked) return;
        var options = Minecraft.getInstance().options;
        if (options.keyDrop.isDown() || options.keySwapOffhand.isDown()) cancel();
        for (var key : options.keyHotbarSlots)
            if (key.isDown()) {
                cancel();
                break;
            }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) {
            locked = false;
            original = ItemStack.EMPTY;
            return;
        }
        if (!locked) return;
        if (!mc.isWindowActive() || mc.screen != null || revision != TooltipConfigSync.revision())
            cancel();
        if (!mc.options.keyUse.isDown()) {
            // 松开结算交给原版，此处仅解除下一次按下的锁定。
            locked = false;
            original = ItemStack.EMPTY;
        } else if (mc.player.isUsingItem()
                && mc.player.getUseItem().is(ModRegistry.LIQUEFIED_ABUNDANCE.get())) {
            if (original.isEmpty()) original = mc.player.getUseItem();
            else if (original != mc.player.getUseItem()) cancel();
        }
    }

    @SubscribeEvent
    public static void noticeTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (!locked
                || mc.player == null
                || mc.screen != null
                || !mc.player.isUsingItem()
                || !mc.player.getUseItem().is(ModRegistry.LIQUEFIED_ABUNDANCE.get())) {
            clearNotice();
            return;
        }
        if (mc.isPaused() || noticeYielded) return;
        var gui = (com.thelongtravail.mixin.SkillNoticeGuiAccessor) mc.gui;
        if (noticeStarted && gui.travail$getOverlayMessage() != displayed) {
            displayed = null;
            noticeYielded = true;
            return;
        }
        if (!noticeStarted) {
            displayed =
                    Component.translatable(
                            "message.the_long_travail.abundance.charging",
                            LiquefiedAbundance.modeName(chargingMode));
            mc.gui.setOverlayMessage(displayed, false);
            noticeStarted = true;
        }
        gui.travail$setOverlayMessageTime(60);
    }

    // 返回负数表示本次提示已不再占有 Action Bar。
    public static double chargeProgress() {
        var mc = Minecraft.getInstance();
        var gui = (com.thelongtravail.mixin.SkillNoticeGuiAccessor) mc.gui;
        if (displayed == null
                || noticeYielded
                || !locked
                || mc.player == null
                || mc.screen != null
                || mc.options.hideGui
                || !mc.player.isUsingItem()
                || !mc.player.getUseItem().is(ModRegistry.LIQUEFIED_ABUNDANCE.get())
                || gui.travail$getOverlayMessage() != displayed
                || gui.travail$getOverlayMessageTime() <= 0) return -1;
        return Math.max(
                0,
                Math.min(1, (duration - mc.player.getUseItemRemainingTicks()) / (double) duration));
    }

    @SubscribeEvent
    public static void renderNotice(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay()
                .id()
                .equals(
                        net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.RECORD_OVERLAY
                                .id())) return;
        double progress = chargeProgress();
        if (progress < 0) return;
        var mc = Minecraft.getInstance();
        var gui = (net.minecraftforge.client.gui.overlay.ForgeGui) mc.gui;
        int width = mc.font.width(displayed);
        // 跟随 Forge 提示区偏移，兼顾额外生命值与护甲行。
        int baseline =
                event.getWindow().getGuiScaledHeight()
                        - Math.max(Math.max(gui.leftHeight, gui.rightHeight) + 9, 68);
        ThinProgressBar.render(
                event.getGuiGraphics(),
                event.getWindow().getGuiScaledWidth() / 2 - width / 2,
                baseline + mc.font.lineHeight - 1,
                width,
                progress);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        clearNotice();
        selectedMode = 0;
        locked = false;
        original = ItemStack.EMPTY;
    }

    private AbundanceUseClient() {}
}

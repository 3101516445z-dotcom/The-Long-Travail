package com.thelongtravail.client;

import com.thelongtravail.item.LastAffairsItem;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "the_long_travail", value = Dist.CLIENT)
public final class LastAffairsUseClient {
    private static Component displayed;
    private static boolean active, yielded;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        var gui = (com.thelongtravail.mixin.presentation.SkillNoticeGuiAccessor) mc.gui;
        boolean using =
                mc.player != null
                        && mc.player.isUsingItem()
                        && mc.player.getUseItem().getItem() instanceof LastAffairsItem;
        if (!using) {
            if (displayed != null && gui.travail$getOverlayMessage() == displayed) {
                gui.travail$setOverlayMessage(Component.empty());
                gui.travail$setOverlayMessageTime(0);
            }
            active = yielded = false;
            displayed = null;
            return;
        }
        if (active && gui.travail$getOverlayMessage() != displayed) yielded = true;
        if (!active) {
            displayed =
                    LastAffairsItem.message(
                            LastAffairsItem.spawn(mc.player.getUseItem())
                                    ? "charging_spawn"
                                    : "charging_death");
            mc.gui.setOverlayMessage(displayed, false);
            active = true;
        }
        if (!yielded && !mc.isPaused()) gui.travail$setOverlayMessageTime(60);
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay()
                .id()
                .equals(
                        net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.RECORD_OVERLAY
                                .id())) return;
        var mc = Minecraft.getInstance();
        if (!active
                || yielded
                || displayed == null
                || mc.player == null
                || mc.options.hideGui
                || !mc.player.isUsingItem()) return;
        var stack = mc.player.getUseItem();
        if (!(stack.getItem() instanceof LastAffairsItem)) return;
        var gui = (net.minecraftforge.client.gui.overlay.ForgeGui) mc.gui;
        int width = mc.font.width(displayed);
        int baseline =
                event.getWindow().getGuiScaledHeight()
                        - Math.max(Math.max(gui.leftHeight, gui.rightHeight) + 9, 68);
        ThinProgressBar.render(
                event.getGuiGraphics(),
                event.getWindow().getGuiScaledWidth() / 2 - width / 2,
                baseline + mc.font.lineHeight - 1,
                width,
                (double) mc.player.getTicksUsingItem() / stack.getUseDuration());
    }

    private LastAffairsUseClient() {}
}

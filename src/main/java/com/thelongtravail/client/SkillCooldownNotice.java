package com.thelongtravail.client;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.abyss.RainTooltips;
import com.thelongtravail.boundless.TimeStopManager;
import com.thelongtravail.mixin.SkillNoticeGuiAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID, value = Dist.CLIENT)
public final class SkillCooldownNotice {
    private static Component displayed;
    private static String key;
    private static int ticks;
    private static boolean dream;

    public static void show(String translationKey, int remainingTicks, boolean dreamSkill) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        clear();
        if (remainingTicks <= 0) {
            mc.player.displayClientMessage(
                    com.thelongtravail.helper.ActionMessages.text(translationKey), true);
            return;
        }
        key = translationKey;
        ticks = remainingTicks;
        dream = dreamSkill;
        displayed = text();
        mc.player.displayClientMessage(displayed, true);
    }

    private static Component text() {
        int seconds = 1 + (Math.max(1, ticks) - 1) / 20;
        var duration =
                Component.translatable("message.the_long_travail.cooldown.seconds", seconds)
                        .withStyle(style -> style.withColor(RainTooltips.ACCENT_COLOR));
        if (key.equals("message.the_long_travail.dream.countdown"))
            return Component.translatable(key, duration)
                    .withStyle(style -> style.withColor(RainTooltips.BODY_COLOR));
        var remaining =
                Component.translatable("message.the_long_travail.cooldown.remaining", duration)
                        .withStyle(style -> style.withColor(RainTooltips.STATE_CONDITION_COLOR));
        return Component.translatable(key, remaining)
                .withStyle(style -> style.withColor(RainTooltips.BODY_COLOR));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || displayed == null) return;
        var mc = Minecraft.getInstance();
        var gui = (SkillNoticeGuiAccessor) mc.gui;
        if (mc.player == null
                || gui.travail$getOverlayMessage() != displayed
                || gui.travail$getOverlayMessageTime() <= 0) {
            clear();
            return;
        }
        if (mc.isPaused() || dream && TimeStopManager.frozen(mc.player)) return;
        int previousSeconds = 1 + (ticks - 1) / 20;
        if (--ticks <= 0) {
            gui.travail$setOverlayMessage(Component.empty());
            clear();
        } else if (1 + (ticks - 1) / 20 != previousSeconds) {
            displayed = text();
            // 此处不能调用 displayClientMessage，否则会重新开始 60 游戏刻的显示计时。
            gui.travail$setOverlayMessage(displayed);
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static void clear() {
        displayed = null;
        key = null;
        ticks = 0;
    }

    private SkillCooldownNotice() {}
}

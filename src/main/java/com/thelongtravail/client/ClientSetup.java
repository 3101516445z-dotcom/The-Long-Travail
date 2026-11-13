package com.thelongtravail.client;

import com.thelongtravail.client.diary.DiaryActionSounds;
import com.thelongtravail.client.diary.DiaryHintTooltip;
import com.thelongtravail.client.diary.DiaryProgressTooltip;
import com.thelongtravail.client.flight.PrinceFlightClient;
import com.thelongtravail.client.lighting.LanternLightingClient;
import com.thelongtravail.client.visual.VisualDeprivationClient;
import com.thelongtravail.compat.sounds.EffectSounds;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

// 客户端注册与网络接收绑定；工作由 ModBus 在客户端生命周期内调用。
final class ClientSetup {
    static void registerEntityRenderers(
            net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                ModRegistry.WAYGUIDE_ENTITY.get(),
                context ->
                        new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(
                                context, 1.0F, true));
    }

    static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(
                () -> {
                    AbundanceUseClient.initialize();
                    com.thelongtravail.network.InsertedEffectPacket.receiver =
                            InsertedEffectClient::receive;
                    com.thelongtravail.item.ItemSummary.append = ThemeTooltips::append;
                    com.thelongtravail.item.ThemeItemText.skillKey =
                            () ->
                                    ThemeTooltips.keyName(BoundlessClient.DREAM)
                                            .copy()
                                            .withStyle(
                                                    style ->
                                                            style.withColor(
                                                                    com.thelongtravail.text
                                                                            .TooltipStyle
                                                                            .ACCENT_COLOR));
                    net.minecraft.client.gui.screens.MenuScreens.register(
                            ModRegistry.COLORING_MENU.get(), ColoringScreen::new);
                    com.thelongtravail.network.PrinceFlightPacket.receiver =
                            PrinceFlightClient::receive;
                    com.thelongtravail.network.ExecutionTerminationPacket.receiver =
                            ExecutionTerminationClient::receive;
                    com.thelongtravail.network.LanternPacket.receiver =
                            LanternLightingClient::receive;
                    com.thelongtravail.network.EffectNotice.receiver = EffectSounds::receive;
                    com.thelongtravail.network.ItemSoundCue.receiver = DiaryActionSounds::receive;
                    com.thelongtravail.network.StiffPacket.receiver =
                            packet -> {
                                var player = Minecraft.getInstance().player;
                                if (player != null)
                                    com.thelongtravail.runtime.effects.StiffState.accept(
                                            player, packet.remaining());
                            };
                    com.thelongtravail.network.VisualDeprivationPacket.receiver =
                            packet -> {
                                if (packet.progress())
                                    VisualDeprivationClient.progress(packet.remaining());
                                else
                                    VisualDeprivationClient.accept(
                                            packet.remaining(), packet.total(), packet.immediate());
                            };
                });
    }

    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(TravailClientEvents.OPEN_DIARY);
    }

    static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(DiaryProgressTooltip.class, DiaryProgressTooltip.Renderer::new);
        event.register(DiaryHintTooltip.class, DiaryHintTooltip.Renderer::new);
    }

    private ClientSetup() {}
}

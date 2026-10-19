package com.thelongtravail.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.thelongtravail.TheLongTravail;
import com.thelongtravail.config.TravailClientConfig;
import com.thelongtravail.network.*;
import com.thelongtravail.abyss.*;
import net.minecraft.client.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.List;

public final class RainClient {
    public static final KeyMapping CAST =
            new KeyMapping(
                    "key.the_long_travail.rain",
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_R,
                    "key.categories.the_long_travail");

    public static void press(InputConstants.Key key, int action) {
        if (action != GLFW.GLFW_PRESS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null
                || !mc.isWindowActive()
                || mc.player == null
                || !mc.player.isAlive()
                || mc.player.isSpectator()) return;
        if (CAST.isActiveAndMatches(key)) TravailNetwork.requestRain();
    }

    @Mod.EventBusSubscriber(modid = TheLongTravail.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent e) {
            e.register(CAST);
            RainMessagePacket.receiver =
                    packet -> {
                        var p = Minecraft.getInstance().player;
                        if (p != null && TravailClientConfig.RAIN_MESSAGES.get())
                            SkillCooldownNotice.show(packet.key(), packet.cooldownTicks(), false);
                    };
        }
    }

    @Mod.EventBusSubscriber(modid = TheLongTravail.MODID, value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent
        public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
            RainState.resetClient();
            RainTooltips.clientEki = List.of();
            RainTooltips.clientTokaido = List.of();
        }
    }

    private RainClient() {}
}

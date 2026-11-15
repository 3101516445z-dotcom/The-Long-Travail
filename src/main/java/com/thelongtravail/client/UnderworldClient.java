package com.thelongtravail.client;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.network.UnderworldPacket;
import com.thelongtravail.network.TravailNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID, value = Dist.CLIENT)
public final class UnderworldClient {
    private static UUID death;

    public static boolean available() {
        return death != null;
    }

    private static Component revivalName() {
        return revivalName("gui.the_long_travail.dead_reborn");
    }

    private static Component revivalName(String key) {
        return Component.translatable(key).withStyle(s -> s.withColor(0xDC143C).withBold(true));
    }

    @Mod.EventBusSubscriber(
            modid = TheLongTravail.MODID,
            value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent
        public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e) {
            UnderworldPacket.receiver =
                    p -> {
                        death = p.available() ? p.death() : null;
                        if (Minecraft.getInstance().screen instanceof DeathScreen screen)
                            screen.init(Minecraft.getInstance(), screen.width, screen.height);
                    };
        }
    }

    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post e) {
        if (!(e.getScreen() instanceof DeathScreen s) || death == null) return;
        UUID id = death;
        var access = (com.thelongtravail.mixin.underworld.BookDeathScreenAccessor) s;
        int firstY =
                access.travail$exitButtons().stream()
                        .mapToInt(Button::getY)
                        .min()
                        .orElse(s.height / 4 + 72);
        for (Button existing : access.travail$exitButtons()) existing.setY(existing.getY() + 24);
        Button button =
                Button.builder(
                                revivalName(),
                                b -> {
                                    TravailNetwork.requestUnderworld(
                                            new UnderworldPacket.Request(id));
                                })
                        .bounds(s.width / 2 - 100, firstY, 200, 20)
                        .build();
        button.active = access.travail$delayTicker() >= 20;
        access.travail$exitButtons().add(0, button);
        e.addListener(button);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
        death = null;
    }

    @SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent e) {
        if (e.phase != net.minecraftforge.event.TickEvent.Phase.END || death == null) return;
        var mc = Minecraft.getInstance();
        if (mc.player != null
                && mc.level != null
                && mc.player.isDeadOrDying()
                && !(mc.screen instanceof DeathScreen))
            mc.setScreen(
                    new DeathScreen(
                            Component.translatable(
                                    "death.attack.generic", mc.player.getDisplayName()),
                            mc.level.getLevelData().isHardcore()));
    }
}

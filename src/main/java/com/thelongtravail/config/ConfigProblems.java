package com.thelongtravail.config;

import com.thelongtravail.TheLongTravail;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class ConfigProblems {
    private static final ConfigDiagnostics diagnostics =
            new ConfigDiagnostics(
                    System::nanoTime,
                    message -> TheLongTravail.LOGGER.warn("[TLT config] {}", message));
    private static final Set<UUID> notified = new java.util.HashSet<>();

    public static void report(Path file, String detail) {
        diagnostics.report(file, detail);
    }

    static void flush() {
        diagnostics.flush(false);
    }

    private static synchronized void notifyPlayer(ServerPlayer player) {
        var server = player.getServer();
        if (diagnostics.size() == 0
                || server == null
                || !(server.isSingleplayer() || player.hasPermissions(2))
                || !notified.add(player.getUUID())) return;
        player.sendSystemMessage(
                Component.translatable("message.the_long_travail.config_recovery"));
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) notifyPlayer(player);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer().getTickCount() % 100 == 0)
            event.getServer().getPlayerList().getPlayers().forEach(ConfigProblems::notifyPlayer);
    }

    @SubscribeEvent
    public static synchronized void stopped(ServerStoppedEvent event) {
        diagnostics.flush(true);
        notified.clear();
    }

    private ConfigProblems() {}
}

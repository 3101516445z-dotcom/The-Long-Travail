package com.thelongtravail.network;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.PlayerJourneyData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class TravailNetwork {
    private static final String PROTOCOL = "15";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TheLongTravail.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(13, UnderworldPacket.class, UnderworldPacket::encode, UnderworldPacket::decode, UnderworldPacket::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(15, BoundlessPacket.Message.class, BoundlessPacket.Message::encode, BoundlessPacket.Message::decode, BoundlessPacket.Message::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(14, UnderworldPacket.Request.class, UnderworldPacket.Request::encode, UnderworldPacket.Request::decode, UnderworldPacket.Request::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(12, LanternPacket.class, LanternPacket::encode, LanternPacket::decode, LanternPacket::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(9, BoundlessPacket.Cast.class, BoundlessPacket.Cast::encode, BoundlessPacket.Cast::decode, BoundlessPacket.Cast::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(10, BoundlessPacket.class, BoundlessPacket::encode, BoundlessPacket::decode, BoundlessPacket::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(11, TimeStopPacket.class, TimeStopPacket::encode, TimeStopPacket::decode, TimeStopPacket::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(8, StiffPacket.class,
                (packet, buffer) -> buffer.writeVarInt(packet.remaining()),
                buffer -> new StiffPacket(buffer.readVarInt()), (packet, supplier) -> {
                    var context = supplier.get();
                    context.enqueueWork(() -> StiffPacket.receiver.accept(packet));
                    context.setPacketHandled(true);
                }, java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(5, RainPacket.class, RainPacket::encode, RainPacket::decode, RainPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(6, RainCastPacket.class, RainCastPacket::encode, RainCastPacket::decode, RainCastPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(7, RainMessagePacket.class, RainMessagePacket::encode, RainMessagePacket::decode, RainMessagePacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(0, TooltipConfigPacket.class,
                TooltipConfigPacket::encode, TooltipConfigPacket::decode, TooltipConfigPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, JourneyPacket.class,
                JourneyPacket::encode, JourneyPacket::decode, JourneyPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2, EffectNotice.class, (packet, buffer) -> {
            buffer.writeResourceLocation(packet.effect()); buffer.writeEnum(packet.kind());
            buffer.writeLong(packet.serial()); buffer.writeLong(packet.batch());
        }, buffer -> new EffectNotice(buffer.readResourceLocation(), buffer.readEnum(EffectNotice.Kind.class),
                buffer.readLong(), buffer.readLong()), (packet, supplier) -> {
            var context = supplier.get(); context.enqueueWork(() -> EffectNotice.receiver.accept(packet));
            context.setPacketHandled(true);
        }, java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(3, ItemSoundCue.class,
                (cue, buffer) -> buffer.writeEnum(cue), buffer -> buffer.readEnum(ItemSoundCue.class),
                (cue, supplier) -> {
                    var context = supplier.get();
                    context.enqueueWork(() -> ItemSoundCue.receiver.accept(cue));
                    context.setPacketHandled(true);
                }, java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(4, VisualDeprivationPacket.class,
                (packet, buffer) -> { buffer.writeVarInt(packet.remaining()); buffer.writeVarInt(packet.total()); buffer.writeBoolean(packet.immediate()); buffer.writeBoolean(packet.progress()); },
                buffer -> new VisualDeprivationPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean()),
                (packet, supplier) -> {
                    var context = supplier.get();
                    context.enqueueWork(() -> VisualDeprivationPacket.receiver.accept(packet));
                    context.setPacketHandled(true);
                }, java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendUnderworld(ServerPlayer p, UnderworldPacket packet) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), packet); }
    public static void requestUnderworld(UnderworldPacket.Request packet) { CHANNEL.sendToServer(packet); }

    public static void sendLantern(ServerPlayer source, LanternPacket packet) {
        if(source.connection != null) CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> source), packet);
    }
    public static void sendLanternTo(ServerPlayer viewer, LanternPacket packet) {
        if(viewer.connection != null) CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), packet);
    }
    public static void sendStiff(ServerPlayer player, int remaining) {
        if (player.connection != null)
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StiffPacket(remaining));
    }

    public static void sendVisualProgress(ServerPlayer player, int remaining) {
        if (player.connection != null)
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new VisualDeprivationPacket(remaining, 0, false, true));
    }

    public static void sendVisualDeprivation(ServerPlayer player, int remaining, int total) {
        sendVisualDeprivation(player, remaining, total, false);
    }

    public static void sendVisualDeprivation(ServerPlayer player, int remaining, int total, boolean immediate) {
        if (player.connection != null)
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new VisualDeprivationPacket(remaining, total, immediate));
    }

    public static void sendItemSound(ServerPlayer player, ItemSoundCue cue) {
        if (player.connection != null) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), cue);
    }

    public static void sendEffectNotice(ServerPlayer player, EffectNotice notice) {
        if (player.connection != null) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), notice);
    }

    public static void sendTooltipConfig(ServerPlayer player) {
        TooltipConfigPacket snapshot = tooltipSnapshot();
        if (snapshot != null) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), snapshot);
    }

    private static TooltipConfigPacket tooltipSnapshot() {
        try { return new TooltipConfigPacket(TooltipConfigSync.serverValues(), TooltipConfigSync.serverPools()); }
        catch (IllegalArgumentException invalid) {
            TheLongTravail.LOGGER.error("Tooltip configuration was not sent: {}. Correct the named pool/value and reload the configuration.", invalid.getMessage());
            return null;
        }
    }

    public static void sendJourney(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new JourneyPacket(
                PlayerJourneyData.discoveredBiomeCount(player), PlayerJourneyData.revealedMask(player)));
    }

    private record JourneyPacket(int count, int revealed) {
        private static void encode(JourneyPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.count); buffer.writeVarInt(packet.revealed);
        }
        private static JourneyPacket decode(FriendlyByteBuf buffer) { return new JourneyPacket(buffer.readVarInt(), buffer.readVarInt()); }
        private static void handle(JourneyPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> JourneySync.accept(packet.count, packet.revealed));
            context.setPacketHandled(true);
        }
    }

    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (!com.thelongtravail.config.ConfigFiles.isCommonSpec(event.getConfig().getSpec())) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) server.execute(() -> {
            if (server.isStopped()) return;
            com.thelongtravail.data.RuntimePools.reload();
            com.thelongtravail.farreach.GoldenAgeActions.reload();
            com.thelongtravail.flourishing.Affection.resetAll();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                // 禁用花朵后重新计算已打开的合成结果，避免旧结果仍可取出。
                player.inventoryMenu.slotsChanged(player.getInventory());
                if (player.containerMenu != player.inventoryMenu) player.containerMenu.slotsChanged(player.getInventory());
            }
            com.thelongtravail.abyss.RainEvents.configReloaded(server);
            TooltipConfigPacket snapshot = tooltipSnapshot();
            if (snapshot == null) return;
            server.getPlayerList().getPlayers().forEach(player ->
                    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), snapshot));
        });
    }

    private record TooltipConfigPacket(Map<String, Double> values, Map<String, List<String>> pools) {
        private TooltipConfigPacket {
            var checked = new TooltipConfigCodec.Snapshot(values, pools);
            values = checked.values(); pools = checked.pools();
        }
        private static void encode(TooltipConfigPacket packet, FriendlyByteBuf buffer) {
            TooltipConfigCodec.encode(new TooltipConfigCodec.Snapshot(packet.values, packet.pools), buffer);
        }
        private static TooltipConfigPacket decode(FriendlyByteBuf buffer) {
            var decoded = TooltipConfigCodec.decode(buffer);
            return new TooltipConfigPacket(decoded.values(), decoded.pools());
        }

        private static void handle(TooltipConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> TooltipConfigSync.accept(packet.values, packet.pools));
            context.setPacketHandled(true);
        }
    }

    public static void sendRain(ServerPlayer player, RainPacket packet) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet); }
    public static void sendRainMessage(ServerPlayer player, RainMessagePacket packet) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet); }
    public static void requestRain() { CHANNEL.sendToServer(new RainCastPacket()); }
    public static void sendDreamMessage(ServerPlayer player, String key) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new BoundlessPacket.Message("message.the_long_travail.dream." + key)); }
    public static void requestDream() { CHANNEL.sendToServer(new BoundlessPacket.Cast()); }
    public static void sendBoundless(ServerPlayer p, BoundlessPacket packet) { if (p.connection != null) CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), packet); }
    public static void sendTimeStop(ServerPlayer p, TimeStopPacket packet) { if (p.connection != null) CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), packet); }
    private TravailNetwork() {}
}

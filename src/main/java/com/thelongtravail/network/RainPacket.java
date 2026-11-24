package com.thelongtravail.network;

import com.thelongtravail.abyss.rain.RainState;
import com.thelongtravail.abyss.rain.RainTooltips;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.util.List;
import java.util.function.Supplier;

public record RainPacket(
        int playerId,
        boolean weather,
        boolean dry,
        boolean snow,
        float speed,
        boolean fire,
        List<Component> eki,
        List<Component> tokaido) {
    public static void encode(RainPacket p, FriendlyByteBuf b) {
        b.writeVarInt(p.playerId);
        b.writeBoolean(p.weather);
        b.writeBoolean(p.dry);
        b.writeBoolean(p.snow);
        b.writeFloat(p.speed);
        b.writeBoolean(p.fire);
        b.writeCollection(p.eki, FriendlyByteBuf::writeComponent);
        b.writeCollection(p.tokaido, FriendlyByteBuf::writeComponent);
        // Retain the trailing slot list for older clients.
        b.writeVarInt(1);
        b.writeUtf(RainState.DECLARED_SLOT);
    }

    public static RainPacket decode(FriendlyByteBuf b) {
        var packet =
                new RainPacket(
                        b.readVarInt(),
                        b.readBoolean(),
                        b.readBoolean(),
                        b.readBoolean(),
                        b.readFloat(),
                        b.readBoolean(),
                        b.readList(FriendlyByteBuf::readComponent),
                        b.readList(FriendlyByteBuf::readComponent));
        b.readList(buf -> buf.readUtf(128));
        return packet;
    }

    public static void handle(RainPacket p, Supplier<NetworkEvent.Context> supplier) {
        var c = supplier.get();
        c.enqueueWork(
                () -> {
                    RainState.clientPlayerId = p.playerId;
                    RainState.clientWeather = p.weather;
                    RainState.clientDry = p.dry;
                    RainState.clientSnow = p.snow;
                    RainState.clientSpeed = p.speed;
                    RainState.clientFire = p.fire;
                    RainTooltips.clientEki = p.eki;
                    RainTooltips.clientTokaido = p.tokaido;
                });
        c.setPacketHandled(true);
    }
}

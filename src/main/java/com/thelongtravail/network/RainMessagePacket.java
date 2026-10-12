package com.thelongtravail.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.*;

public record RainMessagePacket(String key, int cooldownTicks) {
    public RainMessagePacket(String key) {
        this(key, 0);
    }

    public static Consumer<RainMessagePacket> receiver = ignored -> {};

    public static void encode(RainMessagePacket p, FriendlyByteBuf b) {
        b.writeUtf(p.key, 128);
        b.writeVarInt(p.cooldownTicks);
    }

    public static RainMessagePacket decode(FriendlyByteBuf b) {
        return new RainMessagePacket(b.readUtf(128), b.readVarInt());
    }

    public static void handle(RainMessagePacket p, Supplier<NetworkEvent.Context> supplier) {
        var c = supplier.get();
        c.enqueueWork(() -> receiver.accept(p));
        c.setPacketHandled(true);
    }
}

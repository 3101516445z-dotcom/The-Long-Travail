package com.thelongtravail.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.*;
public record RainMessagePacket(String key) {
    public static Consumer<String> receiver = ignored -> {};
    public static void encode(RainMessagePacket p, FriendlyByteBuf b) { b.writeUtf(p.key,128); }
    public static RainMessagePacket decode(FriendlyByteBuf b) { return new RainMessagePacket(b.readUtf(128)); }
    public static void handle(RainMessagePacket p, Supplier<NetworkEvent.Context> supplier) {
        var c=supplier.get(); c.enqueueWork(() -> receiver.accept(p.key)); c.setPacketHandled(true);
    }
}

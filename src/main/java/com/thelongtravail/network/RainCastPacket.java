package com.thelongtravail.network;
import com.thelongtravail.abyss.RainEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record RainCastPacket() {
    public static void encode(RainCastPacket p, FriendlyByteBuf b) {}
    public static RainCastPacket decode(FriendlyByteBuf b) { return new RainCastPacket(); }
    public static void handle(RainCastPacket p, Supplier<NetworkEvent.Context> supplier) {
        var c=supplier.get(); c.enqueueWork(() -> { if(c.getSender()!=null) RainEvents.cast(c.getSender()); }); c.setPacketHandled(true);
    }
}

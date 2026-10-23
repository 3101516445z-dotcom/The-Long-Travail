package com.thelongtravail.network;

import com.thelongtravail.abyss.RainEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record RainCastPacket() {
    public static void encode(RainCastPacket p, FriendlyByteBuf b) {}

    public static RainCastPacket decode(FriendlyByteBuf b) {
        return new RainCastPacket();
    }

    public static void handle(RainCastPacket p, Supplier<NetworkEvent.Context> supplier) {
        var c = supplier.get();
        var sender = c.getSender();
        var connection = c.getNetworkManager();
        c.enqueueWork(
                () -> {
                    if (sender == null
                            || !connection.isConnected()
                            || sender.isRemoved()
                            || sender.connection == null
                            || sender.connection != connection.getPacketListener()
                            || sender.connection.player != sender
                            || sender.serverLevel()
                                            .getServer()
                                            .getPlayerList()
                                            .getPlayer(sender.getUUID())
                                    != sender) return;
                    RainEvents.cast(sender);
                });
        c.setPacketHandled(true);
    }
}

package com.thelongtravail.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

// 类型以服务端为准，不接受客户端指定的类型。
public record AbundanceModePacket(int mode) {
    public static Consumer<Integer> receiver = mode -> {};

    public static void encode(AbundanceModePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.mode());
    }

    public static AbundanceModePacket decode(FriendlyByteBuf buffer) {
        return new AbundanceModePacket(buffer.readVarInt());
    }

    public static void handle(AbundanceModePacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(
                () -> receiver.accept(packet.mode() >= 0 && packet.mode() < 3 ? packet.mode() : 0));
        context.setPacketHandled(true);
    }
}

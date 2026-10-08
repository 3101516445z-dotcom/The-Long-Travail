package com.thelongtravail.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

// 仅同步装备生效状态；位置由原有实体同步与客户端插值提供。
public record LanternPacket(ResourceLocation dimension, UUID player, boolean active) {
    public static Consumer<LanternPacket> receiver = p -> {};
    public static void encode(LanternPacket p, FriendlyByteBuf b) {
        b.writeResourceLocation(p.dimension); b.writeUUID(p.player); b.writeBoolean(p.active);
    }
    public static LanternPacket decode(FriendlyByteBuf b) {
        return new LanternPacket(b.readResourceLocation(), b.readUUID(), b.readBoolean());
    }
    public static void handle(LanternPacket p, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get(); context.enqueueWork(() -> receiver.accept(p)); context.setPacketHandled(true);
    }
}

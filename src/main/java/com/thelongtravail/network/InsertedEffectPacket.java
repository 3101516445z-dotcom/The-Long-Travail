package com.thelongtravail.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

// 服务端明确植入的效果快照，客户端按实体身份接收。
public record InsertedEffectPacket(
        ResourceLocation dimension, int id, UUID uuid, CompoundTag effect) {
    public static Consumer<InsertedEffectPacket> receiver = packet -> {};

    public static void encode(InsertedEffectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.id);
        buffer.writeUUID(packet.uuid);
        buffer.writeNbt(packet.effect);
    }

    public static InsertedEffectPacket decode(FriendlyByteBuf buffer) {
        return new InsertedEffectPacket(
                buffer.readResourceLocation(),
                buffer.readVarInt(),
                buffer.readUUID(),
                buffer.readNbt());
    }

    public static void handle(
            InsertedEffectPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> receiver.accept(packet));
        context.setPacketHandled(true);
    }
}

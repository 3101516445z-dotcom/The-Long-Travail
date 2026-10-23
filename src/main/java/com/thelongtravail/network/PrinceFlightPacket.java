package com.thelongtravail.network;

import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

// 只同步服务端授予的飞行权限，不同步玩家当前是否正在飞行。
public record PrinceFlightPacket(
        ResourceLocation dimension, int entityId, UUID playerId, boolean active, boolean revoke) {
    public PrinceFlightPacket(
            ResourceLocation dimension, int entityId, UUID playerId, boolean active) {
        this(dimension, entityId, playerId, active, false);
    }

    public static BiConsumer<PrinceFlightPacket, Connection> receiver = (packet, connection) -> {};

    public static void encode(PrinceFlightPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.entityId);
        buffer.writeUUID(packet.playerId);
        buffer.writeBoolean(packet.active);
        buffer.writeBoolean(packet.revoke);
    }

    public static PrinceFlightPacket decode(FriendlyByteBuf buffer) {
        return new PrinceFlightPacket(
                buffer.readResourceLocation(),
                buffer.readVarInt(),
                buffer.readUUID(),
                buffer.readBoolean(),
                buffer.readBoolean());
    }
}

package com.thelongtravail.network;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

public record TimeStopCuePacket(ResourceLocation dimension, Vec3 position) {
    public static Consumer<TimeStopCuePacket> receiver = packet -> {};

    public static void encode(TimeStopCuePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeDouble(packet.position.x);
        buffer.writeDouble(packet.position.y);
        buffer.writeDouble(packet.position.z);
    }

    public static TimeStopCuePacket decode(FriendlyByteBuf buffer) {
        ResourceLocation dimension = buffer.readResourceLocation();
        Vec3 position = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        if (!Double.isFinite(position.lengthSqr())) {
            throw new IllegalArgumentException("Invalid time stop cue position");
        }
        return new TimeStopCuePacket(dimension, position);
    }

    public static void handle(TimeStopCuePacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> receiver.accept(packet));
        context.setPacketHandled(true);
    }
}

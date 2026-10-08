package com.thelongtravail.network;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.*;
import java.util.function.*;

public record TimeStopPacket(ResourceLocation dimension, List<View> fields) {
    // 保持协议 15 的既有限制；管理器输入校验与解码器共用这些边界。
    public static final int MAX_FIELDS = 4096, MAX_ALLIES = 4096;
    public static final double MAX_RADIUS = 128;
    public record View(long id, Vec3 center, double radius, Set<UUID> allies) {
        public boolean contains(Vec3 position) { return center.distanceToSqr(position) <= radius*radius; }
    }
    public static Consumer<TimeStopPacket> receiver = packet -> {};
    public static Predicate<Entity> clientFrozen = entity -> false;
    public static BiFunction<Entity,Vec3,Vec3> clientClip = (entity,delta) -> delta;
    public static BiPredicate<Level,BlockPos> clientBlock = (level,position) -> false;
    public static void encode(TimeStopPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.fields.size());
        for (View field : packet.fields) {
            buffer.writeLong(field.id);
            buffer.writeDouble(field.center.x);
            buffer.writeDouble(field.center.y);
            buffer.writeDouble(field.center.z);
            buffer.writeDouble(field.radius);
            buffer.writeCollection(field.allies, FriendlyByteBuf::writeUUID);
        }
    }
    public static TimeStopPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation dimension = buffer.readResourceLocation();
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_FIELDS) throw new IllegalArgumentException("time stop field count");
        List<View> fields = new ArrayList<>();
        for (int i=0; i<count; i++) {
            long id = buffer.readLong();
            Vec3 center = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
            double radius = buffer.readDouble();
            int alliesCount = buffer.readVarInt();
            if (alliesCount < 0 || alliesCount > MAX_ALLIES || !Double.isFinite(center.lengthSqr())
                    || !Double.isFinite(radius) || radius <= 0 || radius > MAX_RADIUS)
                throw new IllegalArgumentException("time stop field");
            Set<UUID> allies = new HashSet<>();
            for (int k=0; k<alliesCount; k++) allies.add(buffer.readUUID());
            fields.add(new View(id, center, radius, Set.copyOf(allies)));
        }
        return new TimeStopPacket(dimension, List.copyOf(fields));
    }
    public static void handle(TimeStopPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> receiver.accept(packet));
        context.setPacketHandled(true);
    }
    public static void send(ServerLevel level) { TimeStopSync.send(level); }
}

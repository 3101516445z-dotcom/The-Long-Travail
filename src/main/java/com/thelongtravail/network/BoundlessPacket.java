package com.thelongtravail.network;

import com.thelongtravail.boundless.dream.DreamState;
import com.thelongtravail.runtime.player.PlayerCooldowns;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.*;

public record BoundlessPacket(boolean active, int cooldown) {
    public static Consumer<BoundlessPacket> receiver = p -> {};

    public static void encode(BoundlessPacket p, FriendlyByteBuf b) {
        b.writeBoolean(p.active);
        b.writeVarInt(p.cooldown);
    }

    public static BoundlessPacket decode(FriendlyByteBuf b) {
        return new BoundlessPacket(b.readBoolean(), b.readVarInt());
    }

    public static void handle(BoundlessPacket p, Supplier<NetworkEvent.Context> s) {
        var c = s.get();
        c.enqueueWork(() -> receiver.accept(p));
        c.setPacketHandled(true);
    }

    public static void sendDream(ServerPlayer p, boolean active) {
        TravailNetwork.sendBoundless(
                p,
                new BoundlessPacket(active, PlayerCooldowns.storedTicks(p, DreamState.COOLDOWN)));
    }

    public record Message(Skill skill, String key, int cooldownTicks) {
        public static Consumer<Message> receiver = key -> {};

        public static void encode(Message p, FriendlyByteBuf b) {
            b.writeEnum(p.skill);
            b.writeUtf(p.key, 128);
            b.writeVarInt(p.cooldownTicks);
        }

        public static Message decode(FriendlyByteBuf b) {
            return new Message(b.readEnum(Skill.class), b.readUtf(128), b.readVarInt());
        }

        public static void handle(Message p, Supplier<NetworkEvent.Context> s) {
            var c = s.get();
            c.enqueueWork(() -> receiver.accept(p));
            c.setPacketHandled(true);
        }
    }

    public enum Skill {
        DREAM,
        WHISPER
    }

    public record Cast(Skill skill) {
        public static void encode(Cast p, FriendlyByteBuf b) {
            b.writeEnum(p.skill);
        }

        public static Cast decode(FriendlyByteBuf b) {
            return new Cast(b.readEnum(Skill.class));
        }

        public static void handle(Cast p, Supplier<NetworkEvent.Context> s) {
            var c = s.get();
            var sender = c.getSender();
            var connection = c.getNetworkManager();
            c.enqueueWork(
                    () -> {
                        // 保留接收请求时的发送者实例，避免重生后将旧请求交给新实例；在线状态在服务端实际执行时校验。
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
                        switch (p.skill) {
                            case DREAM ->
                                    com.thelongtravail.boundless.dream.DreamState.cast(sender);
                            case WHISPER -> com.thelongtravail.boundless.HolyFool.cast(sender);
                        }
                    });
            c.setPacketHandled(true);
        }
    }
}

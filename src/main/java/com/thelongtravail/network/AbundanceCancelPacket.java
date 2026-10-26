package com.thelongtravail.network;

import com.thelongtravail.abyss.LiquefiedAbundance;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 仅取消发送者的转换，不接收奖励或已蓄力时间参数。
public record AbundanceCancelPacket() {
    public static void encode(AbundanceCancelPacket packet, FriendlyByteBuf buffer) {}

    public static AbundanceCancelPacket decode(FriendlyByteBuf buffer) {
        return new AbundanceCancelPacket();
    }

    public static void handle(
            AbundanceCancelPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(
                () -> {
                    var player = context.getSender();
                    if (player != null) LiquefiedAbundance.cancel(player);
                });
        context.setPacketHandled(true);
    }
}

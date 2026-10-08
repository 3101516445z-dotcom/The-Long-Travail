package com.thelongtravail.network;

import com.thelongtravail.underworld.BookRevival;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.*;

public record UnderworldPacket(UUID death,boolean available) {
    public static Consumer<UnderworldPacket> receiver=p->{};
    public static void encode(UnderworldPacket p,FriendlyByteBuf b){b.writeUUID(p.death);b.writeBoolean(p.available);}
    public static UnderworldPacket decode(FriendlyByteBuf b){return new UnderworldPacket(b.readUUID(),b.readBoolean());}
    public static void handle(UnderworldPacket p,Supplier<NetworkEvent.Context> c){var ctx=c.get();ctx.enqueueWork(()->receiver.accept(p));ctx.setPacketHandled(true);}
    public static void send(ServerPlayer p,UUID id,boolean available){if(p.connection!=null)TravailNetwork.sendUnderworld(p,new UnderworldPacket(id,available));}
    public record Request(UUID death) {
        public static void encode(Request p,FriendlyByteBuf b){b.writeUUID(p.death);}
        public static Request decode(FriendlyByteBuf b){return new Request(b.readUUID());}
        public static void handle(Request p,Supplier<NetworkEvent.Context> c){var ctx=c.get();ctx.enqueueWork(()->{if(ctx.getSender()!=null)BookRevival.request(ctx.getSender(),p.death);});ctx.setPacketHandled(true);}
    }
}

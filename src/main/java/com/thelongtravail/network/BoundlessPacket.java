package com.thelongtravail.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.*;
public record BoundlessPacket(boolean active,int cooldown) {
    public static Consumer<BoundlessPacket> receiver=p->{};
    public static void encode(BoundlessPacket p,FriendlyByteBuf b){b.writeBoolean(p.active);b.writeVarInt(p.cooldown);}
    public static BoundlessPacket decode(FriendlyByteBuf b){return new BoundlessPacket(b.readBoolean(),b.readVarInt());}
    public static void handle(BoundlessPacket p,Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->receiver.accept(p));c.setPacketHandled(true);}
    public static void sendDream(ServerPlayer p,boolean active){TravailNetwork.sendBoundless(p,new BoundlessPacket(active,p.getPersistentData().getInt(com.thelongtravail.boundless.DreamState.COOLDOWN)));}
    public record Message(String key) {
        public static Consumer<String> receiver = key -> {};
        public static void encode(Message p,FriendlyByteBuf b){b.writeUtf(p.key,128);}
        public static Message decode(FriendlyByteBuf b){return new Message(b.readUtf(128));}
        public static void handle(Message p,Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->receiver.accept(p.key));c.setPacketHandled(true);}
    }
    public record Cast(){
        public static void encode(Cast p,FriendlyByteBuf b){}
        public static Cast decode(FriendlyByteBuf b){return new Cast();}
        public static void handle(Cast p,Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->{if(c.getSender()!=null)com.thelongtravail.boundless.DreamState.cast(c.getSender());});c.setPacketHandled(true);}
    }
}

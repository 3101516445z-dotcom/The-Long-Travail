package com.thelongtravail.network;
import com.thelongtravail.abyss.RainState;
import com.thelongtravail.abyss.RainTooltips;
import com.thelongtravail.config.RainConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.util.List;
import java.util.function.Supplier;

public record RainPacket(int playerId, boolean weather, boolean dry, boolean snow, float speed, boolean fire,
                         List<Component> eki, List<Component> tokaido, List<String> slots) {
    public static void encode(RainPacket p, FriendlyByteBuf b) {
        b.writeVarInt(p.playerId);
        b.writeBoolean(p.weather); b.writeBoolean(p.dry); b.writeBoolean(p.snow); b.writeFloat(p.speed); b.writeBoolean(p.fire);
        b.writeCollection(p.eki, FriendlyByteBuf::writeComponent); b.writeCollection(p.tokaido, FriendlyByteBuf::writeComponent);
        b.writeCollection(p.slots, (buf, value) -> buf.writeUtf(value,RainConfig.MAX_SLOT_LENGTH));
    }
    public static RainPacket decode(FriendlyByteBuf b) {
        return new RainPacket(b.readVarInt(), b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readFloat(), b.readBoolean(),
                b.readList(FriendlyByteBuf::readComponent), b.readList(FriendlyByteBuf::readComponent), b.readList(buf -> buf.readUtf(RainConfig.MAX_SLOT_LENGTH)));
    }
    public static void handle(RainPacket p, Supplier<NetworkEvent.Context> supplier) {
        var c = supplier.get(); c.enqueueWork(() -> {
            RainState.clientPlayerId=p.playerId;
            RainState.clientWeather=p.weather; RainState.clientDry=p.dry; RainState.clientSnow=p.snow;
            RainState.clientSpeed=p.speed; RainState.clientFire=p.fire;
            RainTooltips.clientEki=p.eki; RainTooltips.clientTokaido=p.tokaido;
            RainState.clientSlots=p.slots;
        }); c.setPacketHandled(true);
    }
}

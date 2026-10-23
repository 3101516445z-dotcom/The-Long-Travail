package com.thelongtravail.client;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.boundless.PrinceFlightPermission;
import com.thelongtravail.network.PrinceFlightPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID, value = Dist.CLIENT)
public final class PrinceFlightClient {
    private static final PrinceFlightPermission PERMISSION = new PrinceFlightPermission();

    public static void receive(PrinceFlightPacket packet, net.minecraft.network.Connection source) {
        var minecraft = Minecraft.getInstance();
        var connection = minecraft.getConnection();
        // 连接关闭或更换后，忽略旧连接排队等待的任务。
        if (connection != null && connection.getConnection() == source)
            PERMISSION.accept(minecraft.player, packet);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        PERMISSION.tick(Minecraft.getInstance().player);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        PERMISSION.clear();
    }

    private PrinceFlightClient() {}
}

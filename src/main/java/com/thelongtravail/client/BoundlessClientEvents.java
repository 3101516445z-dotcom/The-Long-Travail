package com.thelongtravail.client;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.client.timestop.TimeStopClient;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 无垠客户端装配：先处理梦境，再处理时停，统一保持初始化和退出顺序。
public final class BoundlessClientEvents {
    @Mod.EventBusSubscriber(
            modid = TheLongTravail.MODID,
            value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) {
            BoundlessClient.register(event);
            TimeStopClient.setup();
        }
    }

    @Mod.EventBusSubscriber(modid = TheLongTravail.MODID, value = Dist.CLIENT)
    public static final class BoundlessForgeBus {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            BoundlessClient.tick(event);
        }

        @SubscribeEvent
        public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
            BoundlessClient.clear();
            TimeStopClient.clear();
        }
    }

    private BoundlessClientEvents() {}
}

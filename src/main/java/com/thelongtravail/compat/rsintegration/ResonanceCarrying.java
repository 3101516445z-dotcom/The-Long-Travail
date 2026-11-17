package com.thelongtravail.compat.rsintegration;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.api.CarryingApi;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

// 共振盘仅提供只读携带来源，效果资格仍由各物品判断。
@Mod.EventBusSubscriber(modid = TheLongTravail.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ResonanceCarrying {
    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        if (ModList.get().isLoaded("rs_integration"))
            event.enqueueWork(
                    () ->
                            CarryingApi.registerProvider(
                                    new ResourceLocation(TheLongTravail.MODID, "rsi_resonance"),
                                    ResonanceCarrying::carries));
    }

    private static boolean carries(ServerPlayer player, Item item) {
        try {
            return ResonanceInventoryAccess.contains(player, item);
        } catch (LinkageError failure) {
            // 将 API 缺失或二进制不兼容交给携带入口，按来源限频记录并保留 UNKNOWN。
            throw new IllegalStateException("RSI resonance API is not binary compatible", failure);
        }
    }

    private ResonanceCarrying() {}
}

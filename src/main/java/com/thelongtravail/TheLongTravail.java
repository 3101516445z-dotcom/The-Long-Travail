package com.thelongtravail;

import com.mojang.logging.LogUtils;
import com.thelongtravail.config.ConfigFiles;
import com.thelongtravail.event.TravailEvents;
import com.thelongtravail.network.TravailNetwork;
import com.thelongtravail.registry.ModRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(TheLongTravail.MODID)
public class TheLongTravail {
    public static final String MODID = "the_long_travail";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TheLongTravail(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModRegistry.register(modBus);
        TravailNetwork.register();
        modBus.addListener(TravailNetwork::onConfigReloading);
        ConfigFiles.register(context);
        MinecraftForge.EVENT_BUS.register(new TravailEvents());
    }
}

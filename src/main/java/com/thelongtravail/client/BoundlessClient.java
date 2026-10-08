package com.thelongtravail.client;
import com.mojang.blaze3d.platform.InputConstants;
import com.thelongtravail.TheLongTravail;
import com.thelongtravail.boundless.*;
import com.thelongtravail.network.*;
import net.minecraft.client.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.*;
public final class BoundlessClient {
    public static final KeyMapping DREAM=new KeyMapping("key.the_long_travail.dream",KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_T,"key.categories.the_long_travail");
    private static InputConstants.Key handledDreamKey;
    public static boolean dreaming,cooling;public static int cooldown;
    public static boolean press(InputConstants.Key key,int action){
        if(key.equals(handledDreamKey)){
            if(action==GLFW.GLFW_REPEAT)return true;
            if(action==GLFW.GLFW_RELEASE){handledDreamKey=null;return true;}
        }
        var mc=Minecraft.getInstance();if(action!=GLFW.GLFW_PRESS||mc.screen!=null||!mc.isWindowActive()||mc.player==null||!mc.player.isAlive()||TimeStopManager.frozen(mc.player)||!DREAM.isActiveAndMatches(key))return false;
        if(!dreaming&&(mc.player.isSpectator()||!BoundlessEquipment.equipped(mc.player,true)))return false;
        if(!dreaming&&(cooling||cooldown>0)){handledDreamKey=key;message("message.the_long_travail.dream.unavailable");return true;}
        handledDreamKey=key;TravailNetwork.requestDream();return true;
    }

    private static void message(String key){
        var player=Minecraft.getInstance().player;
        if(player!=null&&com.thelongtravail.config.TravailClientConfig.DREAM_MESSAGES.get())
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(key),true);
    }
    @Mod.EventBusSubscriber(modid=TheLongTravail.MODID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(DREAM);BoundlessPacket.Message.receiver=BoundlessClient::message;com.thelongtravail.boundless.BoundlessTooltips.viewer=()->Minecraft.getInstance().player;com.thelongtravail.boundless.BoundlessTooltips.dreamKey=()->DREAM.getTranslatedKeyMessage();BoundlessPacket.receiver=p->{dreaming=p.active();cooldown=Math.max(0,p.cooldown());cooling=cooldown>0;};TimeStopClient.setup();}
    }
    @Mod.EventBusSubscriber(modid=TheLongTravail.MODID,value=Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent e){if(e.phase==net.minecraftforge.event.TickEvent.Phase.END&&cooldown>0&&Minecraft.getInstance().player!=null&&!TimeStopManager.frozen(Minecraft.getInstance().player))cooldown=Math.max(cooling?1:0,cooldown-1);}
        @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){dreaming=false;cooling=false;cooldown=0;handledDreamKey=null;TimeStopClient.clear();}

    }
    private BoundlessClient(){}
}

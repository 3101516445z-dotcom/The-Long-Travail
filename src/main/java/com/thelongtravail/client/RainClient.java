package com.thelongtravail.client;
import com.mojang.blaze3d.platform.InputConstants;
import com.thelongtravail.TheLongTravail;
import com.thelongtravail.config.TravailClientConfig;
import com.thelongtravail.network.*;
import com.thelongtravail.abyss.*;
import net.minecraft.client.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.List;

public final class RainClient {
    public static final KeyMapping CAST = new KeyMapping("key.the_long_travail.rain", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.the_long_travail");
    public static void press(InputConstants.Key key, int action) {
        // 界面占用输入时仍检测按键刚按下的状态，不消耗 KeyMapping 中排队的点击事件。
        if(action!=GLFW.GLFW_PRESS) return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.screen!=null || !mc.isWindowActive() || mc.player==null || !mc.player.isAlive() || mc.player.isSpectator()) return;
        if(CAST.isActiveAndMatches(key)) TravailNetwork.requestRain();
    }
    @Mod.EventBusSubscriber(modid=TheLongTravail.MODID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent e) {
            e.register(CAST);
            RainMessagePacket.receiver=key -> {
                var p=Minecraft.getInstance().player;
                if(p!=null && TravailClientConfig.RAIN_MESSAGES.get()) p.displayClientMessage(Component.translatable(key),true);
            };
        }
    }
    @Mod.EventBusSubscriber(modid=TheLongTravail.MODID,value=Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
            RainState.resetClient(); RainTooltips.clientEki=List.of(); RainTooltips.clientTokaido=List.of();
        }
        @SubscribeEvent public static void tooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent e) {
            if(e.getItemStack().is(com.thelongtravail.registry.ModRegistry.EKI.get())) {
                var lines = e.getToolTip();
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents
                            && RainTooltips.EKI_LOCKED_KEY.equals(contents.getKey())) {
                        lines.remove(i);
                        lines.addAll(i, RainTooltips.ekiForViewer(e.getEntity()));
                        break;
                    }
                }
            }
            if(e.getItemStack().is(com.thelongtravail.registry.ModRegistry.TOKAIDO.get())) {
                e.getToolTip().replaceAll(line -> line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents
                        && RainTooltips.CAST_KEY.equals(contents.getKey())
                        ? Component.translatable(RainTooltips.CAST_KEY, CAST.getTranslatedKeyMessage().copy().withStyle(style -> style.withColor(RainTooltips.ACCENT_COLOR))).withStyle(line.getStyle()) : line);
            }
            if(e.getItemStack().is(com.thelongtravail.registry.ModRegistry.EKI.get())
                    || e.getItemStack().is(com.thelongtravail.registry.ModRegistry.TOKAIDO.get())) {
                e.getToolTip().replaceAll(line -> line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents
                        && contents.getKey().startsWith("tooltip.the_long_travail.")
                        ? RainTooltips.coloredLine(line) : line);
            }
        }
    }
    private RainClient() {}
}

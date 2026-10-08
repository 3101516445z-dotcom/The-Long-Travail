package com.thelongtravail.client;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.item.UnderworldAccessoryItem;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.network.*;
import com.thelongtravail.abyss.RainTooltips;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid=TheLongTravail.MODID,value=Dist.CLIENT)
public final class UnderworldClient {
    private static UUID death;
    public static boolean available(){return death!=null;}
    private static Component revivalName(){return revivalName("gui.the_long_travail.dead_reborn");}
    private static Component revivalName(String key){
        return Component.translatable(key)
                .withStyle(s->s.withColor(0xDC143C).withBold(true));
    }
    @Mod.EventBusSubscriber(modid=TheLongTravail.MODID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e){
            UnderworldPacket.receiver=p->{death=p.available()?p.death():null;
                if(Minecraft.getInstance().screen instanceof DeathScreen screen)screen.init(Minecraft.getInstance(),screen.width,screen.height);
            };
        }
    }
    @SubscribeEvent public static void init(ScreenEvent.Init.Post e) {
        if(!(e.getScreen() instanceof DeathScreen s)||death==null)return;
        UUID id=death;
        Button button=Button.builder(revivalName(),b->{
            TravailNetwork.requestUnderworld(new UnderworldPacket.Request(id));
        }).bounds(s.width/2-100,s.height/4+120,200,20).build();
        var access=(com.thelongtravail.mixin.BookDeathScreenAccessor)s;
        button.active=access.travail$delayTicker()>=20;
        access.travail$exitButtons().add(button);
        e.addListener(button);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){death=null;}
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent e) {
        if(e.phase!=net.minecraftforge.event.TickEvent.Phase.END||death==null)return;
        var mc=Minecraft.getInstance();
        // 死亡玩家重连时，原版不会重新发送 CombatKill 数据包，须根据服务端保存的复活资格恢复界面。
        if(mc.player!=null&&mc.level!=null&&mc.player.isDeadOrDying()&&!(mc.screen instanceof DeathScreen))
            mc.setScreen(new DeathScreen(Component.translatable("death.attack.generic",mc.player.getDisplayName()),mc.level.getLevelData().isHardcore()));
    }
    @SubscribeEvent public static void tooltip(ItemTooltipEvent e) {
        if(!(e.getItemStack().getItem() instanceof UnderworldAccessoryItem item))return;
        var lines=e.getToolTip();int at=-1;
        for(int i=0;i<lines.size();i++)if(lines.get(i).getContents() instanceof TranslatableContents c&&c.getKey().equals("tooltip.the_long_travail.underworld_items.locked")){at=i;break;}
        if(at<0)return;
        List<Component> out=new ArrayList<>();out.add(Component.empty());
        if(e.getEntity()==null||TravailCurios.stack(e.getEntity()).isEmpty()) {
            out.add(lines.get(at));out.add(Component.empty());
            for(int i=0;i<3;i++)out.add(Component.literal("?".repeat(18-i*3)).withStyle(s->s.withColor(RainTooltips.BODY_COLOR).withObfuscated(true)));
        }else{
            String prefix="tooltip.the_long_travail."+(item.ring?"thousand_years.":"book_of_dead.");
            boolean witnessed=com.thelongtravail.data.LongTravailData.hasWitness(
                    TravailCurios.stack(e.getEntity()),com.thelongtravail.TravailAspect.UNDERWORLD);
            String[] keys=item.ring?new String[]{"malice_title","malice","","witness_title","witness"}
                    :witnessed?new String[]{"revive","hardcore","","limits"}
                    :new String[]{"revive","hardcore","","witness_condition","witness","","limits"};
            for(String key:keys){
                if(key.isEmpty()){out.add(Component.empty());continue;}
                if(!item.ring&&key.equals("revive")){
                    out.add(Component.translatable(prefix+key,revivalName("tooltip.the_long_travail.book_of_dead.revival_name")).withStyle(s->s.withColor(RainTooltips.BODY_COLOR)));
                    continue;
                }
                Object[] args=key.equals("malice")?new Object[]{number(TooltipConfigSync.decimal("underworldItem.wither",.1)*100)}
                        :new Object[0];
                out.add(RainTooltips.coloredLine(Component.translatable(prefix+key,args).withStyle(s->s.withColor(key.endsWith("title")?RainTooltips.TITLE_COLOR:RainTooltips.BODY_COLOR))));
            }
        }
        lines.remove(at);lines.addAll(at,out);
    }
    private static String number(double d){return java.math.BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();}
}

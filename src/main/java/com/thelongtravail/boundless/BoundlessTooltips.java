package com.thelongtravail.boundless;

import com.thelongtravail.config.BoundlessItemsConfig;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.network.TooltipConfigSync;
import com.thelongtravail.abyss.RainTooltips;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Supplier;

// 直接生成物品说明，不依赖其他模组可能包裹或展开的提示占位组件。
public final class BoundlessTooltips {
    public static Supplier<Player> viewer=()->null;
    public static Supplier<Component> dreamKey=()->Component.literal("T");
    private static String number(String key,double scale){return BigDecimal.valueOf(TooltipConfigSync.decimal("boundlessItem."+key,BoundlessItemsConfig.get(key))*scale).stripTrailingZeros().toPlainString();}
    private static Component text(String key,Object...args){
        int color=key.endsWith("_heading")?RainTooltips.TITLE_COLOR:RainTooltips.BODY_COLOR;
        return RainTooltips.coloredLine(Component.translatable("tooltip.the_long_travail."+key,args).withStyle(s->s.withColor(color)));
    }
    public static List<Component> forViewer(boolean dream,Player player,Component key){
        List<Component> text=new ArrayList<>();
        text.add(Component.empty());
        if(dream){
            text.add(text("daydream.effect",key));
            text.add(text("daydream.duration",number("dream.duration",1)));
            text.add(text("daydream.wake"));
            text.add(text("daydream.cooldown",number("dream.cooldown",1)));
        }else if(player!=null&&!TravailCurios.stack(player).isEmpty()){
            text.add(text("star_voice.malice_heading"));
            text.add(text("star_voice.malice",number("star.maliceChance",100),number("star.maliceDuration",1)));
            text.add(text("star_voice.lock"));
            text.add(Component.empty());
            text.add(text("star_voice.witness_heading"));
            text.add(text("star_voice.witness",number("star.witnessChance",100),number("star.witnessDuration",1)));
            text.add(text("star_voice.interval",number("star.witnessInterval",1)));
            text.add(text("star_voice.cooldown",number("star.witnessCooldown",1)));
            text.add(text("star_voice.lock"));
            text.add(Component.empty());
            text.add(text("star_voice.range",number("star.radius",1)));
            text.add(text("star_voice.group",number("star.groupLimit",1)));
            text.add(text("star_voice.allies_heading"));
            text.add(text("star_voice.allies"));
        }else{
            text.add(RainTooltips.coloredLine(Component.translatable("tooltip.the_long_travail.star_voice.locked").withStyle(net.minecraft.ChatFormatting.DARK_RED)));
            text.add(Component.empty());
            String name=Component.translatable("item.the_long_travail.star_voice").getString();
            for(int k=0;k<3;k++)text.add(Component.literal(name+" "+"?".repeat(18-k*4)).withStyle(s->s.withColor(RainTooltips.BODY_COLOR).withObfuscated(true)));
            return List.copyOf(text);
        }
        return List.copyOf(text);
    }
    private BoundlessTooltips(){}
}

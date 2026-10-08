package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.config.FarReachItemsConfig;
import com.thelongtravail.helper.DependentAccessories;
import com.thelongtravail.network.TooltipConfigSync;
import com.thelongtravail.abyss.RainTooltips;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import java.math.BigDecimal;
import java.util.List;

public final class FarReachAccessoryItem extends Item implements ICurioItem {
    public static final String LOCKED_KEY = "tooltip.the_long_travail.far_items.locked";
    private final boolean gold;
    public FarReachAccessoryItem(boolean gold){super(new Properties().stacksTo(1).rarity(Rarity.RARE));this.gold=gold;}
    @Override public Component getName(ItemStack stack){return AspectTheme.FAR_REACH.name(super.getName(stack));}
    @Override public boolean canEquip(SlotContext c,ItemStack s){return !c.cosmetic()&&c.identifier().equals(gold?"charm":"back")&&DependentAccessories.unique(c,this);}
    @Override public boolean canEquipFromUse(SlotContext c,ItemStack s){return false;}
    @Override public boolean canRightClickEquip(ItemStack s){return false;}
    private static String n(String k,double scale){return BigDecimal.valueOf(TooltipConfigSync.decimal("farItem."+k,FarReachItemsConfig.get(k))*scale).stripTrailingZeros().toPlainString();}
    private static void line(List<Component> out,String key,Object...args){out.add(RainTooltips.coloredLine(Component.translatable("tooltip.the_long_travail."+key,args).withStyle(s->s.withColor(key.endsWith("heading")?RainTooltips.TITLE_COLOR:RainTooltips.BODY_COLOR))));}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> out,TooltipFlag flag){
        out.add(Component.empty());
        // 在客户端使用当前查看者展开，不将解锁状态写入物品。
        out.add(Component.translatable(LOCKED_KEY).withStyle(net.minecraft.ChatFormatting.DARK_RED));
    }
    public void appendDetails(List<Component> out, net.minecraft.world.entity.player.Player viewer){
        boolean unlocked = viewer != null && !com.thelongtravail.helper.TravailCurios.stack(viewer).isEmpty();
        if(gold){
            if(unlocked){line(out,"golden_age.dig_unlocked");line(out,"golden_age.dig_result");}
            else line(out,"golden_age.dig");
        }else{
            line(out,"icarus.flight");
            line(out,"icarus.ignite",TooltipConfigSync.integer("farItem.icarus.igniteSeconds", FarReachItemsConfig.IGNITE_SECONDS.get()));
        }
        out.add(Component.empty());
        if(!unlocked){
            out.add(Component.translatable(LOCKED_KEY).withStyle(net.minecraft.ChatFormatting.DARK_RED));
            out.add(Component.empty());
            for(int i=0;i<3;i++) out.add(Component.literal((gold ? "Golden Age " : "Icarus ")+"?".repeat(18-i*4))
                    .withStyle(s->s.withColor(RainTooltips.BODY_COLOR).withObfuscated(true)));
            return;
        }
        if(gold){
            line(out,"golden_age.malice_heading");line(out,"golden_age.convert");line(out,"golden_age.malice");
            out.add(Component.empty());
            line(out,"golden_age.witness_heading");line(out,"golden_age.convert");line(out,"golden_age.witness");
        }else{
            line(out,"icarus.malice_heading");line(out,"icarus.malice",n("icarus.malicePeak",100));
            line(out,"icarus.sunny",n("icarus.sunnyPeak",100));line(out,"icarus.dimensions");
            out.add(Component.empty());
            line(out,"icarus.witness_heading");line(out,"icarus.magic");line(out,"icarus.witness",n("icarus.witnessBonus",100));
        }
    }
}

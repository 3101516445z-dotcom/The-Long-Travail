package com.thelongtravail.item;
import com.thelongtravail.AspectTheme;
import com.thelongtravail.helper.DependentAccessories;
import com.thelongtravail.valley.SwordLanternConfig;
import com.thelongtravail.network.TooltipConfigSync;
import com.thelongtravail.abyss.RainTooltips;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import java.util.List;
public final class SwordLanternItem extends Item implements ICurioItem {
    public SwordLanternItem(){super(new Properties().stacksTo(1).rarity(Rarity.RARE));}
    @Override public Component getName(ItemStack s){return AspectTheme.DEEP_VALLEY.name(super.getName(s));}
    @Override public boolean canEquip(SlotContext c,ItemStack s){return !c.cosmetic()&&c.identifier().equals("belt")&&DependentAccessories.unique(c,this);}
    @Override public boolean canEquipFromUse(SlotContext c,ItemStack s){return false;}
    @Override public boolean canRightClickEquip(ItemStack s){return false;}
    private static String num(String key,double fallback,double scale){return java.math.BigDecimal.valueOf(TooltipConfigSync.decimal("swordLantern."+key,fallback)*scale).stripTrailingZeros().toPlainString();}
    private static void line(List<Component> out,String key,Object...args){
        for(String text:Component.translatable("tooltip.the_long_travail.sword_lantern."+key,args).getString().split("\\n"))
            out.add(RainTooltips.coloredLine(Component.literal(text).withStyle(s->s.withColor(RainTooltips.BODY_COLOR))));
    }
    @Override public void appendHoverText(ItemStack s,Level l,List<Component> out,TooltipFlag f){
        out.add(Component.empty());line(out,"light");line(out,"immunity");out.add(Component.empty());
        String bonus=num("bonus",SwordLanternConfig.BONUS.get(),100);
        line(out,"alone",bonus);line(out,"crowd",bonus);line(out,"undead",bonus);line(out,"duration",num("duration",SwordLanternConfig.DURATION.get(),1D/60),bonus);
        line(out,"health",num("health",SwordLanternConfig.HEALTH.get(),100),bonus);
    }
}

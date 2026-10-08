package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.helper.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import java.util.List;

public final class BoundlessAccessoryItem extends Item implements ICurioItem {
    public final boolean dream;
    public BoundlessAccessoryItem(boolean dream){super(new Properties().stacksTo(1).rarity(Rarity.RARE));this.dream=dream;}
    @Override public Component getName(ItemStack s){return AspectTheme.BOUNDLESS.name(super.getName(s));}
    @Override public boolean canEquip(SlotContext c,ItemStack s){return !c.cosmetic()&&c.identifier().equals(dream?"head":"necklace")&&c.entity() instanceof Player p
            &&(dream||!TravailCurios.stack(p).isEmpty())&&DependentAccessories.unique(c,this);}
    @Override public boolean canEquipFromUse(SlotContext c,ItemStack s){return false;}
    @Override public boolean canRightClickEquip(ItemStack s){return false;}
    @Override public void appendHoverText(ItemStack s,Level l,List<Component> lines,TooltipFlag f){lines.addAll(com.thelongtravail.boundless.BoundlessTooltips.forViewer(dream,l==null||l.isClientSide?com.thelongtravail.boundless.BoundlessTooltips.viewer.get():null,com.thelongtravail.boundless.BoundlessTooltips.dreamKey.get()));}
}

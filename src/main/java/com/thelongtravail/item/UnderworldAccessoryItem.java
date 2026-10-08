package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.helper.*;
import com.thelongtravail.underworld.UnderworldItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import java.util.List;

public final class UnderworldAccessoryItem extends Item implements ICurioItem {
    public final boolean ring;
    public UnderworldAccessoryItem(boolean ring) { super(new Properties().stacksTo(1).rarity(Rarity.RARE)); this.ring=ring; }
    @Override public Component getName(ItemStack stack) { return AspectTheme.UNDERWORLD.name(super.getName(stack)); }
    @Override public boolean canEquip(SlotContext c, ItemStack stack) {
        Boolean restoring=com.thelongtravail.underworld.AccessoryRestore.allowed(c,ring);
        return !c.cosmetic() && c.identifier().equals(ring ? "ring" : "charm") && c.entity() instanceof Player p
                && !UnderworldItems.consumed(p,stack) && (restoring!=null?restoring:
                !TravailCurios.stack(p).isEmpty() && DependentAccessories.unique(c,this));
    }
    @Override public boolean canEquipFromUse(SlotContext c, ItemStack s) { return false; }
    @Override public boolean canRightClickEquip(ItemStack s) { return false; }
    @Override public void appendHoverText(ItemStack s, Level l, List<Component> lines, TooltipFlag f) {
        lines.add(Component.translatable("tooltip.the_long_travail.underworld_items.locked").withStyle(net.minecraft.ChatFormatting.DARK_RED));
    }
}

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

    public UnderworldAccessoryItem(boolean ring) {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
        this.ring = ring;
    }

    @Override
    public Component getName(ItemStack stack) {
        return AspectTheme.UNDERWORLD.name(super.getName(stack));
    }

    @Override
    public boolean canEquip(SlotContext c, ItemStack stack) {
        return ring
                && !c.cosmetic()
                && c.identifier().equals("ring")
                && c.entity() instanceof Player p
                && !UnderworldItems.consumed(p, stack)
                && AccessoryEquipRules.hasDiary(p)
                && DependentAccessories.unique(c, this);
    }

    @Override
    public boolean canEquipFromUse(SlotContext c, ItemStack s) {
        return ring;
    }

    @Override
    public boolean canRightClickEquip(ItemStack s) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> lines, TooltipFlag f) {
        ItemDescription.summary(lines);
    }
}

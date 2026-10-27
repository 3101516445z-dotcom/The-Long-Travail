package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.helper.AccessoryEquipRules;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public final class DonQuixoteItem extends Item implements ICurioItem, ItemDescription {
    public DonQuixoteItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public Component getName(ItemStack stack) {
        return AspectTheme.FAR_REACH.name(super.getName(stack));
    }

    @Override
    public boolean canEquip(SlotContext c, ItemStack s) {
        return !c.cosmetic() && AccessoryEquipRules.unique(c, this);
    }

    @Override
    public boolean canEquipFromUse(SlotContext c, ItemStack s) {
        return true;
    }

    @Override
    public boolean canRightClickEquip(ItemStack s) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> out, TooltipFlag f) {
        ItemDescription.summary(out);
    }

    @Override
    public void appendDetails(ItemStack s, Level l, List<Component> out, TooltipFlag f) {
        com.thelongtravail.farreach.FarRelicText.details(out, true);
    }
}

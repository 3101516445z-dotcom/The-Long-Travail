package com.thelongtravail.item;

import com.thelongtravail.equipment.AccessoryEquipRules;

import com.thelongtravail.abyss.rain.RainTooltips;
import com.thelongtravail.abyss.rain.RainState;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public final class TokaidoItem extends Item implements ICurioItem, ItemDescription {
    public TokaidoItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public boolean canEquipFromUse(SlotContext context, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canRightClickEquip(ItemStack stack) {
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        return com.thelongtravail.AspectTheme.ABYSS.name(super.getName(stack));
    }

    @Override
    public boolean canEquip(SlotContext context, ItemStack stack) {
        return AccessoryEquipRules.canEquipUnique(context, this, RainState.DECLARED_SLOT);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        ItemDescription.summary(stack, lines);
    }

    @Override
    public void appendDetails(
            ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        RainTooltips.tokaido(lines);
    }
}

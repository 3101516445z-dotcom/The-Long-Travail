package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.equipment.AccessoryEquipRules;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public final class HolyFoolItem extends Item implements ICurioItem, ItemDescription {
    public HolyFoolItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public Component getName(ItemStack stack) {
        return AspectTheme.BOUNDLESS.name(super.getName(stack));
    }

    @Override
    public boolean canEquip(SlotContext context, ItemStack stack) {
        return !context.cosmetic()
                && context.identifier().equals("head")
                && AccessoryEquipRules.unique(context, this);
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
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        ItemDescription.summary(stack, lines);
    }

    @Override
    public void appendDetails(
            ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        ThemeItemText.details(stack, lines);
    }
}

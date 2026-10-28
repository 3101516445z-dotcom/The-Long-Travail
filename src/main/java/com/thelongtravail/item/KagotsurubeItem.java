package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.helper.AccessoryEquipRules;
import com.thelongtravail.underworld.Kagotsurube;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public final class KagotsurubeItem extends Item implements ICurioItem, ItemDescription {
    public KagotsurubeItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public Component getName(ItemStack stack) {
        return AspectTheme.UNDERWORLD.name(super.getName(stack));
    }

    @Override
    public boolean canEquip(SlotContext c, ItemStack s) {
        return !c.cosmetic()
                && c.identifier().equals("belt")
                && AccessoryEquipRules.unique(c, this);
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
    public void onEquip(SlotContext c, ItemStack old, ItemStack s) {
        Kagotsurube.refresh(c.entity());
    }

    @Override
    public void onUnequip(SlotContext c, ItemStack next, ItemStack s) {
        Kagotsurube.refresh(c.entity());
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> out, TooltipFlag flag) {
        ItemDescription.summary(out);
    }

    @Override
    public void appendDetails(ItemStack stack, Level level, List<Component> out, TooltipFlag flag) {
        out.addAll(SunBladeText.details(false));
    }
}

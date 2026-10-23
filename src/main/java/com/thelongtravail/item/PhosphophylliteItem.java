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

public final class PhosphophylliteItem extends Item implements ICurioItem, ItemDescription {
    public PhosphophylliteItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public Component getName(ItemStack stack) {
        return AspectTheme.DEEP_VALLEY.name(super.getName(stack));
    }

    @Override
    public boolean canEquip(SlotContext c, ItemStack stack) {
        return !c.cosmetic()
                && c.identifier().equals("curio")
                && c.entity() instanceof Player p
                && AccessoryEquipRules.hasDiary(p)
                && AccessoryEquipRules.unique(c, this);
    }

    @Override
    public boolean canEquipFromUse(SlotContext c, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canRightClickEquip(ItemStack stack) {
        return true;
    }

    @Override
    public void onUnequip(SlotContext c, ItemStack next, ItemStack old) {
        if (!c.entity().level().isClientSide)
            com.thelongtravail.valley.Phosphophyllite.tick(c.entity());
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> out, TooltipFlag f) {
        ItemDescription.summary(out);
    }

    @Override
    public void appendDetails(ItemStack s, Level l, List<Component> out, TooltipFlag f) {
        out.addAll(com.thelongtravail.valley.DeepValleyItemText.details("phosphophyllite"));
    }
}

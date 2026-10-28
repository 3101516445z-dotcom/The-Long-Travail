package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import java.util.List;

public final class SunChaserItem extends Item implements ItemDescription {
    public SunChaserItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public Component getName(ItemStack stack) {
        return AspectTheme.FAR_REACH.name(super.getName(stack));
    }

    @Override
    public void inventoryTick(
            ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (entity instanceof net.minecraft.server.level.ServerPlayer player)
            com.thelongtravail.api.CarryingApi.inventoryTick(player, this, slot);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> out, TooltipFlag flag) {
        ItemDescription.summary(out);
    }

    @Override
    public void appendDetails(ItemStack stack, Level level, List<Component> out, TooltipFlag flag) {
        out.addAll(com.thelongtravail.item.SunBladeText.details(true));
    }
}

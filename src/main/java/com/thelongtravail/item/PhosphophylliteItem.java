package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.api.CarryingApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;


import java.util.List;

public final class PhosphophylliteItem extends Item implements ItemDescription {
    public PhosphophylliteItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public Component getName(ItemStack stack) {
        return AspectTheme.DEEP_VALLEY.name(super.getName(stack));
    }

    @Override
    public void inventoryTick(
            ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (entity instanceof ServerPlayer player) CarryingApi.inventoryTick(player, this, slot);
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> out, TooltipFlag f) {
        ItemDescription.summary(s, out);
    }

    @Override
    public void appendDetails(ItemStack s, Level l, List<Component> out, TooltipFlag f) {
        out.addAll(com.thelongtravail.valley.DeepValleyItemText.details("phosphophyllite"));
    }
}

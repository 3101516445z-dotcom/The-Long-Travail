package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.farreach.BabelCarrying;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class BabelItem extends Item implements ItemDescription {
    public BabelItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public Component getName(ItemStack s) {
        return AspectTheme.FAR_REACH.name(super.getName(s));
    }

    @Override
    public void inventoryTick(ItemStack s, Level l, Entity e, int slot, boolean selected) {
        if (e instanceof ServerPlayer p) BabelCarrying.inventoryTick(p, slot);
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> out, TooltipFlag f) {
        ItemDescription.summary(out);
    }

    @Override
    public void appendDetails(ItemStack s, Level l, List<Component> out, TooltipFlag f) {
        com.thelongtravail.farreach.FarRelicText.details(out, false);
    }
}

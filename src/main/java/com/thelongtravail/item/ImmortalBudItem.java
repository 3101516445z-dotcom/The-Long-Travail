package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class ImmortalBudItem extends Item implements ItemDescription {
    public ImmortalBudItem() {
        super(new Properties().rarity(Rarity.RARE).fireResistant());
    }

    @Override
    public Component getName(ItemStack stack) {
        return AspectTheme.FLOURISHING.name(super.getName(stack));
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        ItemDescription.summary(lines);
    }

    @Override
    public void appendDetails(
            ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        for (int i = 0; i < 6; i++) {
            if (i == 3) lines.add(Component.empty());
            lines.add(Component.translatable("tooltip.the_long_travail.immortal_bud.details." + i));
        }
    }
}

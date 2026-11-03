package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.List;

public final class AncientStrangeTalesItem extends BookItem implements ItemDescription {
    public AncientStrangeTalesItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public Component getName(ItemStack stack) {
        return AspectTheme.BOUNDLESS.name(super.getName(stack));
    }

    // 使用物品接口声明资格；附魔候选、等级及选取交由当前附魔系统决定。
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment.isAllowedOnBooks();
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        ItemDescription.summary(lines);
    }

    @Override
    public void appendDetails(
            ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        for (int i = 0; i < 7; i++) {
            if (i == 3 || i == 6) lines.add(Component.empty());
            lines.add(
                    Component.translatable(
                            "tooltip.the_long_travail.ancient_strange_tales.details." + i));
        }
    }
}

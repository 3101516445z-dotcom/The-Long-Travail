package com.thelongtravail.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;

public interface ItemDescription {
    String CATALOG = "tooltip.the_long_travail.catalog_marker";
    String SUMMARY = "tooltip.the_long_travail.summary_marker";

    void appendDetails(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag);

    static void summary(List<Component> lines) {
        lines.add(Component.translatable(SUMMARY));
    }
}

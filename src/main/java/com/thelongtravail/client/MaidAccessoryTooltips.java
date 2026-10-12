package com.thelongtravail.client;

import com.thelongtravail.compat.maid.MaidAccessories;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.fml.ModList;

public final class MaidAccessoryTooltips {
    private static final String PREFIX = "tooltip.the_long_travail.maid_compat.";

    public static void append(
            net.minecraft.world.item.ItemStack stack, java.util.List<Component> lines) {
        if (!ModList.get().isLoaded("touhou_little_maid")) return;
        if (!MaidAccessories.items().contains(stack.getItem())) return;
        int insertion = -1;
        // 在女仆原生提示生成后插入，避免依赖后续渲染事件的物品和文本结构。
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getContents() instanceof TranslatableContents text) {
                if (text.getKey().startsWith(PREFIX)) return;
                if (text.getKey().equals("tooltips.touhou_little_maid.bauble.usage"))
                    insertion = i + 1;
            }
        }
        if (insertion < 0) return;
        lines.add(
                insertion++,
                Component.translatable(PREFIX + "basic").withStyle(ChatFormatting.DARK_GRAY));
        if (stack.is(ModRegistry.TOKAIDO.get()))
            lines.add(
                    insertion,
                    Component.translatable(PREFIX + "tokaido").withStyle(ChatFormatting.DARK_GRAY));
        else if (!stack.is(ModRegistry.SPRING_GAME.get())
                && !stack.is(ModRegistry.SWORD_AND_LANTERN.get()))
            lines.add(
                    insertion,
                    Component.translatable(PREFIX + "diary").withStyle(ChatFormatting.DARK_GRAY));
    }

    private MaidAccessoryTooltips() {}
}

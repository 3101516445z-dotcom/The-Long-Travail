package com.thelongtravail.compat.maid.client;

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
                Component.translatable(
                                PREFIX
                                        + (stack.is(ModRegistry.MIDNIGHT_LAFITE.get())
                                                ? "lafite.basic"
                                                : stack.is(ModRegistry.SOUL_BANNER.get())
                                                                || stack.is(
                                                                        ModRegistry.DEMON_KING
                                                                                .get())
                                                        ? "relic.basic"
                                                        : "basic"))
                        .withStyle(ChatFormatting.DARK_GRAY));
        if (stack.is(ModRegistry.TOKAIDO.get()))
            lines.add(
                    insertion,
                    Component.translatable(PREFIX + "tokaido").withStyle(ChatFormatting.DARK_GRAY));
        else if (!stack.is(ModRegistry.SPRING_CORNER.get())
                && !stack.is(ModRegistry.SWORD_AND_LANTERN.get()))
            lines.add(
                    insertion++,
                    Component.translatable(
                                    PREFIX
                                            + (stack.is(ModRegistry.MIDNIGHT_LAFITE.get())
                                                    ? "lafite.diary"
                                                    : "diary"))
                            .withStyle(ChatFormatting.DARK_GRAY));
        if (stack.is(ModRegistry.SOUL_BANNER.get())) {
            lines.add(
                    insertion++,
                    Component.translatable(PREFIX + "banner.souls")
                            .withStyle(ChatFormatting.DARK_GRAY));
            lines.add(
                    insertion,
                    Component.translatable(PREFIX + "banner.diary")
                            .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static boolean isMaidTooltip(Component component) {
        return component.getContents() instanceof TranslatableContents text
                        && (text.getKey().equals("tooltips.touhou_little_maid.bauble.desc")
                                || text.getKey().equals("tooltips.touhou_little_maid.bauble.usage")
                                || text.getKey().startsWith(PREFIX))
                || component.getSiblings().stream().anyMatch(MaidAccessoryTooltips::isMaidTooltip);
    }

    private MaidAccessoryTooltips() {}
}

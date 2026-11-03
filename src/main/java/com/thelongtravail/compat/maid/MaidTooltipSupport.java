package com.thelongtravail.compat.maid;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class MaidTooltipSupport {
    private static final String PREFIX = "tooltip.the_long_travail.maid_compat.";

    public static boolean isMaidTooltip(Component component) {
        return component.getContents() instanceof TranslatableContents text
                        && (text.getKey().equals("tooltips.touhou_little_maid.bauble.desc")
                                || text.getKey().equals("tooltips.touhou_little_maid.bauble.usage")
                                || text.getKey().startsWith(PREFIX))
                || component.getSiblings().stream().anyMatch(MaidTooltipSupport::isMaidTooltip);
    }

    private MaidTooltipSupport() {}
}

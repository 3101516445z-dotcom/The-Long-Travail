package com.thelongtravail.compat.apotheosis;

import com.thelongtravail.AspectTheme;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;

public final class AffixNameStyle {
    public static final String DIARY = "the_long_travail:apotheosis:name";
    public static final String ASPECT = "the_long_travail:apotheosis:aspect";

    public static Component decorate(ItemStack stack, Component name) {
        if (stack.getTagElement(AffixHelper.AFFIX_DATA) == null) return name;
        String original = stack.getItem().getName(stack).getStyle().getInsertion();
        boolean diary = "the_long_travail:name".equals(original);
        if (!diary && AspectTheme.fromMarker(original) == null) return name;
        var rarity = AffixHelper.getRarity(stack);
        if (!rarity.isBound()) return name;
        var color = rarity.get().getColor();
        String marker = diary ? DIARY : ASPECT;
        var result = Component.empty();
        // 展开文本时保留字体、粗体、斜体及原文；子组件须统一使用稀有度颜色，
        // 否则会继续使用苦旅显式设置的颜色。
        name.visit(
                (style, text) -> {
                    result.append(
                            Component.literal(text)
                                    .setStyle(style.withColor(color).withInsertion(marker)));
                    return Optional.empty();
                },
                Style.EMPTY);
        return result;
    }

    private AffixNameStyle() {}
}

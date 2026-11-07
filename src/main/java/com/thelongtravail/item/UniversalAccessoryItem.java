package com.thelongtravail.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

// Curios 构建槽位列表后统一替换显示，佩戴规则仍由各物品负责。
public interface UniversalAccessoryItem extends ICurioItem {
    @Override
    default List<Component> getSlotsTooltip(List<Component> tooltip, ItemStack stack) {
        return List.of(
                Component.translatable(
                                "tooltip.the_long_travail.universal_slot",
                                Component.translatable("tooltip.the_long_travail.universal_slot.name")
                                        .withStyle(ChatFormatting.YELLOW))
                        .withStyle(ChatFormatting.GOLD));
    }
}

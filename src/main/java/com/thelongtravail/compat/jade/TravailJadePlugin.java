package com.thelongtravail.compat.jade;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.abyss.RainTooltips;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.Identifiers;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

// 仅由 Jade 发现和加载，通用初始化流程不得引用此类。
@WailaPlugin
public final class TravailJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addTooltipCollectedCallback(
                (tooltip, accessor) -> {
                    if (!(accessor instanceof EntityAccessor entityAccessor)
                            || !(entityAccessor.getEntity() instanceof ItemEntity item)
                            || !TheLongTravail.MODID.equals(
                                    BuiltInRegistries.ITEM
                                            .getKey(item.getItem().getItem())
                                            .getNamespace())) {
                        return;
                    }
                    // 遵守 Jade 的物品提示禁用和隐藏设置，不修改其他提供者的内容。
                    if (tooltip.get(Identifiers.MC_ITEM_TOOLTIP).isEmpty()) {
                        return;
                    }
                    tooltip.remove(Identifiers.MC_ITEM_TOOLTIP);
                    tooltip.add(
                            Component.translatable(
                                            "tooltip.the_long_travail.jade.pick_up_for_details")
                                    .withStyle(style -> style.withColor(RainTooltips.BODY_COLOR)),
                            Identifiers.MC_ITEM_TOOLTIP);
                });
    }
}

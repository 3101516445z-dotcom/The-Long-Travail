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

@WailaPlugin
public final class TravailJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addTooltipCollectedCallback(
                (tooltip, accessor) -> {
                    if (!(accessor instanceof EntityAccessor entityAccessor)
                            || !(entityAccessor.getEntity() instanceof ItemEntity item)
                            || !TheLongTravail.MODID.equals(
                                    BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).getNamespace())) {
                        return;
                    }
                    if (tooltip.get(Identifiers.MC_ITEM_TOOLTIP).isEmpty()) {
                        return;
                    }
                    tooltip.remove(Identifiers.MC_ITEM_TOOLTIP);
                    tooltip.add(
                            Component.translatable("tooltip.the_long_travail.jade.pick_up_for_details")
                                    .withStyle(style -> style.withColor(RainTooltips.BODY_COLOR)),
                            Identifiers.MC_ITEM_TOOLTIP);
                });
    }
}

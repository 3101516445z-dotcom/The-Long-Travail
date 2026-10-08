package com.thelongtravail.client;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.item.FarReachAccessoryItem;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID, value = Dist.CLIENT)
public final class FarReachTooltips {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof FarReachAccessoryItem item)) return;
        var lines = event.getToolTip();
        for (int i = 0; i < lines.size(); i++) {
            if (!(lines.get(i).getContents() instanceof TranslatableContents text)
                    || !FarReachAccessoryItem.LOCKED_KEY.equals(text.getKey())) continue;
            var details = new ArrayList<Component>();
            item.appendDetails(details, event.getEntity());
            lines.remove(i);
            lines.addAll(i, details);
            break;
        }
    }
    private FarReachTooltips() {}
}

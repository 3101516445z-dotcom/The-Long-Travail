package com.thelongtravail.client;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.config.AzraelConfig;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.network.TooltipConfigSync;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.abyss.RainTooltips;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.math.BigDecimal;
import java.util.*;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID, value = Dist.CLIENT)
public final class AzraelTooltips {
    private static Component text(String key, Object... args) {
        int color = key.endsWith("_title") ? RainTooltips.TITLE_COLOR : RainTooltips.BODY_COLOR;
        return RainTooltips.coloredLine(Component.translatable("tooltip.the_long_travail.azrael." + key, args)
                .withStyle(s -> s.withColor(color)));
    }
    private static String percent(String key, double fallback) {
        return BigDecimal.valueOf(TooltipConfigSync.decimal("azrael." + key, fallback) * 100).stripTrailingZeros().toPlainString();
    }
    private static List<Component> forViewer(net.minecraft.world.entity.player.Player player) {
        if (player != null && !TravailCurios.stack(player).isEmpty())
            return List.of(Component.empty(), text("malice_title"),
                    text("malice", percent("malice", AzraelConfig.MALICE.get())), text("non_stacking"),
                    Component.empty(), text("witness_title"),
                    text("witness", percent("target", AzraelConfig.targetChance()), percent("self", AzraelConfig.selfChance())));
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Component.translatable("tooltip.the_long_travail.azrael.locked").withStyle(net.minecraft.ChatFormatting.DARK_RED));
        lines.add(Component.empty());
        // 与駅一致：未满足前置时仅提供独立乱码，不把真实效果文字用于混淆。
        for (int i = 0; i < 3; i++) lines.add(Component.literal("Azrael " + "?".repeat(18 - i * 4))
                .withStyle(style -> style.withColor(RainTooltips.BODY_COLOR).withObfuscated(true)));
        return List.copyOf(lines);
    }
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(ModRegistry.AZRAEL.get())) return;
        var lines = event.getToolTip();
        for (int i = 0; i < lines.size(); i++) {
            if (!(lines.get(i).getContents() instanceof TranslatableContents c)
                    || !c.getKey().equals("tooltip.the_long_travail.azrael.locked")) continue;
            lines.remove(i);
            lines.addAll(i, forViewer(event.getEntity()));
            break;
        }
    }
    private AzraelTooltips() {}
}

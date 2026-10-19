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

public final class AzraelTooltips {
    private static Component text(String key, Object... args) {
        int color = key.endsWith("_title") ? RainTooltips.TITLE_COLOR : RainTooltips.BODY_COLOR;
        return RainTooltips.coloredLine(
                Component.translatable("tooltip.the_long_travail.azrael." + key, args)
                        .withStyle(s -> s.withColor(color)));
    }

    private static String percent(String key, double fallback) {
        return BigDecimal.valueOf(TooltipConfigSync.decimal("azrael." + key, fallback) * 100)
                .stripTrailingZeros()
                .toPlainString();
    }

    public static List<Component> forViewer(net.minecraft.world.entity.player.Player player, boolean complete) {
        return List.of(
                Component.empty(),
                text("malice_title"),
                text("malice", percent("malice", AzraelConfig.MALICE.get())),
                text("non_stacking"),
                Component.empty(),
                text("witness_title"),
                text(
                        "witness",
                        percent("target", AzraelConfig.targetChance()),
                        percent("self", AzraelConfig.selfChance())));
    }

    private AzraelTooltips() {}
}

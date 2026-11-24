package com.thelongtravail.boundless;

import com.thelongtravail.text.TooltipStyle;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import java.util.*;
import java.util.function.Supplier;

public final class BoundlessTooltips {
    public static Supplier<Component> dreamKey = () -> Component.literal("Y");

    private static Component text(String key, Object... args) {
        int color = key.endsWith("_heading") ? TooltipStyle.TITLE_COLOR : TooltipStyle.BODY_COLOR;
        return TooltipStyle.coloredLine(
                Component.translatable("tooltip.the_long_travail." + key, args)
                        .withStyle(s -> s.withColor(color)));
    }

    public static List<Component> forViewer(boolean dream, Player player, Component key) {
        return forViewer(dream, player, key, false);
    }

    public static List<Component> forViewer(
            boolean dream, Player player, Component key, boolean complete) {
        List<Component> text = new ArrayList<>();
        text.add(Component.empty());
        if (dream) {
            text.add(text("free_and_easy_wandering.effect", key));
            text.add(
                    text(
                            "free_and_easy_wandering.duration",
                            BoundlessRelicText.number("dream.duration", 1)));
            text.add(text("free_and_easy_wandering.wake"));
            text.add(
                    text(
                            "free_and_easy_wandering.cooldown",
                            BoundlessRelicText.number("dream.cooldown", 1)));
        } else {
            text.add(text("star_voice.malice_heading"));
            text.add(
                    text(
                            "star_voice.malice",
                            BoundlessRelicText.number("star.maliceChance", 100),
                            BoundlessRelicText.number("star.maliceDuration", 1)));
            text.add(text("star_voice.lock"));
            text.add(Component.empty());
            text.add(text("star_voice.witness_heading"));
            text.add(
                    text(
                            "star_voice.witness",
                            BoundlessRelicText.number("star.witnessChance", 100),
                            BoundlessRelicText.number("star.witnessDuration", 1)));
            text.add(
                    text(
                            "star_voice.interval",
                            BoundlessRelicText.number("star.witnessInterval", 1)));
            text.add(
                    text(
                            "star_voice.cooldown",
                            BoundlessRelicText.number("star.witnessCooldown", 1)));
            text.add(text("star_voice.lock"));
            text.add(Component.empty());
            text.add(text("star_voice.range", BoundlessRelicText.number("star.radius", 1)));
            text.add(text("star_voice.group", BoundlessRelicText.number("star.groupLimit", 1)));
            text.add(text("star_voice.allies_heading"));
            text.add(text("star_voice.allies"));
        }
        return List.copyOf(text);
    }

    private BoundlessTooltips() {}
}

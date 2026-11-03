package com.thelongtravail.client.catalog;

import com.thelongtravail.TravailAspect;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class CatalogUtilityDetails {
    public static List<Component> aid(String id) {
        return paragraphs(id, null, null);
    }

    public static List<Component> themed(boolean stone, TravailAspect theme, ItemStack sample) {
        return paragraphs(
                stone ? "stone" : "essence",
                com.thelongtravail.client.TooltipTranslations.text(
                        "aspect.the_long_travail." + theme.id()),
                sample.getHoverName());
    }

    private static List<Component> paragraphs(String id, Component theme, Component item) {
        List<String> rows =
                switch (id) {
                    case "coloring" -> List.of("0", "1", "", "3", "4");
                    case "renewal" -> List.of("0", "3", "", "heading", "4", "1", "revealed", "6");
                    case "homecoming" -> List.of("0", "2", "", "heading", "1", "4", "5", "7");
                    case "wayguide" -> List.of("0", "1", "2", "structures", "", "4", "5", "6", "8");
                    case "stone" -> List.of("0", "1", "", "heading", "2", "persistence", "4", "5");
                    case "essence" ->
                            List.of(
                                    "0", "", "3", "4", "5", "6", "", "8", "9", "10", "11", "", "13",
                                    "14");
                    default -> throw new IllegalArgumentException("Unknown catalogue text: " + id);
                };
        List<Component> result = new ArrayList<>();
        for (String row : rows) {
            if (row.isEmpty()) {
                result.add(Component.empty());
                continue;
            }
            var line =
                    com.thelongtravail.client.TooltipTranslations.text(
                            "gui.the_long_travail.catalog.details." + id + "." + row,
                            id.equals("wayguide") && row.equals("8")
                                    ? com.thelongtravail.network.TooltipConfigSync.integer(
                                            "wayguide.radius",
                                            com.thelongtravail.config.TravailConfig.WAYGUIDE_RADIUS
                                                    .get())
                                    : theme == null ? Component.empty() : theme,
                            item == null ? Component.empty() : item);
            boolean condition = id.equals("essence") && (row.equals("3") || row.equals("8"));
            result.add(
                    line.withStyle(
                            style ->
                                    style.withColor(
                                            condition
                                                    ? CatalogTextStyle.CONDITION
                                                    : CatalogTextStyle.BODY)));
        }
        return List.copyOf(result);
    }

    private CatalogUtilityDetails() {}
}

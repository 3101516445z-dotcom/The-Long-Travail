package com.thelongtravail.client.catalog;

import com.thelongtravail.TravailAspect;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class CatalogUtilityDetails {
    public static List<Component> aid(String id) {
        return paragraphs(id, null, null);
    }

    public static List<Component> themed(boolean stone, TravailAspect theme, ItemStack sample) {
        return paragraphs(
                stone ? "stone" : "essence",
                Component.translatable("aspect.the_long_travail." + theme.id()),
                Component.translatable(sample.getDescriptionId()));
    }

    private static List<Component> paragraphs(String id, Component theme, Component item) {
        int count =
                switch (id) {
                    case "renewal" -> 7;
                    case "homecoming" -> 8;
                    case "wayguide" -> 9;
                    case "stone" -> 6;
                    case "essence" -> 15;
                    default -> throw new IllegalArgumentException("Unknown catalogue text: " + id);
                };
        List<Component> result = new ArrayList<>();
        for (int row = 0; row < count; row++) {
            boolean blank =
                    switch (id) {
                        case "renewal" -> row == 2 || row == 5;
                        case "homecoming" -> row == 3 || row == 6;
                        case "wayguide" -> row == 3 || row == 7;
                        case "stone" -> row == 3;
                        case "essence" -> row == 2 || row == 7 || row == 12;
                        default -> false;
                    };
            if (blank) {
                result.add(Component.empty());
                continue;
            }
            var line =
                    Component.translatable(
                            "gui.the_long_travail.catalog.details." + id + "." + row,
                            theme == null ? Component.empty() : theme,
                            item == null ? Component.empty() : item);
            boolean condition = id.equals("essence") && (row == 3 || row == 8);
            result.add(
                    line.withStyle(
                            style -> style.withColor(condition ? CatalogTextStyle.CONDITION : CatalogTextStyle.BODY)));
        }
        return List.copyOf(result);
    }

    private CatalogUtilityDetails() {}
}

package com.thelongtravail.client;

import net.minecraft.network.chat.Component;

public final class CarryingConditions {
    public static boolean hasNonStackingHint(String id) {
        return switch (id) {
            case "eki",
                            "tower_of_babel",
                            "golden_age",
                            "sun_chaser",
                            "little_prince",
                            "phosphophyllite",
                            "horizon_theory" ->
                    true;
            default -> false;
        };
    }

    public static String diaryWarningKey(String id) {
        if (id.endsWith("_essence")) return "requires_diary_use";
        return switch (id) {
            case "renewal", "wayguide", "homecoming" -> "requires_diary_use";
            case "little_prince", "phosphophyllite", "horizon_theory", "eki", "book_of_the_dead" ->
                    "requires_diary_effect";
            case "free_and_easy_wandering",
                            "star_voice",
                            "azrael",
                            "a_thousand_years_later",
                            "demon_king_of_the_sixth_heaven" ->
                    "requires_diary_equip";
            default -> null;
        };
    }

    public static Component diaryWarning(String key, boolean catalog) {
        var line = Component.literal(ThemeTooltips.text(key).getString());
        if (catalog)
            line.append(Component.translatable("gui.the_long_travail.catalog.abyss.period"));
        return line.withStyle(style -> style.withColor(0xFF5555));
    }

    private CarryingConditions() {}
}

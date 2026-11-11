package com.thelongtravail.text;

import net.minecraft.network.chat.Component;

public final class TooltipStyle {
    public static final int BODY_COLOR = 0xB88750;
    public static final int TITLE_COLOR = 0xFFD84D;
    public static final int ACCENT_COLOR = 0x55D6B0;
    public static final int STATE_CONDITION_COLOR = 0xC9B8FF;

    private static boolean isTravailStateCondition(String key) {
        return switch (key) {
            case "tooltip.the_long_travail.lorentz.malice_heading",
                            "tooltip.the_long_travail.lorentz.witness_heading",
                            "tooltip.the_long_travail.abyss_items.malice_heading",
                            "tooltip.the_long_travail.abyss_items.witness_heading",
                            "tooltip.the_long_travail.golden_age.malice_heading",
                            "tooltip.the_long_travail.golden_age.witness_heading",
                            "tooltip.the_long_travail.icarus.malice_heading",
                            "tooltip.the_long_travail.icarus.witness_heading",
                            "tooltip.the_long_travail.eki.prose.malice_title",
                            "tooltip.the_long_travail.eki.prose.witness_title",
                            "tooltip.the_long_travail.azrael.malice_title",
                            "tooltip.the_long_travail.azrael.witness_title",
                            "tooltip.the_long_travail.star_voice.malice_heading",
                            "tooltip.the_long_travail.star_voice.witness_heading",
                            "tooltip.the_long_travail.thousand_years.malice_title",
                            "tooltip.the_long_travail.thousand_years.witness_title",
                            "tooltip.the_long_travail.book_of_dead.witness_condition",
                            "tooltip.the_long_travail.essence.malice_heading",
                            "tooltip.the_long_travail.essence.witness_heading" ->
                    true;
            default -> false;
        };
    }

    public static Component coloredLine(Component line) {
        if (line.getContents()
                        instanceof net.minecraft.network.chat.contents.TranslatableContents text
                && isTravailStateCondition(text.getKey())) {
            return Component.literal(net.minecraft.ChatFormatting.stripFormatting(line.getString()))
                    .withStyle(line.getStyle().withColor(STATE_CONDITION_COLOR));
        }
        var result = Component.empty();
        var run = new StringBuilder();
        net.minecraft.network.chat.Style[] previous = {null};
        net.minecraft.util.StringDecomposer.iterateFormatted(
                line.getString(),
                line.getStyle(),
                (index, style, codePoint) -> {
                    var color = style.getColor();
                    if (color != null && color.getValue() == 0x55FF55)
                        style = style.withColor(ACCENT_COLOR);
                    else if (color != null && color.getValue() == 0xFFAA00)
                        style = style.withColor(TITLE_COLOR);
                    else if (color != null && color.getValue() == 0xFF55FF)
                        style = style.withColor(STATE_CONDITION_COLOR);
                    if (!style.equals(previous[0]) && run.length() > 0) {
                        result.append(Component.literal(run.toString()).withStyle(previous[0]));
                        run.setLength(0);
                    }
                    previous[0] = style;
                    run.appendCodePoint(codePoint);
                    return true;
                });
        if (run.length() > 0)
            result.append(Component.literal(run.toString()).withStyle(previous[0]));
        return result;
    }

    private TooltipStyle() {}
}

package com.thelongtravail.client.catalog;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringDecomposer;

import java.util.*;
import java.util.regex.Pattern;

// 按详情页标记术语，与段落语义染色分别处理。
public final class CatalogGlossary {
    private static final String PREFIX = "gui.the_long_travail.catalog.glossary.";
    private static final String MARKER = "catalog_glossary:";
    private static final List<String> IDS =
            List.of(
                    "direct_strike",
                    "execution",
                    "insight",
                    "critical",
                    "overlord",
                    "flowers",
                    "missing_health",
                    "separation",
                    "growth",
                    "flower_repeat",
                    "evergreen",
                    "abyss_night",
                    "random_potion",
                    "damage_bonus",
                    "gold_panning",
                    "delicacy",
                    "daytime",
                    "size_ratio",
                    "lantern_alone",
                    "lantern_enemies",
                    "lantern_undead",
                    "lantern_combat",
                    "eligible_attributes",
                    "special_breaking",
                    "souls",
                    "high_altitude");

    private record Match(int start, int end, String id) {}

    public static List<Component> annotate(List<Component> paragraphs) {
        return annotate(paragraphs, "");
    }

    public static List<Component> annotate(List<Component> paragraphs, String item) {
        Set<String> seen = new HashSet<>();
        List<Component> out = new ArrayList<>();
        for (Component paragraph : paragraphs) {
            StringBuilder text = new StringBuilder();
            List<Style> styles = new ArrayList<>();
            paragraph.visit(
                    (style, fragment) -> {
                        StringDecomposer.iterateFormatted(
                                fragment,
                                style,
                                (index, current, cp) -> {
                                    text.appendCodePoint(cp);
                                    for (int n = 0; n < Character.charCount(cp); n++)
                                        styles.add(current);
                                    return true;
                                });
                        return Optional.empty();
                    },
                    Style.EMPTY);
            List<Match> matches = new ArrayList<>();
            for (String id : IDS) {
                if (!enabled(id, item)) continue;
                for (String alias :
                        Component.translatable(PREFIX + id + ".aliases").getString().split("\\|")) {
                    if (alias.isBlank()) continue;
                    String boundary = alias.charAt(0) < 128 ? "\\b" : "";
                    var matcher =
                            Pattern.compile(
                                            boundary + Pattern.quote(alias) + boundary,
                                            Pattern.CASE_INSENSITIVE)
                                    .matcher(text);
                    while (matcher.find())
                        matches.add(new Match(matcher.start(), matcher.end(), id));
                }
            }
            matches.sort(
                    Comparator.comparingInt(Match::start)
                            .thenComparing(Comparator.comparingInt(Match::end).reversed()));
            int end = -1;
            for (Match match : matches) {
                if (match.start < end || seen.contains(match.id)) continue;
                seen.add(match.id);
                end = match.end;
                for (int i = match.start; i < match.end; i++)
                    styles.set(
                            i,
                            styles.get(i).withUnderlined(false).withInsertion(MARKER + match.id));
            }
            var line = Component.empty();
            for (int start = 0; start < text.length(); ) {
                int stop = start + 1;
                while (stop < text.length() && styles.get(start).equals(styles.get(stop))) stop++;
                line.append(
                        Component.literal(text.substring(start, stop)).setStyle(styles.get(start)));
                start = stop;
            }
            out.add(line);
        }
        return List.copyOf(out);
    }

    private static boolean enabled(String id, String item) {
        return switch (id) {
            case "damage_bonus" ->
                    Set.of(
                                    "lorentz_fantasy",
                                    "a_thousand_years_later",
                                    "demon_king_of_the_sixth_heaven",
                                    "sword_and_lantern",
                                    "eki",
                                    "midnight_lafite",
                                    "spring_corner",
                                    "affection",
                                    "mistletoe",
                                    "everlasting_flower",
                                    "icarus",
                                    "don_quixote",
                                    "sun_chaser")
                            .contains(item);
            case "lantern_alone", "lantern_enemies", "lantern_undead", "lantern_combat" ->
                    item.equals("sword_and_lantern");
            case "eligible_attributes" -> item.equals("phosphophyllite");
            case "high_altitude" -> item.equals("free_and_easy_wandering");
            case "souls" -> item.equals("myriad_souls_banner");
            case "special_breaking" -> item.equals("high_frequency_tremor");
            case "daytime" -> item.equals("icarus");
            case "size_ratio" -> item.equals("don_quixote");
            case "gold_panning", "delicacy" -> item.equals("golden_age");
            case "abyss_night" -> item.equals("midnight_lafite");
            case "random_potion" -> item.equals("liquefied_abundance");
            case "flowers" -> item.equals("spring_corner");
            case "missing_health" -> item.equals("affection");
            case "separation" -> item.equals("mistletoe");
            case "growth", "flower_repeat" -> item.equals("everlasting_flower");
            case "evergreen" -> item.equals("immortal_bud");
            default -> true;
        };
    }

    public static Component body(String id) {
        if (id.equals("lantern_enemies") || id.equals("lantern_combat"))
            return CatalogValleyDetails.card(id);
        if (id.equals("gold_panning") || id.equals("delicacy"))
            return CatalogFarReachCards.body(id);
        return id.equals("growth")
                ? CatalogFlourishingDetails.growthCard()
                : Component.translatable(PREFIX + id + ".body");
    }

    public static String term(Style style) {
        if (style == null
                || style.getInsertion() == null
                || !style.getInsertion().startsWith(MARKER)) return null;
        String id = style.getInsertion().substring(MARKER.length());
        return IDS.contains(id) ? id : null;
    }

    // 连续术语仅绘制一次下划线，线宽保持一个帧缓冲像素。
    public static void drawUnderlines(
            GuiGraphics g, Font font, net.minecraft.util.FormattedCharSequence text, int alpha) {
        List<float[]> spans = new ArrayList<>();
        float[] cursor = {0, -1};
        String[] previous = {null};
        text.accept(
                (index, style, cp) -> {
                    String id = term(style);
                    if (!Objects.equals(id, previous[0])) {
                        if (previous[0] != null) spans.add(new float[] {cursor[1], cursor[0]});
                        cursor[1] = id == null ? -1 : cursor[0];
                        previous[0] = id;
                    }
                    cursor[0] +=
                            font.width(
                                    net.minecraft.util.FormattedCharSequence.forward(
                                            new String(Character.toChars(cp)), style));
                    return true;
                });
        if (previous[0] != null) spans.add(new float[] {cursor[1], cursor[0]});
        double guiScale = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
        var matrix = g.pose().last().pose();
        float pixel = (float) (1 / (guiScale * Math.abs(matrix.m11())));
        float y =
                (float)
                        ((Math.round((matrix.m31() + font.lineHeight * matrix.m11()) * guiScale)
                                                / guiScale
                                        - matrix.m31())
                                / matrix.m11());
        for (float[] span : spans) {
            g.pose().pushPose();
            g.pose().translate(span[0], y, 0);
            g.pose().scale(span[1] - span[0], pixel, 1);
            g.fill(0, 0, 1, 1, (Math.round(alpha * 0.7F) << 24) | 0xAD8754);
            g.pose().popPose();
        }
    }

    public static void render(
            GuiGraphics g,
            Font font,
            String id,
            int mouseX,
            int mouseY,
            int width,
            int height,
            float scale) {
        int padding = 10;
        int textWidth = Math.min(210, Math.max(60, (int) ((width - 24) / scale) - padding * 2));
        var title = Component.translatable(PREFIX + id + ".title");
        var lines = new ArrayList<net.minecraft.util.FormattedCharSequence>();
        var indents = new ArrayList<Integer>();
        boolean floral = IDS.indexOf(id) >= 5;
        if (floral) {
            var paragraphs =
                    Arrays.stream(body(id).getString().split("\n", -1))
                            .map(Component::literal)
                            .map(c -> (Component) c)
                            .toList();
            // 卡片沿用原有统一文字颜色，仅保留分区与列表缩进。
            for (Component paragraph : paragraphs) {
                boolean bullet = paragraph.getString().startsWith("- ");
                // 先折行正文再添加符号，避免长列表把符号单独折到一行。
                if (bullet) {
                    var rebuilt = Component.empty();
                    int[] offset = {0};
                    paragraph.visit(
                            (style, fragment) -> {
                                int prefix =
                                        Math.min(fragment.length(), Math.max(0, 2 - offset[0]));
                                if (prefix < fragment.length())
                                    rebuilt.append(
                                            Component.literal(fragment.substring(prefix))
                                                    .withStyle(style));
                                offset[0] += fragment.length();
                                return Optional.empty();
                            },
                            Style.EMPTY);
                    paragraph = rebuilt;
                }
                if (paragraph.getString().isEmpty()) {
                    lines.add(net.minecraft.util.FormattedCharSequence.EMPTY);
                    indents.add(0);
                } else {
                    int indent = bullet ? font.width("- ") : 0;
                    var wrapped = font.split(paragraph, textWidth - indent);
                    for (int i = 0; i < wrapped.size(); i++) {
                        var row = wrapped.get(i);
                        if (bullet && i == 0)
                            row =
                                    net.minecraft.util.FormattedCharSequence.composite(
                                            net.minecraft.util.FormattedCharSequence.forward(
                                                    "- ", Style.EMPTY),
                                            row);
                        lines.add(row);
                        indents.add(i == 0 ? 0 : indent);
                    }
                }
            }
        } else lines.addAll(font.split(body(id), textWidth));
        int boxWidth = textWidth + padding * 2;
        int boxHeight = padding * 2 + font.lineHeight + 10 + lines.size() * 12;
        int visualWidth = (int) Math.ceil(boxWidth * scale);
        int visualHeight = (int) Math.ceil(boxHeight * scale);
        int x = mouseX + 12;
        int y = mouseY + 12;
        if (x + visualWidth > width - 4) x = mouseX - visualWidth - 12;
        if (y + visualHeight > height - 4) y = mouseY - visualHeight - 12;
        x = Math.max(4, Math.min(x, width - visualWidth - 4));
        y = Math.max(4, Math.min(y, height - visualHeight - 4));
        g.pose().pushPose();
        g.pose().translate(x, y, 200);
        g.pose().scale(scale, scale, 1);
        CatalogFrame.rect(g, 3, 3, boxWidth, boxHeight, 0xA0000000);
        CatalogFrame.rect(g, 0, 0, boxWidth, boxHeight, 0xFF35312C);
        CatalogFrame.outline(g, 0, 0, boxWidth, boxHeight, 0xFF796B56);
        CatalogFrame.outline(g, 1, 1, boxWidth - 2, boxHeight - 2, 0xFF211E1A);
        g.drawString(font, title, padding, padding, 0xC5B695, false);
        CatalogFrame.rect(g, padding, padding + font.lineHeight + 4, textWidth, 1, 0xFF625747);
        int lineY = padding + font.lineHeight + 10;
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(
                    font,
                    lines.get(i),
                    padding + (floral ? indents.get(i) : 0),
                    lineY,
                    0xB6AA94,
                    false);
            lineY += 12;
        }
        g.pose().popPose();
    }

    private CatalogGlossary() {}
}

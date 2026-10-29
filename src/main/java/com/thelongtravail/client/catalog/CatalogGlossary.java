package com.thelongtravail.client.catalog;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringDecomposer;

import java.util.*;
import java.util.regex.Pattern;

/** 按详情页标记术语，与段落语义染色分别处理。 */
public final class CatalogGlossary {
    private static final String PREFIX = "gui.the_long_travail.catalog.glossary.";
    private static final String MARKER = "catalog_glossary:";
    private static final List<String> IDS =
            List.of("direct_strike", "execution", "insight", "critical", "overlord");

    private record Match(int start, int end, String id) {}

    public static List<Component> annotate(List<Component> paragraphs) {
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

    public static String term(Style style) {
        if (style == null
                || style.getInsertion() == null
                || !style.getInsertion().startsWith(MARKER)) return null;
        String id = style.getInsertion().substring(MARKER.length());
        return IDS.contains(id) ? id : null;
    }

    /** 连续术语仅绘制一次下划线，线宽保持一个帧缓冲像素。 */
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
        var lines = font.split(Component.translatable(PREFIX + id + ".body"), textWidth);
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
        for (var line : lines) {
            g.drawString(font, line, padding, lineY, 0xB6AA94, false);
            lineY += 12;
        }
        g.pose().popPose();
    }

    private CatalogGlossary() {}
}

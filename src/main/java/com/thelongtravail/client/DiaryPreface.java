package com.thelongtravail.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** 卷首双页仅保存客户端阅读位置，不参与主题进度。 */
final class DiaryPreface {
    private static final Pattern EMPHASIS =
            Pattern.compile(
                    "恶意|惡意|见证|見證|石头|石頭|\\bmalice\\b|\\bwitness\\b|\\bstone\\b",
                    Pattern.CASE_INSENSITIVE);
    private final int[] scroll = new int[2], maximum = new int[2];
    private int activeColumn;

    static Component title() {
        return Component.translatable("gui.the_long_travail.diary.preface.title");
    }

    private static Component body(String text) {
        var result = Component.empty();
        var matcher = EMPHASIS.matcher(text);
        int end = 0;
        while (matcher.find()) {
            result.append(Component.literal(text.substring(end, matcher.start())));
            int color =
                    switch (matcher.group().toLowerCase(java.util.Locale.ROOT)) {
                        case "恶意", "惡意", "malice" -> 0xC83E24;
                        case "见证", "見證", "witness" -> 0x2864B5;
                        default -> 0x278044;
                    };
            result.append(
                    Component.literal(matcher.group())
                            .withStyle(style -> style.withColor(color).withBold(true)));
            end = matcher.end();
        }
        return result.append(Component.literal(text.substring(end)));
    }

    void render(GuiGraphics g, Font font, float x, float y) {
        if (y >= 112 && y < 300) {
            if (x >= 87 && x < 266) activeColumn = 0;
            else if (x >= 302 && x < 481) activeColumn = 1;
        }
        for (int column = 0; column < 2; column++) {
            Component heading =
                    column == 0
                            ? title()
                            : Component.translatable(
                                    "gui.the_long_travail.diary.preface.right_title");
            g.pose().pushPose();
            float titleScale = 2.2F;
            // 与主题标题共用视觉中心，按当前字号补偿字面高度。
            float titleY = (40 + 91) / 2F - (font.lineHeight - 1F) / 2F * titleScale + 3F;
            g.pose().translate(column == 0 ? 169 : 391.5F, titleY, 0);
            g.pose().scale(titleScale, titleScale, 1);
            g.drawString(font, heading, -font.width(heading) / 2, 0, 0x493626, false);
            g.pose().popPose();
        }
        g.fill(87, 91, 251, 92, 0x55755A38);
        g.fill(302, 91, 481, 92, 0x55755A38);
        for (int column = 0; column < 2; column++) {
            int left = column == 0 ? 87 : 302;
            float proseScale = 1.35F;
            List<FormattedCharSequence> lines = new ArrayList<>();
            List<Float> offsets = new ArrayList<>();
            float nextY = 0;
            String text =
                    Component.translatable(
                                    "gui.the_long_travail.diary.preface."
                                            + (column == 0 ? "left" : "right"))
                            .getString();
            for (String line : text.split("\n")) {
                if (line.isBlank()) {
                    nextY += 13;
                    continue;
                }
                for (var wrapped : font.split(body(line), (int) (179 / proseScale))) {
                    lines.add(wrapped);
                    offsets.add(nextY);
                    nextY += 17;
                }
            }
            int visible = 12;
            maximum[column] = Math.max(0, lines.size() - visible);
            scroll[column] = Mth.clamp(scroll[column], 0, maximum[column]);
            int end = Math.min(lines.size(), scroll[column] + visible);
            if (end > scroll[column]) {
                float first = offsets.get(scroll[column]);
                float last = offsets.get(end - 1) - first;
                float inkHeight = (font.lineHeight - 1F) * proseScale;
                float fit = Math.min(1F, (198 - inkHeight) / Math.max(1F, last));
                // 卷首没有主题副标题，短诗适当上移；长文仍使用完整正文区域。
                float lift = Math.min(12F, Math.max(0F, (198 - last * fit - inkHeight) / 2F));
                float top = (112 + 310 - last * fit - inkHeight) / 2F - 3F - lift;
                float center = column == 0 ? 169 : 391.5F;
                for (int i = scroll[column]; i < end; i++) {
                    var line = lines.get(i);
                    StringBuilder plain = new StringBuilder();
                    line.accept(
                            (index, style, cp) -> {
                                plain.appendCodePoint(cp);
                                return true;
                            });
                    float spare = Math.max(0, (179F / proseScale - font.width(line)) / 2F);
                    float offset =
                            Mth.clamp(
                                    ProseAlignment.offset(plain.toString(), font::width),
                                    -spare,
                                    spare);
                    g.pose().pushPose();
                    g.pose().translate(center, top + (offsets.get(i) - first) * fit, 0);
                    g.pose().scale(proseScale, proseScale, 1);
                    g.drawString(font, line, -font.width(line) / 2F + offset, 0, 0x493626, false);
                    g.pose().popPose();
                }
            }
            if (maximum[column] > 0) {
                int rail = left + 184;
                g.fill(rail, 112, rail + 1, 292, 0x38755A38);
                int thumb = Math.max(12, 180 * visible / lines.size());
                int offset = (180 - thumb) * scroll[column] / maximum[column];
                g.fill(rail, 112 + offset, rail + 2, 112 + offset + thumb, 0x96755A38);
            }
        }
    }

    void scroll(double x, double y, double delta) {
        if (y < 107 || y >= 305 || delta == 0) return;
        int column = x >= 87 && x < 274 ? 0 : x >= 302 && x < 489 ? 1 : -1;
        if (column < 0) return;
        activeColumn = column;
        scroll[column] = Mth.clamp(scroll[column] + (delta < 0 ? 2 : -2), 0, maximum[column]);
    }

    void key(int key) {
        int delta =
                switch (key) {
                    case 264 -> 1;
                    case 265 -> -1;
                    case 267 -> 12;
                    case 266 -> -12;
                    default -> 0;
                };
        scroll[activeColumn] = Mth.clamp(scroll[activeColumn] + delta, 0, maximum[activeColumn]);
    }
}

package com.thelongtravail.client;

import com.thelongtravail.abyss.RainTooltips;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record DiaryHintTooltip(Parts parts, int prefixWidth, int keyWidth)
        implements TooltipComponent {
    public record Parts(Component original, String prefix, Component key, String suffix) {}

    public static Parts split(Component line) {
        if (line.getContents() instanceof TranslatableContents text
                && text.getArgs().length == 1
                && text.getArgs()[0] instanceof Component key) {
            String marker = "\uFFF0";
            String pattern = Component.translatable(text.getKey(), marker).getString();
            int at = pattern.indexOf(marker);
            if (at >= 0)
                return new Parts(
                        line,
                        pattern.substring(0, at),
                        key,
                        pattern.substring(at + marker.length()));
        }
        return new Parts(line, "", null, "");
    }

    public static List<DiaryHintTooltip> layout(Font font, List<Component> lines) {
        var parts = lines.stream().map(DiaryHintTooltip::split).toList();
        int prefix =
                parts.stream()
                        .filter(p -> p.key() != null)
                        .mapToInt(p -> font.width(p.prefix()))
                        .max()
                        .orElse(0);
        int key =
                parts.stream()
                        .filter(p -> p.key() != null)
                        .mapToInt(p -> font.width(p.key()))
                        .max()
                        .orElse(0);
        return parts.stream().map(p -> new DiaryHintTooltip(p, prefix, key)).toList();
    }

    public int suffixX() {
        return prefixWidth + keyWidth;
    }

    public int width(Font font) {
        return parts.key() == null
                ? font.width(parts.original())
                : suffixX() + font.width(parts.suffix());
    }

    public static final class Renderer implements ClientTooltipComponent {
        private final DiaryHintTooltip value;

        public Renderer(DiaryHintTooltip value) {
            this.value = value;
        }

        @Override
        public int getHeight() {
            return 10;
        }

        @Override
        public int getWidth(Font font) {
            return value.width(font);
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            Parts row = value.parts();
            if (row.key() == null) {
                graphics.drawString(font, row.original(), x, y, RainTooltips.BODY_COLOR, true);
                return;
            }
            graphics.drawString(font, row.prefix(), x, y, RainTooltips.BODY_COLOR, true);
            int keyX = x + value.prefixWidth() + (value.keyWidth() - font.width(row.key())) / 2;
            graphics.drawString(font, row.key(), keyX, y, RainTooltips.ACCENT_COLOR, true);
            graphics.drawString(
                    font, row.suffix(), x + value.suffixX(), y, RainTooltips.BODY_COLOR, true);
        }
    }
}

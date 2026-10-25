package com.thelongtravail.client;

import com.thelongtravail.client.catalog.CatalogFrame;
import com.thelongtravail.item.NameColoring;
import com.thelongtravail.menu.ColoringMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

// 铜框小面板；槽位坐标与 ColoringMenu 的容器定义保持一致。
public final class ColoringScreen extends AbstractContainerScreen<ColoringMenu> {
    public ColoringScreen(ColoringMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 284;
        imageHeight = 254;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < NameColoring.COUNT; i++) {
            int x, y = 93, h = 25;
            if (i == 0) {
                x = 14;
                h = 54;
            } else if (i < 7) {
                x = 55 + (i - 1) % 3 * 41;
                y += (i - 1) / 3 * 29;
            } else if (i < 9) {
                x = 178;
                y += (i - 7) * 29;
            } else {
                x = 234;
                h = 54;
            }
            addRenderableWidget(new ChoiceButton(i, leftPos + x, topPos + y, h));
        }
        addRenderableWidget(
                new Button(
                        leftPos + 242,
                        topPos + 17,
                        17,
                        17,
                        Component.translatable("gui.close"),
                        b -> onClose(),
                        supplier -> supplier.get()) {
                    @Override
                    protected void renderWidget(GuiGraphics g, int mx, int my, float tick) {
                        CatalogFrame.close(g, getX() + 8, getY() + 8, isHoveredOrFocused());
                    }
                });
    }

    private final class ChoiceButton extends Button {
        private final int choice;
        private final java.util.List<net.minecraft.util.FormattedCharSequence> labelLines;
        private final float labelScale;
        private final int labelHeight;

        ChoiceButton(int choice, int x, int y, int height) {
            super(
                    x,
                    y,
                    36,
                    height,
                    NameColoring.previewLabel(choice).copy().withStyle(s -> s.withBold(true)),
                    b -> {
                        if (minecraft != null && minecraft.gameMode != null)
                            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, choice);
                    },
                    DEFAULT_NARRATION);
            this.choice = choice;
            int wrapWidth = choice == 9 ? 26 : 30;
            // 拉丁字母单词保持完整；中文仍按原有宽度换行。
            var words =
                    java.util.regex.Pattern.compile("[A-Za-z]+").matcher(getMessage().getString());
            while (words.find()) {
                wrapWidth =
                        Math.max(
                                wrapWidth,
                                font.width(
                                        Component.literal(words.group())
                                                .setStyle(getMessage().getStyle())));
            }
            var lines = font.split(getMessage(), wrapWidth);
            int maxLines = height == 25 ? 2 : 4;
            while (lines.size() > maxLines) {
                lines = font.split(getMessage(), ++wrapWidth);
            }
            labelLines = lines;
            labelHeight = Math.max(font.lineHeight, (lines.size() - 1) * 12 + font.lineHeight);
            int widest = lines.stream().mapToInt(font::width).max().orElse(1);
            // 四周留出边框与光晕空间，只缩小，不放大短标签。
            labelScale =
                    Math.min(
                            1.0F,
                            Math.min(30.0F / Math.max(1, widest), (height - 6.0F) / labelHeight));
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float tick) {
            int x = getX(), y = getY(), h = getHeight();
            boolean selected = menu.selection() == choice;
            g.fillGradient(
                    x,
                    y,
                    x + 36,
                    y + h,
                    isHoveredOrFocused() ? 0xFF372C22 : 0xFF29221C,
                    0xFF211B17);
            g.renderOutline(
                    x, y, 36, h, selected || isHoveredOrFocused() ? 0xFFD2B17A : 0xFF665039);
            g.fill(x + 2, y + 1, x + 34, y + 2, selected ? 0x66080605 : 0x2EC5A775);
            g.fill(x + 2, y + h - 2, x + 34, y + h - 1, selected ? 0x26C5A775 : 0x6B080605);
            if (selected) {
                g.fill(x + 3, y + 3, x + 7, y + 4, 0xFFD2B17A);
                g.fill(x + 3, y + 3, x + 4, y + 7, 0xFFD2B17A);
                g.fill(x + 29, y + h - 4, x + 33, y + h - 3, 0xFFD2B17A);
                g.fill(x + 32, y + h - 7, x + 33, y + h - 3, 0xFFD2B17A);
            }
            g.pose().pushPose();
            g.pose().translate(x + 18.0F, y + (h - labelHeight * labelScale) / 2.0F, 0);
            g.pose().scale(labelScale, labelScale, 1.0F);
            int yy = 0;
            for (var line : labelLines) {
                g.drawString(font, line, -font.width(line) / 2, yy, 0xDDD3B5, false);
                yy += 12;
            }
            g.pose().popPose();
        }
    }

    private void rect(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(leftPos + x, topPos + y, leftPos + x + w, topPos + y + h, color);
    }

    private void well(GuiGraphics g, int x, int y, int w, int h) {
        g.fillGradient(
                leftPos + x, topPos + y, leftPos + x + w, topPos + y + h, 0xFF100E0C, 0xFF241E19);
        g.renderOutline(leftPos + x, topPos + y, w, h, 0xFF665039);
        rect(g, x + 1, y + 1, w - 2, 1, 0xAA080605);
        rect(g, x + 1, y + 1, 1, h - 2, 0xAA080605);
        rect(g, x + 1, y + h - 2, w - 2, 1, 0x55B39668);
    }

    private void diamond(GuiGraphics g, int x, int y) {
        rect(g, x, y - 2, 1, 5, 0xFFB99A68);
        rect(g, x - 1, y - 1, 3, 3, 0xFFB99A68);
        rect(g, x - 2, y, 5, 1, 0xFFB99A68);
    }

    @Override
    protected void renderBg(GuiGraphics g, float tick, int mx, int my) {
        CatalogFrame.draw(g, width, height, leftPos, topPos, imageWidth, imageHeight);
        rect(g, 58, 25, 58, 1, 0xFF665039);
        rect(g, 168, 25, 58, 1, 0xFF665039);
        diamond(g, 53, 25);
        diamond(g, 231, 25);
        for (int x : new int[] {75, 181}) {
            rect(g, x, 41, 28, 28, 0xFF201A16);
            well(g, x + 3, 44, 22, 22);
            for (int dx : new int[] {0, 20}) {
                rect(g, x + dx, 41, 8, 1, 0xFF89704C);
                rect(g, x + dx, 68, 8, 1, 0xFF89704C);
            }
            for (int dx : new int[] {0, 27}) {
                rect(g, x + dx, 41, 1, 8, 0xFF89704C);
                rect(g, x + dx, 61, 1, 8, 0xFF89704C);
            }
        }
        rect(g, 118, 55, 44, 1, 0xFF806747);
        for (int i = 0; i < 5; i++) rect(g, 161 + i, 53 + i / 2, 1, 5 - 2 * (i / 2), 0xFF806747);
        rect(g, 40, 84, 174, 1, 0xFF665039);
        rect(g, 258, 84, 12, 1, 0xFF665039);
        rect(g, 223, 93, 1, 54, 0xFF665039);
        rect(g, 225, 93, 1, 54, 0xFF665039);
        rect(g, 61, 156, 162, 1, 0xFF665039);
        rect(g, 57, 159, 170, 82, 0x45080605);
        g.renderOutline(leftPos + 57, topPos + 159, 170, 82, 0xFF3B3024);
        for (int i = 2; i < menu.slots.size(); i++) {
            var slot = menu.slots.get(i);
            well(g, slot.x - 1, slot.y - 1, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, 21, 0xDDCAA8, false);
        g.drawString(
                font,
                Component.translatable("gui.the_long_travail.coloring.names"),
                14,
                80,
                0xA28C6C,
                false);
        g.drawString(
                font,
                Component.translatable("gui.the_long_travail.coloring.clear"),
                234,
                80,
                0xA28C6C,
                false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float tick) {
        renderBackground(g);
        super.render(g, mx, my, tick);
        renderTooltip(g, mx, my);
    }
}

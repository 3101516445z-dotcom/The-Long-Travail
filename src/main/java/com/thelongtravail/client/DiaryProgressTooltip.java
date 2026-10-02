package com.thelongtravail.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record DiaryProgressTooltip(double progress, int hintWidth) implements TooltipComponent {
    public static final class Renderer implements ClientTooltipComponent {
        private static final int EMPTY = 0xFF353535, FILLED = 0xFF858585;
        private static final int TOP_PADDING = 3, BAR_HEIGHT = 3, BOTTOM_PADDING = 5;
        private final DiaryProgressTooltip value;

        public Renderer(DiaryProgressTooltip value) { this.value = value; }

        @Override
        public int getHeight() { return TOP_PADDING + BAR_HEIGHT + BOTTOM_PADDING; }

        @Override
        public int getWidth(Font font) { return Math.max(1, value.hintWidth()); }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            int width = getWidth(font);
            int filled = (int) Math.floor(Math.max(0, Math.min(1, value.progress())) * width);
            int top = y + TOP_PADDING;
            graphics.fill(x, top, x + width, top + BAR_HEIGHT, EMPTY);
            if (filled > 0) graphics.fill(x, top, x + filled, top + BAR_HEIGHT, FILLED);
        }
    }
}

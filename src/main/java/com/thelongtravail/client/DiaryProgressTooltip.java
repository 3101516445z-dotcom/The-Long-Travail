package com.thelongtravail.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record DiaryProgressTooltip(double progress, int hintWidth) implements TooltipComponent {
    public static final class Renderer implements ClientTooltipComponent {
        private static final int TOP_PADDING = 3,
                BAR_HEIGHT = ThinProgressBar.HEIGHT,
                BOTTOM_PADDING = 5;
        private final DiaryProgressTooltip value;

        public Renderer(DiaryProgressTooltip value) {
            this.value = value;
        }

        @Override
        public int getHeight() {
            return TOP_PADDING + BAR_HEIGHT + BOTTOM_PADDING;
        }

        @Override
        public int getWidth(Font font) {
            return Math.max(1, value.hintWidth());
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            ThinProgressBar.render(graphics, x, y + TOP_PADDING, getWidth(font), value.progress());
        }
    }
}

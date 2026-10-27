package com.thelongtravail.client;

import net.minecraft.client.gui.GuiGraphics;

public final class ThinProgressBar {
    public static final int HEIGHT = 3;
    private static final int EMPTY = 0xFF353535, FILLED = 0xFF858585;

    public static void render(GuiGraphics graphics, int x, int y, int width, double progress) {
        width = Math.max(1, width);
        int filled = (int) Math.floor(Math.max(0, Math.min(1, progress)) * width);
        graphics.fill(x, y, x + width, y + HEIGHT, EMPTY);
        if (filled > 0) graphics.fill(x, y, x + filled, y + HEIGHT, FILLED);
    }

    private ThinProgressBar() {}
}

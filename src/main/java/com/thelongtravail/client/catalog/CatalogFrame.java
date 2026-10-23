package com.thelongtravail.client.catalog;

import net.minecraft.client.gui.GuiGraphics;

final class CatalogFrame {
    static void fadeEdges(
            GuiGraphics g,
            int left,
            int top,
            int right,
            int bottom,
            int size,
            boolean above,
            boolean below) {
        g.flush();
        var overlay = net.minecraft.client.renderer.RenderType.guiOverlay();
        if (above) g.fillGradient(overlay, left, top, right, top + size, 0xFF27211E, 0x0027211E, 0);
        if (below)
            g.fillGradient(overlay, left, bottom - size, right, bottom, 0x0027211E, 0xFF27211E, 0);
        g.flush();
    }

    static void rect(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        rect(g, x, y, w, 1, color);
        rect(g, x, y + h - 1, w, 1, color);
        rect(g, x, y, 1, h, color);
        rect(g, x + w - 1, y, 1, h, color);
    }

    static void diamond(GuiGraphics g, int x, int y, int color) {
        rect(g, x, y - 2, 1, 5, color);
        rect(g, x - 1, y - 1, 3, 3, color);
        rect(g, x - 2, y, 5, 1, color);
    }

    static void emblem(GuiGraphics g, int x, int y, int color) {
        diamond(g, x, y, color);
        rect(g, x, y - 5, 1, 11, color);
        rect(g, x - 4, y, 9, 1, color);
        rect(g, x - 15, y, 7, 1, 0xFF725638);
        rect(g, x + 9, y, 7, 1, 0xFF725638);
        rect(g, x, y, 1, 1, 0xFFE5C58A);
    }

    static void corner(GuiGraphics g, int x, int y, int sx, int sy) {
        for (int i = 0; i < 24; i++) {
            int horizontal = sy > 0 ? 0xFFC4A06A : 0xFF896640;
            int vertical = sx > 0 ? 0xFFB18D58 : 0xFF785B3B;
            if (i > 18) {
                horizontal = sy > 0 ? 0xFFA28253 : 0xFF705335;
                vertical = sx > 0 ? 0xFF937144 : 0xFF684D32;
            }
            rect(g, x + sx * i, y, 1, 1, horizontal);
            rect(g, x, y + sy * i, 1, 1, vertical);
            if (i < 17) {
                rect(g, x + sx * i, y + sy * 3, 1, 1, 0xFF705237);
                rect(g, x + sx * 3, y + sy * i, 1, 1, 0xFF705237);
            }
        }
        diamond(g, x + sx * 6, y + sy * 6, 0xFFAD8957);
        int rivetX = x + sx * 6, rivetY = y + sy * 6;
        rect(g, rivetX - 1, rivetY - 1, 2, 1, 0xFFD2B17A);
        rect(g, rivetX + 1, rivetY, 1, 2, 0xFF725033);
        rect(g, rivetX, rivetY, 1, 1, 0xFFBD965E);
    }

    static void arrow(GuiGraphics g, int x, int y, boolean up) {
        for (int i = 0; i < 3; i++) rect(g, x - i, y + (up ? i : -i), i * 2 + 1, 1, 0xFFAC8A5D);
    }

    static void draw(
            GuiGraphics g,
            int width,
            int height,
            int left,
            int top,
            int panelWidth,
            int panelHeight) {
        g.fill(0, 0, width, height, 0xAA09090D);
        rect(g, left + 5, top + 7, panelWidth, panelHeight, 0x65000000);
        rect(g, left, top, panelWidth, panelHeight, 0xFF171313);
        outline(g, left, top, panelWidth, panelHeight, 0xFF8A6A47);
        outline(g, left + 3, top + 3, panelWidth - 6, panelHeight - 6, 0xFF3B2E25);
        rect(g, left + 7, top + 7, panelWidth - 14, panelHeight - 14, 0xFF27211E);
        for (int y = top + 10; y < top + panelHeight - 10; y += 4)
            for (int x = left + 10; x < left + panelWidth - 10; x += 7)
                if (((x * 13 + y * 7) & 15) < 5) rect(g, x, y, 2, 1, 0x103D3025);
        for (int x = left + 40; x < left + panelWidth - 40; x += 8) {
            rect(g, x, top + 5, 3, 1, 0xFF584330);
            rect(g, x, top + panelHeight - 6, 3, 1, 0xFF584330);
        }
        for (int y = top + 40; y < top + panelHeight - 40; y += 8) {
            rect(g, left + 5, y, 1, 3, 0xFF584330);
            rect(g, left + panelWidth - 6, y, 1, 3, 0xFF584330);
        }
        corner(g, left + 10, top + 10, 1, 1);
        corner(g, left + panelWidth - 11, top + 10, -1, 1);
        corner(g, left + 10, top + panelHeight - 11, 1, -1);
        corner(g, left + panelWidth - 11, top + panelHeight - 11, -1, -1);
    }

    static void close(GuiGraphics g, int cx, int y, boolean hover) {
        if (hover) {
            rect(g, cx - 8, y - 8, 17, 17, 0xFF211B17);
            outline(g, cx - 8, y - 8, 17, 17, 0xFF55412E);
        }
        for (int i = 0; i < 7; i++) {
            int c = hover ? 0xFFE2C18A : (i < 3 ? 0xFFB29365 : 0xFF8E6C43);
            rect(g, cx - 3 + i, y - 3 + i, 1, 1, c);
            rect(g, cx + 3 - i, y - 3 + i, 1, 1, c);
        }
    }
}

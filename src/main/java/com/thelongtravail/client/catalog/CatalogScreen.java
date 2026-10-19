package com.thelongtravail.client.catalog;

import com.thelongtravail.TravailAspect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import static com.thelongtravail.client.catalog.CatalogFrame.*;
import static com.thelongtravail.client.catalog.CatalogEntries.*;

public final class CatalogScreen extends Screen {
    private final CatalogSession session;
    // 鼠标、页面和渲染变换均使用拾遗局部坐标；避让边界也须使用同一坐标系。
    private final net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner tooltipPositioner =
            (screenWidth, screenHeight, x, y, tooltipWidth, tooltipHeight) ->
                    net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE
                            .positionTooltip(width, height, x, y, tooltipWidth, tooltipHeight);
    private EditBox search;
    private String query = "", selectedId = "";
    private List<Entry> results = List.of();
    private final List<Card> cards = new ArrayList<>();
    private int left, top, pw, ph, bodyTop, bodyBottom, contentHeight, columnWidth, essenceY, themeY;
    private double scroll, grab;
    private int resultFirst, resultSelected;
    private boolean dropdown, dragging;
    private long highlightUntil, marqueeStarted;
    private String marqueeId = "";

    private record Card(Entry entry, int x, int y, int w, int h) {}

    public CatalogScreen(CatalogSession session) {
        super(Component.translatable("gui.the_long_travail.catalog.title"));
        this.session = session;
    }

    public double scrollPosition() {
        return scroll;
    }

    public String query() {
        return query;
    }

    public String selectedId() {
        return selectedId;
    }

    public int maxScroll() {
        return Math.max(0, contentHeight - (bodyBottom - bodyTop));
    }

    public int[] cardCenter(String id) {
        Card c = cards.stream().filter(v -> v.entry.id().equals(id)).findFirst().orElseThrow();
        return new int[] {left + 29 + c.x + c.w / 2, bodyTop + c.y - (int) scroll + c.h / 2};
    }

    private static net.minecraft.network.chat.MutableComponent ui(String key) {
        return Component.translatable("gui.the_long_travail.catalog." + key);
    }

    @Override
    protected void init() {
        pw = 480;
        ph = 360;
        left = (width - pw) / 2;
        top = (height - ph) / 2;
        bodyTop = top + 78;
        bodyBottom = top + ph - 43;
        search = new EditBox(font, left + pw - 223, top + 23 + (26 - 8) / 2, 171, 8, ui("search"));
        search.setBordered(false);
        search.setTextColor(0xDDCAA8);
        search.setMaxLength(80);
        search.setHint(ui("search_hint").withStyle(s -> s.withColor(0x786854)));
        search.setValue(query);
        search.setResponder(
                text -> {
                    query = text;
                    results = CatalogEntries.search(text);
                    resultFirst = resultSelected = 0;
                    dropdown = !text.isBlank();
                    marqueeId = "";
                });
        addWidget(search);
        cards.clear();
        int available = pw - 76, gap = 6;
        columnWidth = (available - gap * 5) / 6;
        int size = Math.min(40, columnWidth - 8);
        List<Entry> aids = all().stream().filter(e -> e.category() == Category.AID).toList();
        int aidWidth = (available - 12) / 3;
        for (int i = 0; i < aids.size(); i++)
            cards.add(new Card(aids.get(i), i * (aidWidth + 6) + (aidWidth - size) / 2, 22, size, size));
        for (Entry e : all())
            if (e.category() == Category.STONE)
                cards.add(
                        new Card(
                                e,
                                e.theme().ordinal() * (columnWidth + gap) + (columnWidth - size) / 2,
                                96,
                                size,
                                size));
        essenceY = 170;
        themeY = 246;
        for (Entry e : all())
            if (e.category() == Category.ESSENCE)
                cards.add(
                        new Card(
                                e,
                                e.theme().ordinal() * (columnWidth + gap) + (columnWidth - size) / 2,
                                essenceY,
                                size,
                                size));
        var columns =
                Arrays.stream(TravailAspect.values())
                        .map(
                                aspect ->
                                        all().stream()
                                                .filter(e -> e.category() == Category.THEME && e.theme() == aspect)
                                                .toList())
                        .toList();
        int maxRows = columns.stream().mapToInt(List::size).max().orElse(0);
        for (int row = 0; row < maxRows; row++)
            for (int col = 0; col < columns.size(); col++)
                if (row < columns.get(col).size())
                    cards.add(
                            new Card(
                                    columns.get(col).get(row),
                                    col * (columnWidth + gap) + (columnWidth - size) / 2,
                                    themeY + row * (size + 8),
                                    size,
                                    size));
        contentHeight = themeY + maxRows * (size + 8) + 5;
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    public void setQuery(String text) {
        search.setValue(text);
        search.setFocused(true);
    }

    public void locate(String id) {
        Card c = cards.stream().filter(v -> v.entry.id().equals(id)).findFirst().orElseThrow();
        scroll = Mth.clamp(c.y + c.h / 2.0 - (bodyBottom - bodyTop) / 2.0, 0, maxScroll());
        selectedId = id;
        highlightUntil = System.currentTimeMillis() + 1800;
        dropdown = false;
        search.setFocused(false);
    }

    public void collapseSearch() {
        dropdown = false;
        if (search != null) search.setFocused(false);
    }

    private boolean in(double x, double y, int rx, int ry, int w, int h) {
        return x >= rx && x < rx + w && y >= ry && y < ry + h;
    }

    private int railX() {
        return left + pw - 28;
    }

    private int thumbHeight() {
        int v = bodyBottom - bodyTop;
        return Math.min(v, Math.max(26, v * v / Math.max(1, contentHeight)));
    }

    private int thumbY() {
        return bodyTop + (int) (scroll / Math.max(1, maxScroll()) * (bodyBottom - bodyTop - thumbHeight()));
    }

    private boolean overDropdown(double x, double y) {
        return dropdown
                && in(x, y, search.getX() - 8, top + 53, 196, Math.max(1, Math.min(5, results.size())) * 32 + 6);
    }

    private void section(GuiGraphics g, String label, int y) {
        int sy = bodyTop + y - (int) scroll;
        g.drawString(font, label, left + 30, sy, 0xBAA17B, false);
        rect(g, left + 36 + font.width(label), sy + 5, pw - 85 - font.width(label), 1, 0xFF4C3B2B);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        CatalogFrame.draw(g, width, height, left, top, pw, ph);
        g.pose().pushPose();
        g.pose().translate(left + 30, top + 28, 0);
        g.pose().scale(1.6f, 1.6f, 1);
        g.drawString(font, ui("title").copy().withStyle(s -> s.withBold(true)), 0, 0, 0xC5A879, false);
        g.pose().popPose();
        g.drawString(font, ui("subtitle"), left + 31, top + 51, 0x85735C, false);
        rect(g, search.getX() - 8, top + 23, 196, 26, 0xFF201B18);
        outline(g, search.getX() - 8, top + 23, 196, 26, search.isFocused() ? 0xFF9E7B50 : 0xFF584330);
        rect(g, search.getX() - 7, top + 24, 194, 1, 0xFF171310);
        search.render(g, mx, my, partial);
        if (!query.isEmpty()) g.drawString(font, "×", search.getX() + 178, top + 23 + (26 - 8) / 2, 0xB49A74, false);
        CatalogFrame.close(g, railX() + 2, top + 32, in(mx, my, railX() - 6, top + 24, 17, 17));
        rect(g, left + 29, top + 68, pw - 58, 1, 0xFF5D4934);
        session.scissor(g, left + 27, bodyTop, left + pw - 43, bodyBottom);
        section(g, Category.AID.label(), 2);
        section(g, Category.STONE.label(), 76);
        section(g, Category.ESSENCE.label(), 150);
        for (TravailAspect aspect : TravailAspect.values()) {
            int x = left + 29 + aspect.ordinal() * (columnWidth + 6), y = bodyTop + themeY - 10 - (int) scroll;
            rect(g, x, y, columnWidth, contentHeight - themeY + 10, 0xFF231E1B);
            outline(g, x, y, columnWidth, contentHeight - themeY + 10, 0xFF4D3B2D);
            rect(g, x + columnWidth / 2, y - 8, 1, 8, 0xFF80613F);
            diamond(g, x + columnWidth / 2, y, 0xFF937149);
        }
        Card hover = null;
        for (Card c : cards) {
            int x = left + 29 + c.x, y = bodyTop + c.y - (int) scroll;
            if (y + c.h < bodyTop || y > bodyBottom) continue;
            boolean over =
                    !overDropdown(mx, my)
                            && my >= bodyTop + (scroll >= essenceY + 50 ? 18 : 0)
                            && my < bodyBottom
                            && in(mx, my, x, y, c.w, c.h);
            boolean flash = c.entry.id().equals(selectedId) && System.currentTimeMillis() < highlightUntil;
            if (over) hover = c;
            boolean essence = c.entry.category() == Category.ESSENCE;
            rect(g, x, y, c.w, c.h, over ? 0xFF33271E : 0xFF1D1917);
            outline(g, x, y, c.w, c.h, over || flash ? 0xFFD1AD73 : 0xFF816140);
            outline(g, x + 3, y + 3, c.w - 6, c.h - 6, 0xFF3E3026);
            rect(g, x + 1, y + 1, c.w - 2, 1, over ? 0xFFB18A56 : 0xFF6B5035);
            rect(g, x + 1, y + c.h - 2, c.w - 2, 1, 0xFF120F0D);
            int iconSize = c.w >= 40 ? 32 : 24;
            var offset = CatalogIconAlignment.offset(c.entry.stack());
            g.pose().pushPose();
            g.pose()
                    .translate(
                            x + (c.w - iconSize) / 2f + offset.x() * iconSize,
                            y + (c.h - iconSize) / 2f + offset.y() * iconSize,
                            0);
            g.pose().scale(iconSize / 16f, iconSize / 16f, 1);
            g.renderItem(c.entry.stack(), 0, 0);
            g.pose().popPose();
            if (essence) {
                int cx = x + c.w / 2;
                diamond(g, cx, y - 2, 0xFFC09A61);
                g.drawCenteredString(font, Component.literal(themeName(c.entry.theme())), cx, y + c.h + 8, 0xC5AB80);
            }
        }
        g.disableScissor();
        if (scroll >= essenceY + 50) {
            g.flush();
            g.pose().pushPose();
            g.pose().translate(0, 0, 220);
            rect(g, left + 27, bodyTop, pw - 70, 18, 0xFF27211E);
            for (TravailAspect a : TravailAspect.values()) {
                int x = left + 29 + a.ordinal() * (columnWidth + 6);
                g.drawCenteredString(font, Component.literal(themeName(a)), x + columnWidth / 2, bodyTop + 3, 0xC5AB80);
                rect(g, x, bodyTop + 16, columnWidth, 1, 0xFF6A5034);
            }
            g.flush();
            g.pose().popPose();
        }
        fadeEdges(g, left + 27, bodyTop, left + pw - 43, bodyBottom, 8, scroll > 0, scroll < maxScroll());
        int rx = railX(), ty = thumbY(), th = thumbHeight();
        rect(g, rx, bodyTop, 5, bodyBottom - bodyTop, 0xFF151211);
        rect(g, rx + 2, bodyTop, 1, bodyBottom - bodyTop, 0xFF4A3829);
        rect(g, rx - 2, bodyTop, 1, bodyBottom - bodyTop, 0xFF49392A);
        rect(g, rx, ty, 5, th, 0xFF9D7B50);
        rect(g, rx + 1, ty + 1, 1, th - 2, 0xFFD2AE76);
        for (int dy : new int[] {-3, 0, 3}) rect(g, rx + 1, ty + th / 2 + dy, 3, 1, 0xFF513E2C);
        if (scroll > 0) arrow(g, rx + 2, bodyTop - 6, true);
        if (scroll < maxScroll()) arrow(g, rx + 2, bodyBottom + 7, false);
        int fy = top + ph - 29;
        rect(g, left + 29, fy - 7, pw - 58, 1, 0xFF493A2B);
        emblem(g, left + pw / 2, fy + 4, 0xFF806341);
        g.drawString(font, ui("browse"), left + 33, fy, 0x9E886C, false);
        g.drawString(font, ui("exit"), left + pw - 33 - font.width(ui("exit")), fy, 0x9E886C, false);
        if (dropdown) renderResults(g, mx, my);
        else marqueeId = "";
        if (!dropdown && hover != null) {
            List<Component> tip =
                    List.of(
                            hover.entry.stack().getHoverName(),
                            ui("view_details").copy().withStyle(s -> s.withColor(0xB49A74)));
            g.renderTooltip(font, tip.stream().map(Component::getVisualOrderText).toList(), tooltipPositioner, mx, my);
        }
    }

    private void renderResults(GuiGraphics g, int mx, int my) {
        int x = search.getX() - 8, y = top + 53, n = Math.max(1, Math.min(5, results.size()));
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        rect(g, x + 3, y + 4, 196, n * 32 + 6, 0xA0000000);
        rect(g, x, y, 196, n * 32 + 6, 0xFF201B18);
        outline(g, x, y, 196, n * 32 + 6, 0xFF97744C);
        if (results.isEmpty()) g.drawString(font, ui("no_results"), x + 12, y + 13, 0xA28B6D, false);
        int active = resultSelected;
        for (int i = 0; i < Math.min(5, results.size() - resultFirst); i++)
            if (in(mx, my, x + 1, y + 3 + i * 32, 194, 32)) active = resultFirst + i;
        String activeId =
                active >= resultFirst && active < Math.min(results.size(), resultFirst + 5)
                        ? results.get(active).id()
                        : "";
        if (!activeId.equals(marqueeId)) {
            marqueeId = activeId;
            marqueeStarted = System.nanoTime();
        }
        for (int i = 0; i < Math.min(5, results.size() - resultFirst); i++) {
            int index = resultFirst + i, ry = y + 3 + i * 32;
            Entry e = results.get(index);
            if (index == active) rect(g, x + 2, ry, 192, 31, 0xFF392B20);
            g.renderItem(e.stack(), x + 8, ry + 8);
            drawResultName(g, e.stack().getHoverName(), x + 31, ry + 5, 154, index == active);
            g.drawString(font, font.plainSubstrByWidth(e.subtitle(), 154), x + 31, ry + 18, 0x8F7B61, false);
            if (i > 0) rect(g, x + 7, ry, 181, 1, 0xFF423225);
        }
        if (results.size() > 5) {
            int h = Math.max(8, 160 * 5 / results.size());
            rect(g, x + 192, y + 3 + resultFirst * (160 - h) / Math.max(1, results.size() - 5), 2, h, 0xFFAD8957);
        }
        g.flush();
        g.pose().popPose();
    }

    private void drawResultName(GuiGraphics g, Component name, int x, int y, int width, boolean active) {
        int textWidth = font.width(name), period = textWidth + 24;
        double offset = 0;
        if (active && textWidth > width) {
            double elapsed = (System.nanoTime() - marqueeStarted) / 1_000_000_000.0;
            double phase = elapsed % (0.9 + period / 24.0);
            offset = Math.max(0, phase - 0.9) * 24;
        }
        g.flush();
        session.scissor(g, x, y - 1, x + width, y + font.lineHeight + 2);
        g.drawString(font, name, x - (int) offset, y, 0xD8C09A, false);
        if (active && textWidth > width) g.drawString(font, name, x - (int) offset + period, y, 0xD8C09A, false);
        g.flush();
        g.disableScissor();
        if (textWidth > width) {
            int background = active ? 0x392B20 : 0x201B18;
            for (int i = 0; i < 7; i++) {
                int alpha = (7 - i) * 255 / 7;
                if (offset > 0) rect(g, x + i, y - 1, 1, font.lineHeight + 2, (alpha << 24) | background);
                rect(g, x + width - 1 - i, y - 1, 1, font.lineHeight + 2, (alpha << 24) | background);
            }
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            onClose();
            return true;
        }
        if (button != 0) return false;
        if (in(x, y, railX() - 6, top + 24, 17, 17)) {
            onClose();
            return true;
        }
        if (in(x, y, search.getX() + 174, top + 24, 16, 24)) {
            search.setValue("");
            return true;
        }
        if (overDropdown(x, y)) {
            int i = resultFirst + (int) (y - top - 56) / 32;
            if (i >= 0 && i < results.size()) locate(results.get(i).id());
            return true;
        }
        if (in(x, y, search.getX() - 8, top + 23, 174, 26)) {
            search.setFocused(true);
            search.mouseClicked(x, y, button);
            dropdown = !query.isBlank();
            return true;
        }
        collapseSearch();
        if (in(x, y, railX() - 4, bodyTop, 13, bodyBottom - bodyTop) && maxScroll() > 0) {
            if (y >= thumbY() && y <= thumbY() + thumbHeight()) {
                dragging = true;
                grab = y - thumbY();
            } else scroll = Mth.clamp(scroll + (y < thumbY() ? -1 : 1) * (bodyBottom - bodyTop), 0, maxScroll());
            return true;
        }
        if (y >= bodyTop + (scroll >= essenceY + 50 ? 18 : 0) && y < bodyBottom)
            for (Card c : cards)
                if (in(x, y, left + 29 + c.x, bodyTop + c.y - (int) scroll, c.w, c.h)) {
                    selectedId = c.entry.id();
                    session.detail(c.entry, c.entry.stack());
                    return true;
                }
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (overDropdown(x, y)) {
            resultFirst = Mth.clamp(resultFirst - (int) delta, 0, Math.max(0, results.size() - 5));
            return true;
        }
        scroll = Mth.clamp(scroll - delta * 28, 0, maxScroll());
        return true;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (dragging)
            scroll =
                    Mth.clamp(
                            (y - bodyTop - grab) / Math.max(1, bodyBottom - bodyTop - thumbHeight()) * maxScroll(),
                            0,
                            maxScroll());
        else if (search.isFocused()) search.mouseDragged(x, y, button, dx, dy);
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int b) {
        dragging = false;
        return true;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return search.charTyped(c, modifiers);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (dropdown && !results.isEmpty()) {
            if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_UP) {
                resultSelected =
                        Mth.clamp(resultSelected + (key == GLFW.GLFW_KEY_DOWN ? 1 : -1), 0, results.size() - 1);
                resultFirst = Mth.clamp(resultFirst, Math.max(0, resultSelected - 4), resultSelected);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                locate(results.get(resultSelected).id());
                return true;
            }
        }
        if (search.isFocused()) {
            search.keyPressed(key, scan, mods);
            return true;
        }
        double amount =
                switch (key) {
                    case GLFW.GLFW_KEY_DOWN -> 18;
                    case GLFW.GLFW_KEY_UP -> -18;
                    case GLFW.GLFW_KEY_PAGE_DOWN -> bodyBottom - bodyTop;
                    case GLFW.GLFW_KEY_PAGE_UP -> bodyTop - bodyBottom;
                    case GLFW.GLFW_KEY_HOME -> -contentHeight;
                    case GLFW.GLFW_KEY_END -> contentHeight;
                    default -> 0;
                };
        scroll = Mth.clamp(scroll + amount, 0, maxScroll());
        return true;
    }

    @Override
    public void tick() {
        search.tick();
    }

    @Override
    public void onClose() {
        session.close();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

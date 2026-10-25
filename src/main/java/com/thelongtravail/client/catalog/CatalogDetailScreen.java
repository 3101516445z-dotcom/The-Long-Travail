package com.thelongtravail.client.catalog;

import static com.thelongtravail.client.catalog.CatalogFrame.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class CatalogDetailScreen extends Screen {
    private static final int WRAPPED_LINE_STEP = 12;
    private static final int SENTENCE_STEP = 14;
    private static final int PARAGRAPH_EXTRA_GAP = 10;
    private final CatalogSession session;
    private final boolean fromParent;
    private final ItemStack stack;
    private final List<Component> paragraphs;
    private final List<Row> rows = new ArrayList<>();
    private int left, top, panelWidth, panelHeight, bodyTop, bodyBottom, contentHeight;
    private double scroll, grabOffset;
    private boolean dragging;
    private float bodyTextScale;

    private record Row(FormattedCharSequence text, int y) {}

    public CatalogDetailScreen(
            CatalogSession session, ItemStack stack, List<Component> paragraphs) {
        super(stack.getHoverName());
        this.session = session;
        this.fromParent = session.parent() != null;
        this.stack = stack.copy();
        this.paragraphs = List.copyOf(paragraphs);
    }

    @Override
    protected void init() {
        panelWidth = 480;
        panelHeight = 360;
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        bodyTop = top + 91;
        bodyBottom = top + panelHeight - 43;
        bodyTextScale = session.bodyTextScale();
        rows.clear();
        int y = 0;
        for (Component paragraph : paragraphs) {
            if (paragraph.getString().isEmpty()) {
                if (y > 0) y += Math.round(PARAGRAPH_EXTRA_GAP * bodyTextScale);
                continue;
            }
            for (FormattedCharSequence line :
                    wrapParagraph(
                            paragraph, Math.max(1, (int) ((panelWidth - 88) / bodyTextScale)))) {
                rows.add(new Row(line, y));
                y += Math.round(WRAPPED_LINE_STEP * bodyTextScale);
            }
            y += Math.round((SENTENCE_STEP - WRAPPED_LINE_STEP) * bodyTextScale);
        }
        contentHeight =
                rows.isEmpty()
                        ? 0
                        : rows.get(rows.size() - 1).y
                                + (int) Math.ceil(font.lineHeight * bodyTextScale);
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    /** 避免中文句末标点独占末行；移动前一字时保留其原有样式。 */
    private List<FormattedCharSequence> wrapParagraph(Component paragraph, int width) {
        var lines = new ArrayList<>(font.split(paragraph, width));
        if (lines.size() < 2) return lines;
        int last = lines.size() - 1;
        StringBuilder tail = new StringBuilder();
        lines.get(last)
                .accept(
                        (index, style, codePoint) -> {
                            tail.appendCodePoint(codePoint);
                            return true;
                        });
        if (tail.isEmpty() || !tail.codePoints().allMatch(c -> "。，、；：！？）》】」』".indexOf(c) >= 0))
            return lines;
        List<FormattedCharSequence> previous = new ArrayList<>();
        List<Integer> codePoints = new ArrayList<>();
        lines.get(last - 1)
                .accept(
                        (index, style, codePoint) -> {
                            previous.add(
                                    FormattedCharSequence.forward(
                                            new String(Character.toChars(codePoint)), style));
                            codePoints.add(codePoint);
                            return true;
                        });
        if (previous.size() < 2
                || Character.UnicodeScript.of(codePoints.get(codePoints.size() - 1))
                        != Character.UnicodeScript.HAN) return lines;
        var joined =
                FormattedCharSequence.composite(previous.get(previous.size() - 1), lines.get(last));
        if (font.width(joined) > width) return lines;
        previous.remove(previous.size() - 1);
        lines.set(last - 1, FormattedCharSequence.composite(previous));
        lines.set(last, joined);
        return lines;
    }

    private int viewport() {
        return bodyBottom - bodyTop;
    }

    private float bodyLineHeight() {
        return (font.lineHeight + 2) * bodyTextScale;
    }

    private int edgeFadeHeight() {
        return Math.min(viewport() / 2, (int) Math.ceil(bodyLineHeight() * 1.5F));
    }

    private int rowAlpha(int y) {
        float lineHeight = bodyLineHeight();
        float guard = Math.max(1F, lineHeight * .25F);
        float opacity = 1;
        if (scroll > 0) opacity = Math.min(opacity, (y - bodyTop) / guard);
        if (scroll < maxScroll())
            opacity = Math.min(opacity, (bodyBottom - y - lineHeight) / guard);
        opacity = Mth.clamp(opacity, 0, 1);
        return Math.round(opacity * opacity * (3 - 2 * opacity) * 255);
    }

    public int maxScroll() {
        return Math.max(0, contentHeight - viewport());
    }

    public double scrollPosition() {
        return scroll;
    }

    private int railX() {
        return left + panelWidth - 28;
    }

    private int closeCenterX() {
        return railX() + 2;
    }

    private boolean overClose(double x, double y) {
        return x >= closeCenterX() - 8 && x < closeCenterX() + 9 && y >= top + 24 && y < top + 41;
    }

    private int thumbHeight() {
        return Math.min(
                viewport(), Math.max(26, viewport() * viewport() / Math.max(1, contentHeight)));
    }

    private int thumbY() {
        return bodyTop
                + (int)
                        Math.round(
                                scroll / Math.max(1, maxScroll()) * (viewport() - thumbHeight()));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        CatalogFrame.draw(g, width, height, left, top, panelWidth, panelHeight);
        rect(g, left + 28, top + 26, 42, 42, 0xFF19191D);
        outline(g, left + 28, top + 26, 42, 42, 0xFF896E50);
        outline(g, left + 31, top + 29, 36, 36, 0xFF393242);
        rect(g, left + 34, top + 32, 30, 30, 0xFF211E2B);
        outline(g, left + 26, top + 24, 46, 46, 0xFF493B2C);
        for (int ix = 0; ix < 2; ix++)
            for (int iy = 0; iy < 2; iy++) {
                int x = left + 28 + ix * 37, y = top + 26 + iy * 37;
                rect(g, x, y, 5, 5, 0xFF987549);
                rect(g, x, y, 5, 1, 0xFFC5A16B);
                rect(g, x, y + 1, 1, 3, 0xFFAD8753);
                rect(g, x + 1, y + 4, 4, 1, 0xFF5E442C);
                rect(g, x + 4, y + 1, 1, 3, 0xFF705132);
                rect(g, x + 2, y + 2, 1, 1, 0xFF453427);
            }
        g.pose().pushPose();
        g.pose().translate(left + 33, top + 31, 0);
        g.pose().scale(2, 2, 1);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
        float headingScale = 1.6F;
        float headingGap = 5;
        float headingHeight = font.lineHeight * headingScale;
        float labelHeight = headingHeight - 2;
        float labelScale = labelHeight / font.lineHeight;
        float headingTop = top + 47 - (labelHeight + headingHeight + headingGap) / 2;
        g.pose().pushPose();
        g.pose().translate(left + 85, headingTop, 0);
        g.pose().scale(labelScale, labelScale, 1);
        g.drawString(
                font,
                Component.translatable("gui.the_long_travail.catalog.title")
                        .withStyle(style -> style.withBold(true)),
                0,
                0,
                0xB49A74,
                false);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(left + 85, headingTop + labelHeight + headingGap, 0);
        g.pose().scale(headingScale, headingScale, 1);
        g.drawString(font, title, 0, 0, 0xE8D8AF, false);
        g.pose().popPose();
        int closeX = closeCenterX() - 3;
        boolean closeHover = overClose(mouseX, mouseY);
        if (closeHover) {
            rect(g, closeX - 5, top + 24, 17, 17, 0xFF211B17);
            outline(g, closeX - 5, top + 24, 17, 17, 0xFF55412E);
            rect(g, closeX - 4, top + 25, 15, 1, 0xFF171310);
            rect(g, closeX - 4, top + 39, 15, 1, 0xFF705436);
        }
        for (int i = 0; i < 7; i++) {
            rect(g, closeX + i + 1, top + 30 + i, 1, 1, 0xFF17120F);
            rect(g, closeX + 7 - i, top + 30 + i, 1, 1, 0xFF17120F);
        }
        for (int i = 0; i < 7; i++) {
            int copper =
                    closeHover
                            ? (i < 3 ? 0xFFE2C18A : 0xFFC09960)
                            : (i < 3 ? 0xFFB29365 : 0xFF8E6C43);
            rect(g, closeX + i, top + 29 + i, 1, 1, copper);
            rect(g, closeX + 6 - i, top + 29 + i, 1, 1, copper);
        }
        rect(g, left + 29, top + 78, panelWidth - 58, 1, 0xFF5D4934);
        rect(g, left + panelWidth / 2 - 19, top + 77, 39, 3, 0xFF27211E);
        emblem(g, left + panelWidth / 2, top + 78, 0xFFAD8754);
        session.scissor(g, left + 30, bodyTop, left + panelWidth - 46, bodyBottom);
        int contentOffsetY = Math.max(0, (viewport() - contentHeight) / 2);
        for (Row row : rows) {
            int y = bodyTop + contentOffsetY + row.y - (int) scroll;
            int alpha = rowAlpha(y);
            // Font 会将极小的 alpha 值视为未指定透明度，因此直接跳过这些行的绘制。
            if (alpha >= 4 && y + font.lineHeight * bodyTextScale >= bodyTop && y < bodyBottom) {
                g.pose().pushPose();
                g.pose().translate(left + 34, y, 0);
                g.pose().scale(bodyTextScale, bodyTextScale, 1);
                g.drawString(
                        font,
                        row.text,
                        0,
                        0,
                        (alpha << 24) | (CatalogTextStyle.BODY & 0xFFFFFF),
                        false);
                g.pose().popPose();
            }
        }
        g.disableScissor();
        fadeEdges(
                g,
                left + 30,
                bodyTop,
                left + panelWidth - 46,
                bodyBottom,
                edgeFadeHeight(),
                scroll > 0,
                scroll < maxScroll());
        if (maxScroll() > 0) {
            int x = railX(), y = thumbY(), h = thumbHeight();
            rect(g, x, bodyTop, 5, viewport(), 0xFF151211);
            rect(g, x + 2, bodyTop, 1, viewport(), 0xFF4A3829);
            rect(g, x - 2, bodyTop, 1, viewport(), 0xFF49392A);
            rect(g, x + 6, bodyTop, 1, viewport(), 0xFF100E0D);
            boolean over = mouseX >= x - 3 && mouseX <= x + 8 && mouseY >= y && mouseY <= y + h;
            rect(g, x - 1, y + 2, 7, h - 4, 0xFF55412E);
            rect(g, x, y + 1, 5, h - 2, over || dragging ? 0xFFC6A16D : 0xFF9D7B50);
            rect(g, x + 1, y, 3, 1, 0xFFC5A16B);
            rect(g, x + 1, y + h - 1, 3, 1, 0xFF765638);
            rect(g, x + 1, y + 2, 1, h - 4, 0xFFD2AE76);
            for (int dy : new int[] {-3, 0, 3}) rect(g, x + 1, y + h / 2 + dy, 3, 1, 0xFF513E2C);
            if (dragging) rect(g, x + 3, y + 3, 1, h - 6, 0xFFEDD09B);
            if (scroll > 0) arrow(g, x + 2, bodyTop - 8, true);
            if (scroll < maxScroll()) arrow(g, x + 2, bodyBottom + 8, false);
        }
        int footerY = top + panelHeight - 29;
        rect(g, left + 29, footerY - 7, panelWidth - 58, 1, 0xFF493A2B);
        emblem(g, left + panelWidth / 2, footerY + 4, 0xFF806341);
        g.drawString(
                font,
                Component.translatable("gui.the_long_travail.catalog.read"),
                left + 33,
                footerY,
                0x9E886C,
                false);
        boolean backHover = overBack(mouseX, mouseY);
        if (backHover) outline(g, backLeft(), footerY - 4, backWidth(), 17, 0xFF806341);
        g.drawString(
                font,
                Component.translatable("gui.the_long_travail.catalog.back"),
                backLeft() + 3,
                footerY,
                backHover ? 0xE2C18A : 0xB49A74,
                false);
        String back = escapeHint().getString();
        g.drawString(
                font, back, left + panelWidth - 33 - font.width(back), footerY, 0x9E886C, false);
    }

    private Component escapeHint() {
        return Component.translatable(
                fromParent
                        ? "gui.the_long_travail.catalog.escape_back"
                        : "gui.the_long_travail.catalog.exit");
    }

    private void dismiss() {
        if (fromParent) session.back();
        else session.close();
    }

    private int backRight() {
        return left + panelWidth - 33 - font.width(escapeHint()) - 12;
    }

    private int backWidth() {
        return Math.max(
                62, font.width(Component.translatable("gui.the_long_travail.catalog.back")) + 6);
    }

    private int backLeft() {
        return backRight() - backWidth();
    }

    private boolean overBack(double x, double y) {
        return x >= backLeft()
                && x <= backRight()
                && y >= top + panelHeight - 33
                && y <= top + panelHeight - 17;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        scroll = Mth.clamp(scroll - delta * 28, 0, maxScroll());
        return true;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            dismiss();
            return true;
        }
        if (button != 0) return false;
        if (overClose(x, y)) {
            onClose();
            return true;
        }
        if (overBack(x, y)) {
            session.back();
            return true;
        }
        if (maxScroll() > 0
                && x >= railX() - 4
                && x <= railX() + 9
                && y >= bodyTop
                && y <= bodyBottom) {
            if (y >= thumbY() && y <= thumbY() + thumbHeight()) {
                dragging = true;
                grabOffset = y - thumbY();
            } else
                scroll =
                        Mth.clamp(
                                scroll + (y < thumbY() ? -viewport() : viewport()), 0, maxScroll());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (!dragging) return false;
        scroll =
                Mth.clamp(
                        (y - bodyTop - grabOffset)
                                / Math.max(1, viewport() - thumbHeight())
                                * maxScroll(),
                        0,
                        maxScroll());
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        dragging = false;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            dismiss();
            return true;
        }
        double amount =
                switch (key) {
                    case GLFW.GLFW_KEY_DOWN -> 18;
                    case GLFW.GLFW_KEY_UP -> -18;
                    case GLFW.GLFW_KEY_PAGE_DOWN -> viewport();
                    case GLFW.GLFW_KEY_PAGE_UP -> -viewport();
                    case GLFW.GLFW_KEY_HOME -> -contentHeight;
                    case GLFW.GLFW_KEY_END -> contentHeight;
                    default -> 0;
                };
        if (amount == 0) return false;
        scroll = Mth.clamp(scroll + amount, 0, maxScroll());
        return true;
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

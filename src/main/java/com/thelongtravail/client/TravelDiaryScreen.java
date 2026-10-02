package com.thelongtravail.client;

import com.thelongtravail.TheLongTravail;
import com.mojang.blaze3d.systems.RenderSystem;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.item.LongTravailItem;
import com.thelongtravail.network.TooltipConfigSync;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** 以模态阅读层绘制，原背包或容器仍为当前活动界面。 */
public final class TravelDiaryScreen extends Screen {
    private static final ResourceLocation BACKGROUND = texture("diary_base");
    private static final ResourceLocation BOOKMARK = texture("diary_bookmark");
    private static final int WIDTH = 524, HEIGHT = 360, BOOK_X = 40, BOOK_WIDTH = 480;
    private static final int HEADER_Y = 74, HEADER_RULE_Y = HEADER_Y + 17;
    private static final int RIGHT_X = 302, RIGHT_WIDTH = 179, BODY_TOP = HEADER_RULE_Y + 21, BODY_BOTTOM = 300;
    private static final int LINE_HEIGHT = 15, VISIBLE_LINES = (BODY_BOTTOM - BODY_TOP) / LINE_HEIGHT;
    private static final int INK = 0xFF493626, MUTED_INK = 0xFF80664C;
    private static final int[] COLORS = {0x487846, 0x397F8A, 0xB78732, 0x7A8085, 0xA14A3E, 0x805091};
    private static final int[] TEXT_COLORS = {0x355B32, 0x285C65, 0x805B22, 0x50565C, 0x81352D, 0x654073};
    private static TravailAspect lastSelected = TravailAspect.FLOURISHING;

    private final Supplier<ItemStack> source;
    private final Runnable closeAction;
    private final float[] bookmarkExtensions = new float[6];
    private TravailAspect selected = lastSelected;
    private boolean journeyPage;
    private int scroll;
    private int maxScroll;
    private int bookLeft, bookTop;
    private float bookScale = 1;
    private long lastFrame = System.nanoTime();
    private long contentChanged;
    private ItemStack diary = ItemStack.EMPTY;
    private LayoutKey proseKey, contentKey;
    private List<FormattedCharSequence> cachedProse = List.of();
    private List<Float> cachedOffsets = List.of();
    private List<Float> cachedCenters = List.of();
    private List<PageLine> cachedContent = List.of();
    private LongTravailData.RequirementIdentity cachedRequirements;
    private record LayoutKey(TravailAspect aspect, boolean witness, boolean journey, boolean revealed,
                             boolean initialized, int biomes, long config, long resources,
                             net.minecraft.locale.Language language, int lineHeight) {}
    private LayoutKey layoutKey() {
        return new LayoutKey(selected, completed(), journeyPage,
                com.thelongtravail.network.JourneySync.isRevealed(selected), LongTravailData.isInitialized(diary),
                com.thelongtravail.network.JourneySync.biomeCount(), TooltipConfigSync.revision(),
                DiaryFontEffects.resourceGeneration(), net.minecraft.locale.Language.getInstance(), font.lineHeight);
    }


    public TravelDiaryScreen(Supplier<ItemStack> source, Runnable closeAction) {
        super(Component.translatable("gui.the_long_travail.diary.title"));
        this.source = source;
        this.closeAction = closeAction;
        bookmarkExtensions[selected.ordinal()] = 8;
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(TheLongTravail.MODID, "textures/gui/" + name + ".png");
    }

    private Component text(String key, Object... args) {
        return Component.translatable("gui.the_long_travail.diary." + key, args);
    }

    private boolean completed() { return LongTravailData.hasWitness(diary, selected); }
    private boolean hasJourney() { return !completed(); }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        diary = source.get();
        if (!diary.is(ModRegistry.LONG_TRAVAIL.get())) { onClose(); return; }
        if (!hasJourney()) journeyPage = false;
        updateLayout();
        float localX = (mouseX - bookLeft) / bookScale;
        float localY = (mouseY - bookTop) / bookScale;
        long now = System.nanoTime();
        double elapsed = (now - lastFrame) / 1_000_000_000.0;
        lastFrame = now;

        graphics.fill(0, 0, width, height, 0xB0101010);
        graphics.pose().pushPose();
        graphics.pose().translate(bookLeft, bookTop, 0);
        graphics.pose().scale(bookScale, bookScale, 1);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (TravailAspect aspect : TravailAspect.values()) {
            int index = aspect.ordinal();
            boolean hovered = overBookmark(localX, localY, index);
            float target = aspect == selected ? (hovered ? 10 : 8) : hovered ? 4 : 0;
            bookmarkExtensions[index] = BookmarkAnimation.approach(bookmarkExtensions[index], target, elapsed);
            float x = 14 - bookmarkExtensions[index];
            int y = bookmarkY(index);
            int color = COLORS[index];
            graphics.setColor(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, 1);
            // 仅采样布纹区域，保留生成素材的透明边距。
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.blit(BOOKMARK, 0, 0, 88, 27, 150, 140, 1870, 430, 2172, 724);
            graphics.pose().popPose();
        }
        graphics.setColor(1, 1, 1, 1);
        graphics.blit(BACKGROUND, BOOK_X, 0, BOOK_WIDTH, HEIGHT, 0, 0, 1448, 1086, 1448, 1086);
        RenderSystem.disableBlend();
        renderIdentity(graphics);
        renderTabs(graphics, localX, localY);
        renderContent(graphics);
        renderClose(graphics, localX, localY);
        graphics.pose().popPose();

        for (TravailAspect aspect : TravailAspect.values()) {
            if (overBookmark(localX, localY, aspect.ordinal())) {
                renderBookmarkHud(graphics, Component.translatable("gui.the_long_travail.diary.bookmark_hint",
                        Component.translatable("aspect.the_long_travail." + aspect.id()),
                        Component.translatable("state.the_long_travail." +
                                (LongTravailData.hasWitness(diary, aspect) ? "witness" : "malice"))), mouseX, mouseY);
                break;
            }
        }
    }

    private void renderBookmarkHud(GuiGraphics graphics, Component label, int mouseX, int mouseY) {
        float scale = 0.8F;
        int panelWidth = font.width(label) + 10, panelHeight = font.lineHeight + 8;
        int renderedWidth = (int) Math.ceil(panelWidth * scale);
        int renderedHeight = (int) Math.ceil(panelHeight * scale);
        int x = mouseX + 9, y = mouseY - renderedHeight - 3;
        if (x + renderedWidth > width - 3) x = mouseX - renderedWidth - 9;
        x = Math.max(3, Math.min(x, width - renderedWidth - 3));
        y = Math.max(3, Math.min(y, height - renderedHeight - 3));

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 400);
        graphics.pose().scale(scale, scale, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.fillGradient(1, 1, panelWidth - 1, panelHeight - 1, 0xA05A6A85, 0xA0253049);
        graphics.fill(2, 0, panelWidth - 2, 1, 0x9AD8ECFF);
        graphics.fill(0, 2, 1, panelHeight - 2, 0x85BDD9EE);
        graphics.fill(panelWidth - 1, 2, panelWidth, panelHeight - 2, 0x707E9BB8);
        graphics.fill(2, panelHeight - 1, panelWidth - 2, panelHeight, 0x707E9BB8);
        graphics.fill(1, 1, panelWidth - 1, 3, 0x20FFFFFF);
        for (int row = 0; row < 5; row++) {
            int left = 3 + row;
            graphics.fill(left, 3 + row, left + 2, 4 + row, 0x18D8F2FF);
        }
        graphics.drawString(font, label, 5, 4, 0xFFF0F6FF, false);
        graphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    private void updateLayout() {
        bookScale = 0.8F * Math.max(0.1F, Math.min(1.25F, Math.min((width - 12F) / WIDTH, (height - 12F) / HEIGHT)));
        bookLeft = (width - Math.round(WIDTH * bookScale)) / 2;
        bookTop = (height - Math.round(HEIGHT * bookScale)) / 2;
    }

    private static int bookmarkY(int index) { return 61 + index * 39; }

    private boolean overBookmark(double x, double y, int index) {
        // 固定命中区域同时覆盖收起和展开位置，避免悬停状态反复切换。
        return inside(x, y, 4, bookmarkY(index), 57, bookmarkY(index) + 28);
    }

    private void renderIdentity(GuiGraphics graphics) {
        float titleScale = 2.6F;
        // 默认文字可见高度约八像素，第九像素用于行距。
        float inkHeight = font.lineHeight - 1F;
        float titleInkCenter = inkHeight / 2F;
        float titleY = (40 + HEADER_RULE_Y) / 2F - titleInkCenter * titleScale + 3F;
        drawScaledCentered(graphics, text("title"), 169, titleY, titleScale, INK);
        graphics.fill(87, HEADER_RULE_Y, 251, HEADER_RULE_Y + 1, 0x55755A38);
        Component aspectName = text("bookmark_hint",
                Component.translatable("aspect.the_long_travail." + selected.id()),
                Component.translatable("state.the_long_travail." + (completed() ? "witness" : "malice")));
        float aspectScale = Math.min(1.8F, 164F / Math.max(1, font.width(aspectName)));
        float proseScale = 1.2F;
        // 左页散文缓存仅受页面和字体变化影响，不随攻击冷却 NBT 失效。
        LayoutKey currentProseKey = new LayoutKey(selected, completed(), false, false, false, 0, 0,
                DiaryFontEffects.resourceGeneration(), net.minecraft.locale.Language.getInstance(), font.lineHeight);
        if (!currentProseKey.equals(proseKey)) {
            List<FormattedCharSequence> proseLines = new ArrayList<>();
            List<Float> lineOffsets = new ArrayList<>();
            float nextLineY = 0;
            for (String line : DiaryPageProse.text(selected, completed()).split("\n")) {
                if (line.isBlank()) {
                    nextLineY += 10; // 诗节间距，额外叠加在普通行距之上。
                    continue;
                }
                for (var wrapped : font.split(Component.literal(line), (int) (164 / proseScale))) {
                    proseLines.add(wrapped);
                    lineOffsets.add(nextLineY);
                    nextLineY += 15;
                }
            }
            List<Float> centers = new ArrayList<>();
            for (var line : proseLines) {
                StringBuilder text = new StringBuilder();
                line.accept((index, style, codePoint) -> { text.appendCodePoint(codePoint); return true; });
                float offset = ProseAlignment.offset(text.toString(), value -> font.width(value));
                float spare = Math.max(0, (164F / proseScale - font.width(line)) / 2F);
                centers.add(-font.width(line) / 2F + Math.max(-spare, Math.min(spare, offset)));
            }
            cachedProse = List.copyOf(proseLines);
            cachedOffsets = List.copyOf(lineOffsets);
            cachedCenters = List.copyOf(centers);
            proseKey = currentProseKey;
        }
        List<FormattedCharSequence> proseLines = cachedProse;
        List<Float> lineOffsets = cachedOffsets;
        // 副标题跨页固定，仅将其下方正文居中。
        float headingY = HEADER_RULE_Y + 12;
        float areaTop = headingY + inkHeight * aspectScale;
        float areaBottom = 310;
        float lastOffset = lineOffsets.get(lineOffsets.size() - 1);
        float spacingFactor = Math.min(1F, (areaBottom - areaTop
                - inkHeight * proseScale) / Math.max(1F, lastOffset));
        float proseHeight = lastOffset * spacingFactor + inkHeight * proseScale;
        drawScaledCentered(graphics, aspectName, 169, headingY, aspectScale, 0xFF000000 | TEXT_COLORS[selected.ordinal()]);
        // 略微上移视觉中心，为纸张底部纹理留出空间。
        float proseY = (areaTop + areaBottom - proseHeight) / 2F - 3F;
        graphics.pose().pushPose();
        graphics.pose().translate(169, proseY, 0);
        graphics.pose().scale(proseScale, proseScale, 1);
        for (int i = 0; i < proseLines.size(); i++) {
            var line = proseLines.get(i);
            graphics.pose().pushPose();
            graphics.pose().translate(cachedCenters.get(i),
                    lineOffsets.get(i) * spacingFactor / proseScale, 0);
            graphics.drawString(font, line, 0, 0, INK, false);
            graphics.pose().popPose();
        }
        graphics.pose().popPose();
    }

    private void renderTabs(GuiGraphics graphics, float x, float y) {
        tab(graphics, text("effects"), 305, HEADER_Y, 73, !journeyPage, inside(x, y, 302, 68, 381, 94));
        if (hasJourney()) {
            Component title = text(com.thelongtravail.network.JourneySync.isRevealed(selected) ? "journey" : "journey_locked");
            tab(graphics, title, 391, HEADER_Y, 93, journeyPage, inside(x, y, 387, 68, 490, 94));
        }
    }

    private void tab(GuiGraphics graphics, Component title, int x, int y, int width, boolean active, boolean hovered) {
        if (hovered) graphics.fill(x - 2, y - 4, x + width + 2, y + 16, 0x14755A38);
        drawCentered(graphics, title, x + width / 2, y, active ? INK : MUTED_INK);
        if (active) graphics.fill(x, y + 17, x + width, y + 18, 0xFF96764E);
    }

    private List<PageLine> content() {
        LayoutKey key = layoutKey();
        LongTravailData.RequirementIdentity requirements = LongTravailData.queryIdentity(diary);
        if (!key.equals(contentKey) || !requirements.equals(cachedRequirements)) {
            cachedContent = List.copyOf(buildContent());
            cachedRequirements = requirements;
            contentKey = key;
        }
        return cachedContent;
    }

    private List<PageLine> buildContent() {
        List<PageLine> lines = new ArrayList<>();
        if (!journeyPage) {
            if (selected == TravailAspect.FLOURISHING && completed()) {
                append(lines, LongTravailItem.flourishingProgress(), INK);
                lines.add(new PageLine(FormattedCharSequence.EMPTY, INK, LINE_HEIGHT / 2));
            }
            for (FormattedCharSequence line : font.split(LongTravailItem.aspectEffect(selected, completed()), RIGHT_WIDTH))
                lines.add(new PageLine(line, INK, font.width(line) == 0 ? LINE_HEIGHT / 2 : LINE_HEIGHT));
        } else if (!com.thelongtravail.network.JourneySync.isRevealed(selected)) {
            append(lines, Component.translatable("tooltip.the_long_travail.revelation.hidden"), MUTED_INK);
        } else if (!LongTravailData.isInitialized(diary)) {
            append(lines, Component.translatable("tooltip.the_long_travail.revelation.not_started"), MUTED_INK);
        } else {
            appendGroup(lines, false, "biomes");
            lines.add(new PageLine(FormattedCharSequence.EMPTY, INK));
            appendGroup(lines, true, "structures");
        }
        return lines;
    }

    private void appendGroup(List<PageLine> lines, boolean structures, String heading) {
        var entries = LongTravailData.requirementsForDisplay(diary, selected, structures);
        long done = entries.stream().filter(LongTravailData.RequirementStatus::completed).count();
        float headingScale = 1.15F;
        for (var line : font.split(text(heading, done, entries.size()), (int) (RIGHT_WIDTH / headingScale)))
            lines.add(new PageLine(line, INK, 18, headingScale));
        lines.add(new PageLine(FormattedCharSequence.EMPTY, INK, 4));
        if (entries.isEmpty()) append(lines, Component.translatable("tooltip.the_long_travail.revelation.none"), MUTED_INK);
        for (var entry : entries) {
            String name = TooltipConfigSync.requirementDisplayName(selected, structures, entry.id());
            append(lines, Component.literal((entry.completed() ? "✓ " : "○ ") + name),
                    entry.completed() ? 0xFF677653 : INK);
        }
    }

    private void append(List<PageLine> lines, Component component, int color) {
        for (FormattedCharSequence line : font.split(component, RIGHT_WIDTH))
            lines.add(new PageLine(line, color));
    }

    private void renderContent(GuiGraphics graphics) {
        List<PageLine> lines = content();
        int viewportHeight = BODY_BOTTOM - BODY_TOP;
        int contentHeight = lines.stream().mapToInt(PageLine::height).sum();
        maxScroll = lines.size();
        int lastPageHeight = 0;
        while (maxScroll > 0 && lastPageHeight + lines.get(maxScroll - 1).height() <= viewportHeight) {
            lastPageHeight += lines.get(--maxScroll).height();
        }
        scroll = Math.min(scroll, maxScroll);
        float fade = Math.min(1, 0.3F + (System.nanoTime() - contentChanged) / 160_000_000F);
        int alpha = Math.max(4, (int) (255 * fade)) << 24;
        graphics.enableScissor((int) (bookLeft + RIGHT_X * bookScale), (int) (bookTop + (BODY_TOP - 2) * bookScale),
                (int) Math.ceil(bookLeft + (RIGHT_X + RIGHT_WIDTH + 3) * bookScale),
                (int) Math.ceil(bookTop + BODY_BOTTOM * bookScale));
        int y = BODY_TOP;
        for (int index = scroll; index < lines.size() && y < BODY_BOTTOM; index++) {
            PageLine line = lines.get(index);
            graphics.pose().pushPose();
            graphics.pose().translate(RIGHT_X, y, 0);
            graphics.pose().scale(line.scale(), line.scale(), 1);
            graphics.drawString(font, line.text(), 0, 0, alpha | (line.color() & 0xFFFFFF), false);
            graphics.pose().popPose();
            y += line.height();
        }
        graphics.disableScissor();
        if (maxScroll > 0) {
            int trackX = 493, trackHeight = BODY_BOTTOM - BODY_TOP;
            graphics.fill(trackX, BODY_TOP, trackX + 2, BODY_BOTTOM, 0x30755A38);
            int thumbHeight = Math.max(12, trackHeight * viewportHeight / contentHeight);
            int offset = lines.subList(0, scroll).stream().mapToInt(PageLine::height).sum();
            int maxOffset = lines.subList(0, maxScroll).stream().mapToInt(PageLine::height).sum();
            int thumbY = BODY_TOP + (trackHeight - thumbHeight) * offset / maxOffset;
            graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, 0xB0755A38);
            Component position = Component.translatable("gui.the_long_travail.diary.scroll", scroll + 1, maxScroll + 1);
            graphics.drawString(font, position, RIGHT_X, 311, MUTED_INK, false);
        }
    }

    private void renderClose(GuiGraphics graphics, float x, float y) {
        boolean hover = inside(x, y, 469, 43, 496, 67);
        if (hover) graphics.fill(470, 44, 495, 66, 0x28755A38);
        // 用几何图形绘制叉号，避免依赖字体是否包含该字形。
        int color = hover ? 0xFF713D2A : MUTED_INK;
        for (int i = 0; i < 9; i++) {
            graphics.fill(477 + i, 50 + i, 479 + i, 52 + i, color);
            graphics.fill(485 - i, 50 + i, 487 - i, 52 + i, color);
        }
    }

    private void drawCentered(GuiGraphics graphics, Component value, int centerX, int y, int color) {
        graphics.drawString(font, value, centerX - font.width(value) / 2, y, color, false);
    }

    private void drawScaledCentered(GuiGraphics graphics, Component value, int centerX, float y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, value, -font.width(value) / 2, 0, color, false);
        graphics.pose().popPose();
    }

    private void changeContent() { scroll = 0; contentChanged = System.nanoTime(); }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { onClose(); return true; }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        double x = (mouseX - bookLeft) / bookScale, y = (mouseY - bookTop) / bookScale;
        if (inside(x, y, 469, 43, 496, 67)) { onClose(); return true; }
        for (TravailAspect aspect : TravailAspect.values()) {
            if (overBookmark(x, y, aspect.ordinal())) {
                if (selected != aspect) {
                    DiaryActionSounds.switchPage(true);
                    selected = aspect;
                    lastSelected = aspect;
                    journeyPage = false;
                    changeContent();
                }
                return true;
            }
        }
        if (inside(x, y, 302, 68, 381, 94) && journeyPage) {
            DiaryActionSounds.switchPage(false);
            journeyPage = false;
            changeContent();
        } else if (inside(x, y, 387, 68, 490, 94) && hasJourney() && !journeyPage) {
            DiaryActionSounds.switchPage(false);
            journeyPage = true;
            changeContent();
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        double x = (mouseX - bookLeft) / bookScale, y = (mouseY - bookTop) / bookScale;
        if (delta != 0 && inside(x, y, RIGHT_X - 4, BODY_TOP - 5, 499, 326))
            scroll = Math.max(0, Math.min(maxScroll, scroll + (delta < 0 ? 2 : -2)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE) onClose();
        else if (keyCode == GLFW.GLFW_KEY_DOWN) scroll = Math.min(maxScroll, scroll + 1);
        else if (keyCode == GLFW.GLFW_KEY_UP) scroll = Math.max(0, scroll - 1);
        else if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) scroll = Math.min(maxScroll, scroll + VISIBLE_LINES);
        else if (keyCode == GLFW.GLFW_KEY_PAGE_UP) scroll = Math.max(0, scroll - VISIBLE_LINES);
        return true;
    }

    @Override
    public void onClose() { closeAction.run(); }

    @Override
    public boolean isPauseScreen() { return false; }

    private static boolean inside(double x, double y, int left, int top, int right, int bottom) {
        return x >= left && x < right && y >= top && y < bottom;
    }

    private record PageLine(FormattedCharSequence text, int color, int height, float scale) {
        private PageLine(FormattedCharSequence text, int color, int height) { this(text, color, height, 1F); }
        private PageLine(FormattedCharSequence text, int color) { this(text, color, LINE_HEIGHT); }
    }
}


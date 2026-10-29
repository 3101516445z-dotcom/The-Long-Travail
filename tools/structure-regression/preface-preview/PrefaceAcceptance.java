package travail.prefacepreview;

import com.thelongtravail.client.TravelDiaryScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

/** Temporary language override for layout tests; no production copy is modified. */
final class PrefaceAcceptance {
    private static Language original;

    static int layout(String name) throws Exception {
        var field = TravelDiaryScreen.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(null);
    }

    static void installLongCopy() {
        original = Language.getInstance();
        Language.inject(
                new Language() {
                    public String getOrDefault(String key, String fallback) {
                        if (key.equals("gui.the_long_travail.diary.preface.left"))
                            return "滚动验证：这是一段仅用于检查布局的临时文字。\n".repeat(36);
                        if (key.equals("gui.the_long_travail.diary.preface.right"))
                            return "Independent right-page scrolling and boundary verification.\n"
                                    .repeat(44);
                        return original.getOrDefault(key, fallback);
                    }

                    public boolean has(String key) {
                        return original.has(key);
                    }

                    public boolean isDefaultRightToLeft() {
                        return original.isDefaultRightToLeft();
                    }

                    public FormattedCharSequence getVisualOrder(FormattedText text) {
                        return original.getVisualOrder(text);
                    }
                });
        clearTextLayoutCache();
        String injected =
                net.minecraft.network.chat.Component.translatable(
                                "gui.the_long_travail.diary.preface.left")
                        .getString();
        if (!injected.startsWith("滚动验证"))
            throw new AssertionError("Temporary language override did not take effect");
    }

    // Runtime language injection bypasses resource reload. Modern UI caches layouts
    // by component, so invalidate that test-only stale cache after switching text.
    private static void clearTextLayoutCache() {
        try {
            Class<?> engine = Class.forName("icyllis.modernui.mc.text.TextLayoutEngine");
            engine.getMethod("clear").invoke(engine.getMethod("getInstance").invoke(null));
        } catch (ClassNotFoundException absent) {
            // Vanilla does not have this additional layout cache.
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(
                    "Cannot invalidate test language layout cache", failure);
        }
    }

    static void restore() {
        if (original != null) {
            Language.inject(original);
            original = null;
            clearTextLayoutCache();
        }
    }

    static int[] values(Object page, String name) throws Exception {
        var field = page.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (int[]) field.get(page);
    }

    static void wheel(PrefacePreview probe, int x, int y, double delta) throws Exception {
        float scale = (float) probe.field("bookScale");
        probe.page.mouseScrolled(
                (int) probe.field("bookLeft") + x * scale,
                (int) probe.field("bookTop") + y * scale,
                delta);
    }

    static void verifyScroll(PrefacePreview probe) throws Exception {
        Object page = probe.field("prefacePage");
        int[] scroll = values(page, "scroll"), max = values(page, "maximum");
        probe.check(
                max[0] > 12 && max[1] > 12,
                "Long copy must overflow both pages: " + java.util.Arrays.toString(max));
        wheel(probe, 100, 150, -1);
        probe.check(scroll[0] == 2 && scroll[1] == 0, "Left independent wheel");
        wheel(probe, 320, 150, -1);
        probe.check(scroll[0] == 2 && scroll[1] == 2, "Right independent wheel");
        probe.page.keyPressed(267, 0, 0);
        probe.check(scroll[1] == 14 && scroll[0] == 2, "PageDown targets active right page");
        probe.page.keyPressed(266, 0, 0);
        probe.check(scroll[1] == 2, "PageUp");
        probe.page.keyPressed(264, 0, 0);
        probe.page.keyPressed(265, 0, 0);
        probe.check(scroll[1] == 2, "Arrow down/up");
        for (int i = 0; i < 100; i++) probe.page.keyPressed(267, 0, 0);
        probe.check(scroll[1] == max[1], "Right lower bound");
        for (int i = 0; i < 100; i++) probe.page.keyPressed(266, 0, 0);
        probe.check(scroll[1] == 0, "Right upper bound");
        for (int i = 0; i < 100; i++) wheel(probe, 100, 150, -1);
        probe.check(scroll[0] == max[0] && scroll[1] == 0, "Left lower bound");
        for (int i = 0; i < 100; i++) wheel(probe, 100, 150, 1);
        wheel(probe, 280, 150, -1);
        probe.check(scroll[0] == 0 && scroll[1] == 0, "Upper bound and gutter ignored");
        wheel(probe, 100, 150, -1);
        wheel(probe, 320, 150, -1);
    }

    static void scale(int value) {
        var mc = Minecraft.getInstance();
        mc.options.guiScale().set(value);
        mc.resizeDisplay();
    }
}

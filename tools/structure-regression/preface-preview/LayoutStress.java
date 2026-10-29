package travail.prefacepreview;

import net.minecraft.client.Minecraft;

import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;

/** Screen-coordinate input probes against rendered, animated bookmark geometry. */
final class LayoutStress {
    private final int[][] sizes = {
        {640, 480}, {800, 600}, {900, 640}, {1280, 720}, {1920, 1080}, {1500, 700}
    };
    private final int[] scales = {1, 2, 3, 0};
    private int scenario, frame, checks, clicks;
    private final StringBuilder report = new StringBuilder();

    void begin() {
        configure();
    }

    private void configure() {
        var mc = Minecraft.getInstance();
        int[] size = sizes[scenario / 4];
        GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), size[0], size[1]);
        frame = 0;
    }

    private void expect(PrefacePreview p, boolean value, String message) {
        checks++;
        p.check(value, "layout scenario=" + scenario + " frame=" + frame + " " + message);
    }

    private int selected(PrefacePreview p) throws Exception {
        return (boolean) p.field("preface") ? 0 : ((Enum<?>) p.field("selected")).ordinal() + 1;
    }

    private void press(PrefacePreview p, double x, double y) throws Exception {
        // Quantize to real framebuffer pixel centres before dispatching GUI coordinates.
        double gui = Minecraft.getInstance().getWindow().getGuiScale();
        double s = (float) p.field("bookScale");
        double sx = (int) p.field("bookLeft") + x * s;
        double sy = (int) p.field("bookTop") + y * s;
        p.page.mouseClicked(
                (Math.floor(sx * gui) + 0.5) / gui, (Math.floor(sy * gui) + 0.5) / gui, 0);
        clicks++;
    }

    private void hover(PrefacePreview p, double x, double y) throws Exception {
        var w = Minecraft.getInstance().getWindow();
        double s = (float) p.field("bookScale");
        GLFW.glfwSetCursorPos(
                w.getWindow(),
                ((int) p.field("bookLeft") + x * s) * w.getScreenWidth() / w.getGuiScaledWidth(),
                ((int) p.field("bookTop") + y * s) * w.getScreenHeight() / w.getGuiScaledHeight());
    }

    boolean step(PrefacePreview p) throws Exception {
        ++frame;
        // Native resize callbacks may override an immediately requested GUI scale.
        if (frame == 10) PrefaceAcceptance.scale(scales[scenario % 4]);
        if (frame <= 20) return false;
        var mc = Minecraft.getInstance();
        float s = (float) p.field("bookScale");
        if (frame == 21)
            expect(
                    p,
                    mc.getWindow().getGuiScale()
                            == mc.getWindow().calculateScale(scales[scenario % 4], false),
                    "actual GUI scale differs from runtime-supported scale");
        if (frame == 21 && scenario == 18)
            expect(p, mc.getWindow().getGuiScale() == 3, "requested GUI 3 did not take effect");
        float[] extensions = (float[]) p.field("bookmarkExtensions");
        // Texture insertion edge comes from the original 1448 px-wide background.
        double right = 40 + 69 * 480.0 / 1448;
        int index = ((frame - 21) / 10) % 7;
        double y = 59 + index * 34;
        double left = 28 - extensions[index];
        double inset = Math.max(1.5, 1.5 / (s * mc.getWindow().getGuiScale()));
        expect(
                p,
                (int) p.field("bookTop") >= 0 && (int) p.field("bookLeft") >= 0,
                "book origin outside viewport");
        expect(
                p,
                (int) p.field("bookTop") + 360 * s <= p.page.height + 1,
                "book bottom outside viewport");
        for (float extension : extensions)
            expect(p, extension >= 0 && extension <= 10.01, "animation overshoot");

        // Interior near each painted edge, staying clear of the decorative V notch.
        press(p, left + inset, y + 3);
        expect(p, selected(p) == index, "painted upper-left edge missed");
        press(p, right - inset, y + 24);
        expect(p, selected(p) == index, "painted lower-right edge missed");
        for (double[] outside :
                new double[][] {
                    {left - inset, y + 12},
                    {right + inset, y + 12},
                    {right - 3, y - 2},
                    {right - 3, y + 30}
                }) {
            int sentinel = (index + 1) % 7;
            press(p, right - 5, 59 + sentinel * 34 + 12);
            press(p, outside[0], outside[1]);
            expect(p, selected(p) == sentinel, "outside click changed selection");
        }
        press(p, right - 5, y + 12);
        // Hover each ribbon for ten ticks; next frame probes its animated extension.
        hover(p, right - 5, y + 12);
        if (frame < 90) return false;
        p.shot("stress-" + scenario);
        for (int cycle = 0; cycle < 10; cycle++) {
            for (int i = 0; i < 7; i++) {
                press(p, right - 5, 59 + i * 34 + 12);
                expect(p, selected(p) == i, "rapid switch selected wrong bookmark");
                if (i > 0) {
                    press(p, 430, 80);
                    expect(p, (boolean) p.field("journeyPage"), "journey tab missed");
                    press(p, 340, 80);
                    expect(p, !(boolean) p.field("journeyPage"), "effect tab missed");
                }
            }
        }
        p.bookmark(0);
        PrefaceAcceptance.verifyScroll(p);
        press(p, 482, 55);
        expect(p, mc.screen == null, "close icon missed");
        p.open();
        expect(p, selected(p) == 0, "resize/reopen lost preface");
        // Click before the first render after reopening: coordinates must already be valid.
        double expectedScale =
                0.8
                        * Math.max(
                                0.1,
                                Math.min(
                                        1.25,
                                        Math.min(
                                                (p.page.width - 12.0) / 524,
                                                (p.page.height - 12.0) / 360)));
        int expectedLeft = (p.page.width - (int) Math.round(524 * expectedScale)) / 2;
        int expectedTop = (p.page.height - (int) Math.round(360 * expectedScale)) / 2;
        p.page.mouseClicked(
                expectedLeft + 55 * expectedScale, expectedTop + 105 * expectedScale, 0);
        clicks++;
        expect(p, selected(p) == 1, "first input after reopening uses stale layout");
        p.page.mouseClicked(0, 0, 1);
        expect(p, mc.screen == null, "right-click close missed");
        p.open();
        String line =
                String.format(
                        "size=%dx%d requestedGui=%d actualGui=%.1f bookScale=%.4f checks=%d"
                                + " clicks=%d%n",
                        mc.getWindow().getScreenWidth(),
                        mc.getWindow().getScreenHeight(),
                        scales[scenario % 4],
                        mc.getWindow().getGuiScale(),
                        s,
                        checks,
                        clicks);
        report.append(line);
        System.out.print("LAYOUT_STRESS " + line);
        if (++scenario < sizes.length * scales.length) {
            configure();
            return false;
        }
        Files.writeString(
                mc.gameDirectory.toPath().resolve("layout-stress.txt"),
                "PASS scenarios="
                        + scenario
                        + " checks="
                        + checks
                        + " clicks="
                        + clicks
                        + "\n"
                        + report);
        return true;
    }
}

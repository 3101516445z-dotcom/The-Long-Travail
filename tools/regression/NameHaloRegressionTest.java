import com.thelongtravail.client.NameHaloKernel;
import com.thelongtravail.client.DiaryText;
import com.thelongtravail.config.TravailClientConfig.HaloQuality;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import java.util.concurrent.atomic.AtomicInteger;

public final class NameHaloRegressionTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void run() {
        var pose = new Matrix4f().translate(19, -3, 7).rotateZ(0.37F).scale(1.5F, 0.7F, 2);
        var original = new Matrix4f(pose);
        var scratch = new Matrix4f();
        int cases = 0;
        for (var quality : HaloQuality.values()) {
            var kernel = NameHaloKernel.of(quality);
            for (int a = 0; a < 256; a++) {
                int active = 0;
                for (int ring = 0; ring < 2; ring++) {
                    float base = ring == 0 ? 0.075F : 0.035F;
                    float opacity = quality.samplesPerRing == 8 ? base : quality.samplesPerRing == 0 ? 0
                            : 1F - (float) Math.pow(1F - base, 8F / quality.samplesPerRing);
                    int expectedAlpha = Math.round((a < 4 ? 255 : a) * opacity);
                    check(kernel.alpha(a << 24, ring) == expectedAlpha, "alpha normalization and rounding");
                    if (expectedAlpha >= 4) active += quality.samplesPerRing;
                    if (quality.samplesPerRing == 0) continue;
                    int stride = 8 / quality.samplesPerRing;
                    for (int sample = 0; sample < quality.samplesPerRing; sample++) {
                        int index = (sample * stride + (ring == 0 ? 0 : stride / 2)) % 8;
                        double angle = (index + ring * 0.5) * Math.PI / 4;
                        float radius = ring == 0 ? 0.55F : 1.1F;
                        var expected = new Matrix4f(pose).translate(radius * (float) Math.cos(angle), radius * (float) Math.sin(angle), 0);
                        scratch.set(pose).translate(kernel.x(ring, sample), kernel.y(ring, sample), 0);
                        check(expected.equals(scratch), "transformed vertices preserve old sample order and offsets");
                    }
                }
                AtomicInteger visits = new AtomicInteger();
                var style = Style.EMPTY.withBold(true).withItalic(true).withInsertion("the_long_travail:test");
                FormattedCharSequence source = sink -> { visits.incrementAndGet(); return sink.accept(3, style, 0x7A77) && sink.accept(8, style, 0x1F30D); };
                var text = DiaryText.forDraw(source, active > 0);
                check(visits.get() == (active > 0 ? 1 : 0), "single-pass text remains lazy");
                for (int pass = 0; pass <= active; pass++) text.accept((index, clean, cp) -> {
                    check(clean.equals(style.withInsertion(null)), "presentation styles preserved");
                    check(index == 3 ? cp == 0x7A77 : index == 8 && cp == 0x1F30D, "indices and codepoints preserved");
                    return true;
                });
                check(visits.get() == 1, "all halo passes share one source traversal");
                cases++;
            }
        }
        check(pose.equals(original), "caller matrix not modified");
        System.out.println("NAME_HALO_PASS: " + cases + " quality/alpha combinations; old offsets, matrix isolation, styles and one source traversal");
    }
}

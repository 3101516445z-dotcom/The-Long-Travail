import com.thelongtravail.client.DiaryText;
import com.thelongtravail.client.DiaryFontEffects;
import com.thelongtravail.data.PeriodicSchedule;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public final class PerformanceRegressionTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        NameHaloRegressionTest.run();
        int peak = 0;
        int[] loads = new int[100];
        Random random = new Random(42);
        for (int player = 0; player < 100; player++) {
            int hash = random.nextInt(), hits = 0, last = -1;
            for (int tick = 0; tick < 400; tick++) if (PeriodicSchedule.due(tick, hash, PeriodicSchedule.FLUID, 100)) {
                hits++;
                if (last >= 0) check(tick - last == 100, "unchanged interval");
                last = tick;
                if (tick < 100) loads[tick]++;
            }
            check(hits == 4, "unchanged work per complete period");
        }
        for (int load : loads) peak = Math.max(peak, load);
        check(peak < 15, "fixed simulation must spread 100 players across the interval");
        for (int hash : new int[]{0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            for (long time : new long[]{0, -1, Long.MIN_VALUE, Long.MAX_VALUE}) {
                check(PeriodicSchedule.due(time, hash, PeriodicSchedule.FAR_REACH, 1), "every-tick config preserved");
                boolean expected = PeriodicSchedule.due(Math.floorMod(time, 100), hash, PeriodicSchedule.FLUID, 100);
                check(expected == PeriodicSchedule.due(time, hash, PeriodicSchedule.FLUID, 100), "clock overflow safe");
            }
        }
        boolean different = false;
        for (int tick = 0; tick < 100; tick++)
            different |= PeriodicSchedule.due(tick, 123, PeriodicSchedule.FLUID, 100)
                    != PeriodicSchedule.due(tick, 123, PeriodicSchedule.FAR_REACH, 100);
        check(different, "tasks use different stable phases");

        AtomicInteger visits = new AtomicInteger();
        Style first = Style.EMPTY.withColor(0xC5AA77).withBold(true).withInsertion("the_long_travail:name");
        Style second = first.withItalic(true).withFont(ResourceLocation.fromNamespaceAndPath("minecraft", "uniform"));
        FormattedCharSequence source = sink -> {
            visits.incrementAndGet();
            return sink.accept(7, first, 0x82E6) && sink.accept(9, first, 0x1F30D) && sink.accept(12, second, 0x65C5);
        };
        var prepared = DiaryText.prepare(source);
        record Glyph(int index, Style style, int codePoint) {}
        var expected = List.of(new Glyph(7, first.withInsertion(null), 0x82E6),
                new Glyph(9, first.withInsertion(null), 0x1F30D), new Glyph(12, second.withInsertion(null), 0x65C5));
        for (int pass = 0; pass < 17; pass++) {
            check(DiaryFontEffects.material(prepared) == -1, "prepared pass never re-enters material rendering");
            var actual = new ArrayList<Glyph>();
            prepared.accept((index, style, cp) -> { actual.add(new Glyph(index, style, cp)); return true; });
            check(actual.equals(expected), "all passes retain indices, codepoints, order and presentation styles");
        }
        check(visits.get() == 1, "17 passes traverse original text once");
        AtomicInteger emitted = new AtomicInteger();
        check(!prepared.accept((index, style, cp) -> emitted.incrementAndGet() < 2) && emitted.get() == 2,
                "sink early termination preserved");
        check(first.getInsertion().equals("the_long_travail:name"), "original styles remain untouched");
        var lazy = DiaryText.lazyUnmarked(source);
        check(DiaryFontEffects.material(lazy) == -1 && visits.get() == 1, "lazy prose probe does not traverse source");
        lazy.accept((index, style, cp) -> { check(style.getInsertion() == null, "lazy marker removed"); return true; });
        check(visits.get() == 2, "single-pass prose remains lazy");
        class Cache extends com.thelongtravail.client.DrawScopedCache<Object, Object> {
            int created;
            protected Object create(Object key) { created++; return new Object(); }
        }
        var cache = new Cache();
        Object firstType = new String("atlas"), secondType = new String("atlas");
        Object firstValue = cache.value(firstType), secondValue = cache.value(secondType);
        for (int i = 0; i < 17; i++) {
            check(cache.value(firstType) == firstValue && cache.value(secondType) == secondValue,
                    "mixed atlases retain stable identity mappings");
        }
        check(firstValue != secondValue && cache.created == 2, "equal but distinct atlas keys remain distinct");
        check(new Cache().value(firstType) != firstValue, "different draws never share captured material state");
        System.out.println("PASS: unchanged periodic frequency; 100-player simulation peak " + peak
                + " vs 100 before staggering (work counts, not timing); 17 name passes traverse source once; styles/order/early-stop preserved");
    }
}

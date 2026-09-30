import com.thelongtravail.client.VisualDeprivationTimeline;

public final class VisualDeprivationTimelineTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        var timeline = new VisualDeprivationTimeline();
        Object first = new Object(), replacement = new Object();
        timeline.accept(first, 10, 100, 100);
        timeline.tick(first);
        check(timeline.remaining() == 99, "packet before first client tick survives owner binding");
        timeline.accept(first, 30, 100, 100);
        check(timeline.startTick() == 10, "refresh preserves fade-in origin");
        timeline.accept(replacement, 2, 70, 100);
        timeline.tick(replacement);
        check(timeline.remaining() == 69 && timeline.startTick() == 2, "replacement player's packet survives tick");
        timeline.tick(first);
        check(timeline.remaining() == 0, "replacement without packet clears stale visuals");
        timeline.accept(first, 50, 100, 100);
        timeline.accept(first, 51, 0, 0);
        timeline.accept(first, 52, 0, 0);
        check(timeline.remaining() == 0 && timeline.total() == 0, "explicit clear is idempotent");
        timeline.accept(first, 60, 1, 1);
        timeline.tick(first);
        check(timeline.remaining() == 0, "natural expiry");
        timeline.accept(first, 61, 100, 100);
        timeline.reset();
        timeline.tick(first);
        check(timeline.remaining() == 0, "logout drops stale state");
        timeline.accept(null, 0, 100, 100);
        check(timeline.remaining() == 0, "no episode without a player");
        timeline.accept(first, 0, 10, 10, 30, 30, 5, 4, false);
        for (int i = 0; i < 5; i++) timeline.tick(first);
        check(Math.abs(timeline.sample(0) - 1) < .0001, "short episode reaches full intensity with proportional fades");
        for (int i = 0; i < 3; i++) timeline.tick(first);
        float before = timeline.sample(.5F);
        timeline.accept(first, 8, 100, 100, 30, 30, 5, 4, false);
        check(Math.abs(timeline.sample(.5F) - before) < .0001, "refresh during fade-out is continuous");
        for (int i = 0; i < 6; i++) timeline.tick(first);
        check(timeline.sample(0) == 1, "refresh returns smoothly to full intensity");
        timeline.accept(first, 14, 0, 0, 30, 30, 5, 4, false);
        check(timeline.remaining() == 0 && timeline.sample(0) == 1, "milk clears logic immediately but retains visual release");
        timeline.tick(first); timeline.tick(first);
        check(Math.abs(timeline.sample(0) - .5) < .0001, "release midpoint");
        timeline.accept(first, 16, 0, 0, 30, 30, 5, 4, false);
        timeline.tick(first); timeline.tick(first);
        check(timeline.sample(0) == 0, "repeated clear does not extend release");
        timeline.accept(first, 20, 100, 100, 0, 30, 5, 4, false);
        check(timeline.sample(0) == 1, "zero fade-in");
        timeline.accept(first, 20, 0, 0, 30, 30, 5, 4, true);
        check(timeline.sample(0) == 0, "hard clear has no residue");
        var style = new com.thelongtravail.client.VisualDeprivationStyle(.65F,.98F,.45F,.45F,1,1.25F,5,4,.5F,0);
        check(style.pulse((float)Math.PI, false) == .5F && style.pulse(0, false) == 1, "independent pulse range");
        check(style.pulse((float)Math.PI, true) == 1, "reduce motion suppresses pulse");
        System.out.println("PASS: owner/packet order, proportional short fades, continuous refresh, release/repeated clear, hard clear, expiry, logout, pulse/reduced motion");
    }
}

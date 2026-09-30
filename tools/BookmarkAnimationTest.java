import com.thelongtravail.client.BookmarkAnimation;

public class BookmarkAnimationTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        float expected = BookmarkAnimation.approach(0, 8, 0.5);
        for (int fps : new int[]{30, 60, 144, 240}) {
            float position = 0;
            for (int frame = 0; frame < fps / 2; frame++) {
                float next = BookmarkAnimation.approach(position, 8, 1.0 / fps);
                check(next >= position && next <= 8, "Extension must not overshoot");
                position = next;
            }
            check(Math.abs(position - expected) < 0.00001, "Equal elapsed time must give equal positions");
        }
        float position = BookmarkAnimation.approach(0, 8, 1.0 / 144);
        check(position > 0 && position < 1, "Subpixel movement must remain available");
        float reversed = BookmarkAnimation.approach(position, 0, 1.0 / 144);
        check(reversed > 0 && reversed < position, "Reversal must continue from the current position");
        check(BookmarkAnimation.approach(3, 8, 0) == 3, "No jump at zero elapsed time");
        check(BookmarkAnimation.approach(3, 8, -1) == 3, "Negative time must be ignored");
        check(BookmarkAnimation.approach(0, 8, 10) == 8, "Long frames must converge without overshooting");
        System.out.println("PASS: frame-rate independence, subpixel motion, reversal and bounds");
    }
}

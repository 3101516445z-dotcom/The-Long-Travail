import com.thelongtravail.data.FlourishingBonus;
import java.util.Map;
import java.util.Set;

public class JourneyAndSoundTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        check(Math.abs(FlourishingBonus.calculate(12, 0.05, 0) - 0.6) < 1e-9, "12 unique biomes give +60%");
        check(FlourishingBonus.calculate(0, 0.05, 0) == 0, "Empty history gives no bonus");
        check(FlourishingBonus.calculate(100, 0.05, 2) == 2, "Display and healing must respect cap");
        check(FlourishingBonus.calculate(100, 0.05, 0) == 5, "Zero cap means unlimited");
        System.out.println("PASS: healing totals/cap");
    }
}

import com.thelongtravail.client.DiaryHoldProgress;

public class DiaryHoldProgressTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        DiaryHoldProgress charge = new DiaryHoldProgress();
        check(!charge.advance(1.49, true), "Must not open before 1.5 seconds");
        check(charge.advance(0.02, true), "Must open at 1.5 seconds");
        check(charge.fraction() == 1, "Charge must clamp at full");
        charge.reset();
        charge.advance(0.9, true);
        charge.advance(0.3, false);
        check(Math.abs(charge.fraction() - 0.4) < 0.000001, "Release must visibly retreat");
        check(!charge.advance(0.8, true), "Resuming must retain remaining progress");
        check(charge.advance(0.11, true), "Resumed charge must complete");
        charge.advance(10, false);
        check(charge.fraction() == 0, "Release must clamp at empty");
        for (int fps : new int[]{30, 60, 144}) {
            charge.reset();
            for (int frame = 0; frame < fps; frame++) charge.advance(1.0 / fps, true);
            check(Math.abs(charge.fraction() - 2.0 / 3.0) < 0.000001, "Charge must be frame-rate independent");
        }
        charge.reset();
        check(charge.fraction() == 0, "Changing target must start empty");
        System.out.println("PASS: hold duration, release, resume, reset, bounds and frame-rate independence");
    }
}

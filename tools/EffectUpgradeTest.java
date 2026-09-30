import com.thelongtravail.data.EffectUpgrade;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

public class EffectUpgradeTest {
    private static void check(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
    }

    public static void main(String[] args) {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        MobEffect effect = new MobEffect(MobEffectCategory.BENEFICIAL, 0) {};
        MobEffectInstance first = new MobEffectInstance(effect, 200, 0, true, false, false);
        EffectUpgrade.apply(first, 2, 5);
        check(first.getAmplifier() == 2 && first.getDuration() == 200, "First insertion keeps upgraded input");
        check(first.isAmbient() && !first.isVisible() && !first.showIcon(), "Preserve presentation flags");
        MobEffectInstance renewal = new MobEffectInstance(effect, 400, 0);
        EffectUpgrade.apply(renewal, 2, 5);
        first.update(renewal);
        check(first.getAmplifier() == 2 && first.getDuration() == 400, "Renewal merges upgraded input");
        MobEffectInstance high = new MobEffectInstance(effect, 600, 6);
        high.update(renewal);
        check(high.getAmplifier() == 6, "Lower input never replaces stronger existing effect");
        EffectUpgrade.apply(high, 2, 5);
        check(high.getAmplifier() == 6, "Cap does not downgrade an incoming stronger effect");
        MobEffectInstance capped = new MobEffectInstance(effect, 100, 3);
        EffectUpgrade.apply(capped, 20, 5);
        check(capped.getAmplifier() == 4, "Configured maximum respected");
        MobEffectInstance infinite = new MobEffectInstance(effect, -1, 0);
        EffectUpgrade.apply(infinite, 1, 5);
        check(infinite.getDuration() == -1 && infinite.getAmplifier() == 1, "Infinite duration preserved");
        System.out.println("PASS: first gain, renewal, stronger existing effect, cap, flags, infinite duration.");
    }
}

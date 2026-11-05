package travail.smoke;

import com.thelongtravail.common.TickDurations;
import com.thelongtravail.config.FlourishingItemsConfig;
import com.thelongtravail.config.RainConfig;
import com.thelongtravail.integration.MaidIntegration;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

@Mod("travail_smoke")
public final class StructureRegression {
    private long assertions;

    public StructureRegression() {
        MinecraftForge.EVENT_BUS.addListener(this::start);
    }

    private void equal(int actual, int expected, double seconds, String policy) {
        assertions++;
        if (actual != expected)
            throw new AssertionError(
                    policy
                            + " input="
                            + Double.toHexString(seconds)
                            + " expected="
                            + expected
                            + " actual="
                            + actual);
    }

    private void sample(double s, Method effect) throws Exception {
        int nonNegative = Math.max(0, (int) Math.ceil(s * 20));
        int signed = (int) Math.min(Integer.MAX_VALUE, Math.ceil(s * 20));
        int rounded =
                s <= 0.0D
                        ? 1
                        : (int) Math.max(1L, Math.min(Integer.MAX_VALUE, Math.round(s * 20.0D)));
        equal(TickDurations.ceilNonNegative(s), nonNegative, s, "common ceil nonnegative");
        equal(RainConfig.ticks(s), nonNegative, s, "rain wrapper");
        equal(TickDurations.ceilSigned(s), signed, s, "common ceil signed");
        equal(FlourishingItemsConfig.ticks(s), signed, s, "flourishing wrapper");
        equal(TickDurations.roundAtLeastOne(s), rounded, s, "common round");
        equal((int) effect.invoke(null, s), rounded, s, "effect wrapper");
    }

    private void start(ServerStartedEvent event) {
        boolean pass = false;
        String failureText = "";
        long start = System.nanoTime();
        try {
            Method effect =
                    Class.forName("com.thelongtravail.runtime.effects.TravailEffectActions")
                            .getDeclaredMethod("secondsToTicks", double.class);
            effect.setAccessible(true);
            double[] edges = {
                0.0D,
                -0.0D,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.MAX_VALUE,
                -Double.MAX_VALUE,
                Double.MIN_VALUE,
                -Double.MIN_VALUE,
                0.025D,
                0.05D,
                -0.025D,
                -0.05D,
                Integer.MAX_VALUE / 20.0D,
                Integer.MIN_VALUE / 20.0D
            };
            for (double s : edges) {
                sample(s, effect);
                sample(Math.nextDown(s), effect);
                sample(Math.nextUp(s), effect);
            }
            Random random = new Random(20261024L);
            for (int i = 0; i < 200000; i++) {
                sample(Double.longBitsToDouble(random.nextLong()), effect);
                sample((random.nextDouble() - 0.5D) * 100000.0D, effect);
            }
            if (net.minecraftforge.fml.ModList.get().isLoaded("touhou_little_maid"))
                throw new AssertionError("Optional-mod absence scenario unexpectedly loaded TLM");
            var cow =
                    net.minecraft.world.entity.EntityType.COW.create(event.getServer().overworld());
            if (cow == null
                    || com.thelongtravail.integration.MaidIntegration.isMaid(cow)
                    || !com.thelongtravail.integration.MaidIntegration.casterId(cow)
                            .equals(cow.getUUID()))
                throw new AssertionError("Optional-mod facade fallback failed");
            int[] phases = {
                MaidIntegration.lorentzPhase(cow),
                MaidIntegration.quixotePhase(cow),
                MaidIntegration.flowerPhase(cow),
                MaidIntegration.lafitePhase(cow),
                MaidIntegration.phosphophyllitePhase(cow),
                MaidIntegration.relicPhase(cow)
            };
            for (int phase : phases)
                if (phase != 0) throw new AssertionError("Absent maid phase is active");
            boolean[] rules = {
                MaidIntegration.quixote(cow),
                MaidIntegration.lafite(cow),
                MaidIntegration.seesUndead(cow),
                MaidIntegration.lantern(cow)
            };
            for (boolean rule : rules)
                if (rule) throw new AssertionError("Absent maid rule is active");
            var stacks =
                    new net.minecraft.world.item.ItemStack[] {
                        MaidIntegration.lorentzStack(cow),
                        MaidIntegration.flowerStack(cow),
                        MaidIntegration.relicStack(cow, true),
                        MaidIntegration.relicStack(cow, false),
                        MaidIntegration.springCrown(cow)
                    };
            for (var stack : stacks)
                if (!stack.isEmpty()) throw new AssertionError("Absent maid stack is nonempty");
            var damage = cow.damageSources().mobAttack(cow);
            if (MaidIntegration.bonus(cow, damage) != 0
                    || MaidIntegration.witherBonus(cow, damage) != 0
                    || MaidIntegration.immune(cow, damage, 7)
                    || MaidIntegration.reduce(cow, damage, 7, true) != 7)
                throw new AssertionError("Absent maid combat calculation fallback failed");
            MaidIntegration.received(cow, damage);
            MaidIntegration.confirmed(cow, damage, true);
            MaidIntegration.successful(cow, damage);
            MaidIntegration.retaliate(cow, damage);
            var unchanged =
                    new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.REGENERATION, 40);
            if (MaidIntegration.extend(cow, unchanged, cow) != unchanged)
                throw new AssertionError("Absent maid effect identity changed");
            System.out.println(
                    "MAID_ABSENT_FACADE_PASS ruleChecks=15 calculationChecks=4 feedbackCalls=4"
                            + " effectIdentityChecks=1");
            pass = true;
        } catch (Throwable failure) {
            failure.printStackTrace();
            failureText = failure.toString();
        } finally {
            try {
                Files.writeString(
                        Path.of("checks.txt"),
                        "assertions="
                                + assertions
                                + "\nelapsedMs="
                                + (System.nanoTime() - start) / 1000000L
                                + "\nfailure="
                                + failureText
                                + "\n");
                Files.writeString(Path.of("result.txt"), pass ? "PASS" : "FAIL");
            } catch (Exception failure) {
                throw new RuntimeException(failure);
            }
            System.out.println(
                    "STRUCTURE_ROUND_01_" + (pass ? "PASS" : "FAIL") + " assertions=" + assertions);
            event.getServer().halt(false);
        }
    }
}

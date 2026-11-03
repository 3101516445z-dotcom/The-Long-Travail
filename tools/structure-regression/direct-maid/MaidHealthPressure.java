package travail.directtest;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.thelongtravail.compat.maidspell.MaidSpellHealth;

import net.minecraft.world.entity.ai.attributes.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.*;
import java.util.UUID;

@Mod("travail_smoke")
public final class MaidHealthPressure {
    public MaidHealthPressure() {
        MinecraftForge.EVENT_BUS.addListener(this::run);
    }

    private static int checks;

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }

    private static void health(EntityMaid maid, float value) {
        if (ModList.get().isLoaded("touhou_little_maid_spell")) Spell.write(maid, value);
        else maid.setHealth(value);
    }

    private static final class Spell {
        static void write(EntityMaid maid, float value) {
            com.github.yimeng261.maidspell.utils.MaidHealthWriteGuard.runBypassing(
                    () ->
                            maid.getEntityData()
                                    .set(
                                            com.github.yimeng261.maidspell.mixin.accessor
                                                    .LivingEntityHealthAccessor
                                                    .maidspell$getHealthAccessor(),
                                            value));
        }
    }

    private void run(ServerStartedEvent event) {
        var server = event.getServer();
        long started = System.nanoTime();
        try {
            var maid = new EntityMaid(server.overworld());
            var attribute = maid.getAttribute(Attributes.MAX_HEALTH);
            attribute.setBaseValue(20);
            var external =
                    new AttributeModifier(
                            UUID.randomUUID(),
                            "retained external bonus",
                            7,
                            AttributeModifier.Operation.ADDITION);
            attribute.addTransientModifier(external);
            for (int i = 0; i < 20000; i++) {
                var bonus =
                        new AttributeModifier(
                                UUID.randomUUID(),
                                "removed accessory",
                                20 + i % 80,
                                AttributeModifier.Operation.ADDITION);
                attribute.addTransientModifier(bonus);
                health(maid, maid.getMaxHealth());
                attribute.removeModifier(bonus);
                check(maid.getHealth() > maid.getMaxHealth(), "fixture is over cap");
                MaidSpellHealth.clampAfterModifierRemoval(maid);
                check(
                        maid.getHealth() == maid.getMaxHealth() && maid.getMaxHealth() == 27,
                        "clamp after removal");
                check(attribute.hasModifier(external), "unrelated modifier retained");
                health(maid, 10);
                MaidSpellHealth.clampAfterModifierRemoval(maid);
                check(maid.getHealth() == 10, "below cap unchanged");
            }
            Files.writeString(
                    Path.of("maid-health-result.txt"),
                    "PASS checks="
                            + checks
                            + " removals=20000 spell="
                            + ModList.get().isLoaded("touhou_little_maid_spell")
                            + " elapsedMs="
                            + (System.nanoTime() - started) / 1000000);
        } catch (Throwable failure) {
            failure.printStackTrace();
            try {
                Files.writeString(Path.of("maid-health-result.txt"), "FAIL " + failure);
            } catch (Exception ignored) {
            }
        } finally {
            server.halt(false);
        }
    }
}

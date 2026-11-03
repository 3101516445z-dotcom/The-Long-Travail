package com.thelongtravail.compat.maidspell;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

public final class MaidSpellHealth {
    private static boolean inspected;
    private static boolean guardedWrite;
    private static EntityDataAccessor<Float> health;

    @SuppressWarnings("unchecked")
    private static void inspect() {
        if (inspected) return;
        inspected = true;
        if (!ModList.get().isLoaded("touhou_little_maid_spell")) return;
        try {
            health = com.thelongtravail.compat.DirectCalls.call(Direct::health);
            guardedWrite = true;
        } catch (ReflectiveOperationException failure) {
            org.slf4j.LoggerFactory.getLogger("the_long_travail.compat")
                    .warn(
                            "MaidSpell health-cap API unavailable; retaining normal health setter",
                            failure);
        }
    }

    public static void clampAfterModifierRemoval(LivingEntity maid) {
        float maximum = maid.getMaxHealth();
        if (!Float.isFinite(maximum) || maximum <= 0 || maid.getHealth() <= maximum) return;
        inspect();
        if (guardedWrite) {
            try {
                com.thelongtravail.compat.DirectCalls.call(
                        () -> {
                            Direct.write(() -> maid.getEntityData().set(health, maximum));
                            return null;
                        });
                return;
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException(
                        "MaidSpell failed to reconcile removed health modifier", failure);
            }
        }
        maid.setHealth(maximum);
    }

    private static final class Direct {
        private static final java.util.function.Consumer<Runnable> WRITE =
                com.github.yimeng261.maidspell.utils.MaidHealthWriteGuard::runBypassing;

        static EntityDataAccessor<Float> health() {
            return com.github.yimeng261.maidspell.mixin.accessor.LivingEntityHealthAccessor
                    .maidspell$getHealthAccessor();
        }

        static void write(Runnable action) {
            WRITE.accept(action);
        }
    }

    private MaidSpellHealth() {}
}

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

    // 校验预计算的非致死效果损失，并在女仆现有生命写入作用域内应用。
    public static boolean applyNonlethalEffectLoss(LivingEntity maid, float nextHealth) {
        if (!com.thelongtravail.integration.MaidIntegration.isMaid(maid)) return false;
        inspect();
        if (!guardedWrite) return false;
        if (maid.level().isClientSide
                || !maid.isAlive()
                || com.thelongtravail.runtime.effects.HealthLoss.blocks(maid, nextHealth)
                || !Float.isFinite(nextHealth)
                || nextHealth < 1
                || nextHealth >= maid.getHealth()) return true;
        try {
            com.thelongtravail.compat.DirectCalls.call(
                    () -> {
                        Direct.write(() -> maid.getEntityData().set(health, nextHealth));
                        return null;
                    });
            return true;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(
                    "MaidSpell failed to apply nonlethal effect loss", failure);
        }
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

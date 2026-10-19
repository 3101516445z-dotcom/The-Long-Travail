package com.thelongtravail.compat.maidspell;

import java.lang.reflect.Method;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

public final class MaidSpellHealth {
    private static boolean inspected;
    private static Method guardedWrite;
    private static EntityDataAccessor<Float> health;

    @SuppressWarnings("unchecked")
    private static void inspect() {
        if (inspected) return;
        inspected = true;
        if (!ModList.get().isLoaded("touhou_little_maid_spell")) return;
        try {
            health =
                    (EntityDataAccessor<Float>)
                            Class.forName("com.github.yimeng261.maidspell.mixin.accessor.LivingEntityHealthAccessor")
                                    .getMethod("maidspell$getHealthAccessor")
                                    .invoke(null);
            guardedWrite =
                    Class.forName("com.github.yimeng261.maidspell.utils.MaidHealthWriteGuard")
                            .getMethod("runBypassing", Runnable.class);
        } catch (ReflectiveOperationException failure) {
            org.slf4j.LoggerFactory.getLogger("the_long_travail.compat")
                    .warn("MaidSpell health-cap API unavailable; retaining normal health setter", failure);
        }
    }

    public static void clampAfterModifierRemoval(LivingEntity maid) {
        float maximum = maid.getMaxHealth();
        if (!Float.isFinite(maximum) || maximum <= 0 || maid.getHealth() <= maximum) return;
        inspect();
        if (guardedWrite != null) {
            try {
                guardedWrite.invoke(null, (Runnable) () -> maid.getEntityData().set(health, maximum));
                return;
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("MaidSpell failed to reconcile removed health modifier", failure);
            }
        }
        maid.setHealth(maximum);
    }

    private MaidSpellHealth() {}
}

package com.thelongtravail.compat.maidspell;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.mixin.accessor.LivingEntityHealthAccessor;
import com.github.yimeng261.maidspell.utils.MaidHealthWriteGuard;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import com.thelongtravail.integration.MaidIntegration;
import com.thelongtravail.runtime.effects.HealthLoss;
import net.minecraftforge.fml.ModList;

public final class MaidSpellHealth {
    private MaidSpellHealth() {}

    private static boolean available() {
        return ModList.get().isLoaded("touhou_little_maid_spell") && Guard.WRITE != null;
    }

    public static void clampAfterModifierRemoval(LivingEntity maid) {
        float maximum = maid.getMaxHealth();
        if (!Float.isFinite(maximum) || maximum <= 0 || maid.getHealth() <= maximum) return;
        if (available()) Guard.lower(maid, maximum);
        else maid.setHealth(maximum);
    }

    public static boolean applyNonlethalEffectLoss(LivingEntity maid, float nextHealth) {
        if (!MaidIntegration.isMaid(maid) || !available()) return false;
        if (!HealthLoss.blocks(maid, nextHealth)) Guard.lower(maid, nextHealth);
        return true;
    }

    // 首次使用时解析受限写入接口，按方法签名启用联动。
    private static final class Guard {
        static final MethodHandle WRITE = resolve();

        private static MethodHandle resolve() {
            try {
                var lookup =
                        MethodHandles.privateLookupIn(
                                MaidHealthWriteGuard.class, MethodHandles.lookup());
                var signature =
                        MethodType.methodType(
                                void.class,
                                EntityMaid.class,
                                EntityDataAccessor.class,
                                float.class,
                                boolean.class);
                var write =
                        lookup.findStatic(MaidHealthWriteGuard.class, "writeAuthorized", signature);
                return MethodHandles.insertArguments(
                        write, 1, LivingEntityHealthAccessor.maidspell$getHealthAccessor());
            } catch (ReflectiveOperationException | LinkageError failure) {
                org.slf4j.LoggerFactory.getLogger("the_long_travail.compat")
                        .warn("MaidSpell guarded health write unavailable", failure);
                return null;
            }
        }

        static void lower(LivingEntity entity, float target) {
            float current = entity.getHealth();
            if (!(entity instanceof EntityMaid maid)
                    || entity.level().isClientSide
                    || !entity.isAlive()
                    || !Float.isFinite(current)
                    || !Float.isFinite(entity.getMaxHealth())
                    || !Float.isFinite(target)
                    || target < 1
                    || target >= current) return;
            try {
                WRITE.invokeExact(maid, target, false);
            } catch (RuntimeException | Error failure) {
                throw failure;
            } catch (Throwable failure) {
                throw new IllegalStateException("MaidSpell guarded health write failed", failure);
            }
        }
    }
}

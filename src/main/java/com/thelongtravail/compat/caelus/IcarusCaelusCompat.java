package com.thelongtravail.compat.caelus;

import com.thelongtravail.compat.OptionalCompatState;
import net.minecraft.world.entity.LivingEntity;
import top.theillusivec4.caelus.api.CaelusApi;

public final class IcarusCaelusCompat {
    private IcarusCaelusCompat() {}

    public static boolean denied(LivingEntity entity) {
        if (!OptionalCompatState.CAELUS.enabled()) return false;
        try {
            return Access.denied(entity);
        } catch (LinkageError failure) {
            OptionalCompatState.CAELUS.disable(failure.toString());
            return false;
        }
    }

    private static final class Access {
        static boolean denied(LivingEntity entity) {
            return CaelusApi.getInstance().canFallFly(entity) == CaelusApi.TriState.DENY;
        }
    }
}

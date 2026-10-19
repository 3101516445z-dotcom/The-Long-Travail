package com.thelongtravail.compat.caelus;

import net.minecraft.world.entity.LivingEntity;
import top.theillusivec4.caelus.api.CaelusApi;

public final class IcarusCaelusCompat {
    private IcarusCaelusCompat() {}

    public static boolean denied(LivingEntity entity) {
        return net.minecraftforge.fml.ModList.get().isLoaded("caelus") && Access.denied(entity);
    }

    private static final class Access {
        static boolean denied(LivingEntity entity) {
            return CaelusApi.getInstance().canFallFly(entity) == CaelusApi.TriState.DENY;
        }
    }
}

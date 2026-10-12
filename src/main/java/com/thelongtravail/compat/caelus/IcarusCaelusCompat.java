package com.thelongtravail.compat.caelus;

import net.minecraft.world.entity.LivingEntity;
import top.theillusivec4.caelus.api.CaelusApi;

// 入口不要求 Caelus；仅在检测到它后加载内部 API 调用类。
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

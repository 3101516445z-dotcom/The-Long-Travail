package com.thelongtravail.farreach;

import net.minecraft.world.entity.LivingEntity;
import top.theillusivec4.caelus.api.CaelusApi;

// 仅在 ModList 确认可选依赖 Caelus 存在后加载。
final class IcarusCaelusCompat {
    private IcarusCaelusCompat() {}
    static boolean denied(LivingEntity entity) {
        return CaelusApi.getInstance().canFallFly(entity) == CaelusApi.TriState.DENY;
    }
}

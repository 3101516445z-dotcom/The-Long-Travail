package com.thelongtravail.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;


/**
 * 权威状态保存在玩家数据中，此实例仅提供 HUD 图标，不修改属性或执行周期逻辑。
 * 保留原版治疗物品规则，使牛奶可以解除该状态。
 */
public class VisualDeprivationEffect extends MobEffect {
    public VisualDeprivationEffect() {
        super(MobEffectCategory.HARMFUL, 0x2A2A33);

    }
}

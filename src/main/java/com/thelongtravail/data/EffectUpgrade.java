package com.thelongtravail.data;

import net.minecraft.world.effect.MobEffectInstance;

/** 直接修改 Forge 随后将插入或合并的输入实例，避免重入。 */
public final class EffectUpgrade {
    public static void apply(MobEffectInstance added, int bonus, int maxLevel) {
        int amplifier = (int) Math.min((long) added.getAmplifier() + bonus, (long) maxLevel - 1);
        if (amplifier <= added.getAmplifier()) return;
        added.update(new MobEffectInstance(added.getEffect(), added.getDuration(), amplifier,
                added.isAmbient(), added.isVisible(), added.showIcon()));
    }

    private EffectUpgrade() {}
}

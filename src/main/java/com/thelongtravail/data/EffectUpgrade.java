package com.thelongtravail.data;

import net.minecraft.world.effect.MobEffectInstance;

/** Mutate the input that Forge will subsequently insert or merge, without re-entry. */
public final class EffectUpgrade {
    public static void apply(MobEffectInstance added, int bonus, int maxLevel) {
        int amplifier = (int) Math.min((long) added.getAmplifier() + bonus, (long) maxLevel - 1);
        if (amplifier <= added.getAmplifier()) return;
        added.update(new MobEffectInstance(added.getEffect(), added.getDuration(), amplifier,
                added.isAmbient(), added.isVisible(), added.showIcon()));
    }

    private EffectUpgrade() {}
}

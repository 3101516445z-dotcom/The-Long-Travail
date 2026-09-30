package com.thelongtravail.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;


/**
 * Purely visual malice effect.
 *
 * The authoritative state lives in the player data, so this instance only supplies the HUD icon
 * only. No attribute modifiers and no
 * periodic logic, so it never changes gameplay. Curative items stay at the vanilla default,
 * which keeps milk a legitimate way out.
 */
public class VisualDeprivationEffect extends MobEffect {
    public VisualDeprivationEffect() {
        super(MobEffectCategory.HARMFUL, 0x2A2A33);

    }
}

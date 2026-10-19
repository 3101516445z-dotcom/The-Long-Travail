package com.thelongtravail.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MobEffectInstance.class)
public interface EffectDurationAccessor {
    @Accessor("amplifier")
    void travail$amplifier(int amplifier);

    @Accessor("duration")
    void travail$duration(int ticks);

    @Accessor("hiddenEffect")
    void travail$hiddenEffect(MobEffectInstance hidden);

    @Accessor("hiddenEffect")
    MobEffectInstance travail$hiddenEffect();
}

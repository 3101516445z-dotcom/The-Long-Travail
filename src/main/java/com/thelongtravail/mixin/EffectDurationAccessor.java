package com.thelongtravail.mixin;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(MobEffectInstance.class)
public interface EffectDurationAccessor {
    @Accessor("duration") void travail$duration(int ticks);
    @Accessor("hiddenEffect") void travail$hiddenEffect(MobEffectInstance hidden);
}

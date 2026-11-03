package com.thelongtravail.mixin.combat;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface DamageReplayStateAccessor {
    @Accessor("lastHurt")
    float travail$lastHurt();

    @Accessor("lastHurt")
    void travail$lastHurt(float amount);
}

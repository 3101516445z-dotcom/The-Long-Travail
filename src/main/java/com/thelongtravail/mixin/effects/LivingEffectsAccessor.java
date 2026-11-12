package com.thelongtravail.mixin.effects;

import java.util.Map;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.world.entity.Entity;

@Mixin(LivingEntity.class)
public interface LivingEffectsAccessor {
    @Accessor("activeEffects")
    Map<MobEffect, MobEffectInstance> travail$activeEffects();

    @Accessor("effectsDirty")
    void travail$effectsDirty(boolean dirty);

    @Invoker("onEffectAdded")
    void travail$onEffectAdded(MobEffectInstance effect, Entity source);
}

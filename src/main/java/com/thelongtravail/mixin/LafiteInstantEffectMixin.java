package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.data.IncomingEffectLevels;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MobEffect.class)
public abstract class LafiteInstantEffectMixin {
    @WrapMethod(method = "applyInstantenousEffect")
    private void travail$saturation(
            Entity source, Entity owner, LivingEntity target, int amplifier, double potency, Operation<Void> original) {
        if ((Object) this == MobEffects.SATURATION && com.thelongtravail.abyss.MidnightLafite.equipped(target))
            amplifier = IncomingEffectLevels.adjusted(target, MobEffects.SATURATION, amplifier);
        original.call(source, owner, target, amplifier, potency);
    }
}

package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.combat.DamageReplay;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class DamageReplayMixin {
    @WrapMethod(method = "getDamageAfterArmorAbsorb")
    private float travail$replayArmor(DamageSource source, float amount, Operation<Float> original) {
        return DamageReplay.active((LivingEntity) (Object) this, source) ? amount : original.call(source, amount);
    }

    @WrapMethod(method = "getDamageAfterMagicAbsorb")
    private float travail$replayMagic(DamageSource source, float amount, Operation<Float> original) {
        float result =
                DamageReplay.active((LivingEntity) (Object) this, source) ? amount : original.call(source, amount);
        var context = com.thelongtravail.helper.CombatContext.current((LivingEntity) (Object) this, source);
        return context == null ? result : context.beforeAbsorption(result);
    }

    @WrapMethod(method = "isDamageSourceBlocked")
    private boolean travail$replayShield(DamageSource source, Operation<Boolean> original) {
        return !DamageReplay.active((LivingEntity) (Object) this, source) && original.call(source);
    }
}

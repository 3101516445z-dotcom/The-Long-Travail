package com.thelongtravail.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.flourishing.FloralEvents;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(MobEffect.class)
public abstract class FloralPoisonMixin {
    @WrapMethod(method="applyEffectTick")
    private void travail$poison(LivingEntity entity,int amplifier,Operation<Void> original) {
        var old=FloralEvents.poisonScope((Object)this==MobEffects.POISON?entity:null);
        try{original.call(entity,amplifier);}finally{FloralEvents.restorePoison(old);}
    }
}

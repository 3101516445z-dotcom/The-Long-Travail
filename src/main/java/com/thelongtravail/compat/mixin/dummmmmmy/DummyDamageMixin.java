package com.thelongtravail.compat.mixin.dummmmmmy;

import com.thelongtravail.helper.CombatContext;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mehvahdjukaar.dummmmmmy.common.TargetDummyEntity", remap = false)
public abstract class DummyDamageMixin {
    @Inject(
            method = "onActuallyDamagedOrTrueDamageDetected",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/mehvahdjukaar/dummmmmmy/common/TargetDummyEntity;totalDamageTakenInCombat:F",
                            opcode = Opcodes.PUTFIELD,
                            shift = At.Shift.AFTER),
            remap = false)
    private void travail$recordDamage(float amount, DamageSource source, CallbackInfo ci) {
        CombatContext.recordDummyDamage((LivingEntity) (Object) this, source, amount);
    }
}

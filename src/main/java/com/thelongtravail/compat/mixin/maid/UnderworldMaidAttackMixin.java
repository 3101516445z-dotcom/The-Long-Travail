package com.thelongtravail.compat.mixin.maid;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Mob.class)
public abstract class UnderworldMaidAttackMixin {
    @WrapOperation(
            method = "doHurtTarget",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;getMobType()Lnet/minecraft/world/entity/MobType;"))
    private MobType travail$undead(LivingEntity target, Operation<MobType> original) {
        return target != (Object) this
                        && com.thelongtravail.compat.maid.MaidAccessories.seesUndead(
                                (Mob) (Object) this)
                ? MobType.UNDEAD
                : original.call(target);
    }
}

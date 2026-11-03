package com.thelongtravail.mixin.underworld;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.underworld.AttackMobTypeContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Mob.class)
public abstract class UnderworldMobAttackMixin {
    // 生物的独立攻击不能继承外层附魔目标视角；自身附魔回调会重新建立作用域。
    @WrapMethod(method = "doHurtTarget")
    private boolean travail$separateAttack(Entity target, Operation<Boolean> original) {
        try (var ignored = AttackMobTypeContext.suspend()) {
            return original.call(target);
        }
    }
}

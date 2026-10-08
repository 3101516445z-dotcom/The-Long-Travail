package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.thelongtravail.underworld.UnderworldItems;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.enchantment.DamageEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DamageEnchantment.class)
public abstract class UnderworldEnchantmentMixin {
    @WrapOperation(method="doPostAttack",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;getMobType()Lnet/minecraft/world/entity/MobType;"))
    private MobType travail$undead(LivingEntity target,Operation<MobType> original,LivingEntity attacker,Entity victim,int level) {
        return attacker!=target&&UnderworldItems.seesUndead(attacker)?MobType.UNDEAD:original.call(target);
    }
}

package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.thelongtravail.underworld.UnderworldItems;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class UnderworldAttackMixin {
    @WrapOperation(method="attack",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;getMobType()Lnet/minecraft/world/entity/MobType;"))
    private MobType travail$undead(LivingEntity target,Operation<MobType> original) {
        return target!=(Object)this&&UnderworldItems.seesUndead((Player)(Object)this)?MobType.UNDEAD:original.call(target);
    }
}

package com.thelongtravail.mixin.underworld;

import com.thelongtravail.underworld.AttackMobTypeContext;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 原版多个实体重写了类型查询，因此同时覆盖这些实现。
@Mixin({
    net.minecraft.world.entity.LivingEntity.class,
    net.minecraft.world.entity.animal.Bee.class,
    net.minecraft.world.entity.animal.Turtle.class,
    net.minecraft.world.entity.animal.WaterAnimal.class,
    net.minecraft.world.entity.animal.axolotl.Axolotl.class,
    net.minecraft.world.entity.animal.horse.SkeletonHorse.class,
    net.minecraft.world.entity.animal.horse.ZombieHorse.class,
    net.minecraft.world.entity.boss.wither.WitherBoss.class,
    net.minecraft.world.entity.monster.AbstractIllager.class,
    net.minecraft.world.entity.monster.AbstractSkeleton.class,
    net.minecraft.world.entity.monster.Endermite.class,
    net.minecraft.world.entity.monster.Guardian.class,
    net.minecraft.world.entity.monster.Phantom.class,
    net.minecraft.world.entity.monster.Silverfish.class,
    net.minecraft.world.entity.monster.Spider.class,
    net.minecraft.world.entity.monster.Zoglin.class,
    net.minecraft.world.entity.monster.Zombie.class
})
public abstract class AttackMobTypeMixin {
    @Inject(method = "getMobType", at = @At("RETURN"), cancellable = true, require = 1)
    private void travail$attackType(CallbackInfoReturnable<MobType> cir) {
        cir.setReturnValue(
                AttackMobTypeContext.resolve((LivingEntity) (Object) this, cir.getReturnValue()));
    }
}

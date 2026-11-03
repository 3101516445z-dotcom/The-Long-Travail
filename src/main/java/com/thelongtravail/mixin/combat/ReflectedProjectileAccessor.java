package com.thelongtravail.mixin.combat;

import net.minecraft.world.entity.projectile.Projectile;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Projectile.class)
public interface ReflectedProjectileAccessor {
    @Accessor("leftOwner")
    void travail$leftOwner(boolean value);
}

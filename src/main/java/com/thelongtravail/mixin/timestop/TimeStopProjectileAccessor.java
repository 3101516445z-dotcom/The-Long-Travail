package com.thelongtravail.mixin.timestop;

import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Projectile.class)
public interface TimeStopProjectileAccessor {
    @Accessor("ownerUUID")
    java.util.UUID travail$ownerUUID();
}

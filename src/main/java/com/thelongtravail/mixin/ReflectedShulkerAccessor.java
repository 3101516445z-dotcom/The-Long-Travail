package com.thelongtravail.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ShulkerBullet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(ShulkerBullet.class)
public interface ReflectedShulkerAccessor {
    @Accessor("finalTarget")
    void travail$target(Entity target);

    @Accessor("targetId")
    void travail$targetId(UUID target);
}

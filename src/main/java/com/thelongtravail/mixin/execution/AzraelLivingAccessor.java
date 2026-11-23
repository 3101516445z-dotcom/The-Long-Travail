package com.thelongtravail.mixin.execution;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface AzraelLivingAccessor {
    @Accessor("dead")
    boolean travail$dead();

    @Accessor("lastHurtByPlayer")
    net.minecraft.world.entity.player.Player travail$lastPlayer();

    @Accessor("lastHurtByPlayerTime")
    int travail$recentPlayerHit();

    @Accessor("lastHurtByPlayerTime")
    void travail$recentPlayerHit(int ticks);
}

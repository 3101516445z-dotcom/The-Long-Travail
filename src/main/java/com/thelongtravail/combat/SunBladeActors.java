package com.thelongtravail.combat;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.compat.maid.MaidAccessories;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** 只解析效果主体；不修改伤害的直接来源或归属来源。 */
public final class SunBladeActors {
    public static LivingEntity attacker(DamageSource source) {
        return source.getEntity() instanceof ServerPlayer player
                ? player
                : MaidAccessories.damageActor(source);
    }

    public static TravailStateApi.AspectState phase(LivingEntity actor, TravailAspect aspect) {
        return actor instanceof ServerPlayer player
                ? TravailStateApi.state(player, aspect)
                : MaidAccessories.phase(actor, aspect);
    }

    private SunBladeActors() {}
}

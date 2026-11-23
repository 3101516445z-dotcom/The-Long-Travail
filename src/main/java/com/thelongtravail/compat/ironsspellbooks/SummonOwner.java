package com.thelongtravail.compat.ironsspellbooks;

import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;

import net.minecraft.world.entity.Entity;

public final class SummonOwner {
    public static Entity get(Entity entity) {
        return entity instanceof IMagicSummon summon ? summon.getSummoner() : null;
    }

    private SummonOwner() {}
}

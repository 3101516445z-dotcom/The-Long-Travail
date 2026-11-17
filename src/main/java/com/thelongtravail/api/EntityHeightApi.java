package com.thelongtravail.api;

import net.minecraft.world.entity.Entity;

// 实体原点的绝对 Y 坐标差；不修正体型或追踪多部件归属。
public final class EntityHeightApi {
    public static double heightDifference(Entity first, Entity second) {
        return Math.abs(first.getY() - second.getY());
    }

    private EntityHeightApi() {}
}

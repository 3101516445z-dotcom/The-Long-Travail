package com.thelongtravail.combat;

import com.thelongtravail.registry.ModAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

public final class CombatAttributes {
    private static double value(LivingEntity entity, Attribute attribute, double maximum) {
        var instance = entity.getAttribute(attribute);
        double value = instance == null ? 0 : instance.getValue();
        return Double.isFinite(value) ? Math.max(0, Math.min(maximum, value)) : 0;
    }

    public static double insight(LivingEntity entity) {
        return value(entity, ModAttributes.INSIGHT.get(), Float.MAX_VALUE);
    }

    public static double criticalChance(LivingEntity entity) {
        return value(entity, ModAttributes.CRITICAL_CHANCE.get(), 1);
    }

    public static double overlord(LivingEntity entity) {
        return value(entity, ModAttributes.OVERLORD.get(), 1);
    }

    private CombatAttributes() {}
}

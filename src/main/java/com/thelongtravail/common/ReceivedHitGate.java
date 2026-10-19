package com.thelongtravail.common;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public final class ReceivedHitGate {
    private final Map<LivingEntity, Long> last = new WeakHashMap<>();

    public boolean acquire(LivingEntity entity, DamageSource source, Collection<? extends String> excluded, int ticks) {
        String id = source.typeHolder().unwrapKey().map(k -> k.location().toString()).orElse("");
        if (excluded.contains(id)) return false;
        long now = entity.level().getGameTime();
        Long previous = last.get(entity);
        if (previous != null && now >= previous && now - previous < ticks) return false;
        last.put(entity, now);
        return true;
    }

    public void forget(LivingEntity entity) {
        last.remove(entity);
    }

    public void clear() {
        last.clear();
    }
}

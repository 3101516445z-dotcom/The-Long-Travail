package com.thelongtravail.common;

import net.minecraft.world.entity.LivingEntity;

public final class OverflowHealing {
    private static final ThreadLocal<Request> CURRENT = new ThreadLocal<>();

    public static final class Request {
        final LivingEntity target;
        boolean claimed;
        public float overflow;

        Request(LivingEntity target) {
            this.target = target;
        }
    }

    public static void heal(LivingEntity target, float amount) {
        if (target.level().isClientSide
                || !target.isAlive()
                || !Float.isFinite(amount)
                || amount <= 0) return;
        Request previous = CURRENT.get();
        Request request = new Request(target);
        CURRENT.set(request);
        try {
            target.heal(amount);
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
        if (target.isAlive() && request.overflow > 0) {
            target.setAbsorptionAmount(
                    (float)
                            Math.min(
                                    Float.MAX_VALUE,
                                    (double) target.getAbsorptionAmount() + request.overflow));
        }
    }

    public static Request claim(LivingEntity target) {
        Request request = CURRENT.get();
        if (request == null || request.target != target || request.claimed) return null;
        request.claimed = true;
        return request;
    }

    private OverflowHealing() {}
}

package com.thelongtravail.diagnostics;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.combat.DamagePolicy;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** 按进程限制条数的伤害链日志；关闭时不创建作用域或格式化日志。 */
public final class DamageTrace {
    public static final boolean ENABLED = Boolean.getBoolean("thelongtravail.damageTrace");
    private static final int LIMIT =
            Math.max(
                    1,
                    Math.min(10000, Integer.getInteger("thelongtravail.damageTrace.limit", 256)));
    private static final String TARGET =
            System.getProperty("thelongtravail.damageTrace.target", "");
    private static final AtomicLong IDS = new AtomicLong();
    private static final AtomicInteger LINES = new AtomicInteger();
    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();

    public static final class Scope implements AutoCloseable {
        private final Scope previous;
        private final long id, root;

        private Scope() {
            previous = CURRENT.get();
            id = IDS.incrementAndGet();
            root = previous == null ? id : previous.root;
            CURRENT.set(this);
        }

        @Override
        public void close() {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    public static Scope open(LivingEntity target, DamageSource source, float amount) {
        if (!ENABLED || LINES.get() >= LIMIT) return null;
        if (CURRENT.get() == null
                && !TARGET.isEmpty()
                && !TARGET.equals(target.getUUID().toString())) return null;
        Scope scope = new Scope();
        event(
                "enter target="
                        + target.getUUID()
                        + " type="
                        + source.typeHolder()
                                .unwrapKey()
                                .map(key -> key.location().toString())
                                .orElse("unregistered")
                        + " actor="
                        + (source.getEntity() == null ? "none" : source.getEntity().getUUID())
                        + " amount="
                        + amount);
        return scope;
    }

    public static void event(String message) {
        if (!ENABLED) return;
        Scope scope = CURRENT.get();
        if (scope == null || LINES.get() >= LIMIT || LINES.getAndIncrement() >= LIMIT) return;
        TheLongTravail.LOGGER.info(
                "TLT_DAMAGE root={} id={} parent={} {}",
                scope.root,
                scope.id,
                scope.previous == null ? 0 : scope.previous.id,
                message);
    }

    public static void policy(DamagePolicy policy) {
        if (ENABLED) event("policy=" + policy);
    }

    public static void confirmed(double total, double health) {
        if (ENABLED) event("confirmed total=" + total + " health=" + health);
    }

    private DamageTrace() {}
}

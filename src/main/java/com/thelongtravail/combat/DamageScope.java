package com.thelongtravail.combat;

import com.thelongtravail.diagnostics.DamageTrace;
import com.thelongtravail.farreach.SunChaser;
import com.thelongtravail.helper.CombatContext;
import com.thelongtravail.underworld.Kagotsurube;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

/** 调用链覆盖 hurt 及其收尾；同一来源逐日在派生链内最多追加一次。 */
public final class DamageScope {
    private static final ThreadLocal<Set<UUID>> SUN_CHAIN = new ThreadLocal<>();

    public static boolean claimSun(UUID attacker) {
        var chain = SUN_CHAIN.get();
        boolean claimed = chain != null && chain.add(attacker);
        if (DamageTrace.ENABLED)
            DamageTrace.event("sunClaim actor=" + attacker + " accepted=" + claimed);
        return claimed;
    }

    public static boolean hurt(
            LivingEntity target,
            DamageSource source,
            float amount,
            BiFunction<DamageSource, Float, Boolean> original) {
        try (var trace = DamageTrace.open(target, source, amount)) {
            try {
                boolean result = settle(target, source, amount, original);
                if (DamageTrace.ENABLED) DamageTrace.event("returned=" + result);
                return result;
            } catch (RuntimeException | Error failure) {
                DamageTrace.event("failed");
                throw failure;
            }
        }
    }

    private static boolean settle(
            LivingEntity target,
            DamageSource source,
            float amount,
            BiFunction<DamageSource, Float, Boolean> original) {
        if (!CombatContext.acceptsInput(target, source, amount)) return false;
        DamageSource effective = Kagotsurube.convert(target, source);
        if (SunChaser.immune(target, effective)) return false;
        boolean root = SUN_CHAIN.get() == null;
        if (root) SUN_CHAIN.set(new HashSet<>());
        try {
            SunChaser.clearCooldown(target);
            CombatContext context = CombatContext.open(target, source, effective);
            boolean result;
            try (context) {
                result = original.apply(effective, amount);
            }
            context.finish();
            return result;
        } finally {
            SunChaser.clearCooldown(target);
            if (root) SUN_CHAIN.remove();
        }
    }

    private DamageScope() {}
}

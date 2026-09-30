package com.thelongtravail.helper;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.event.TravailEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** One server-side hurt invocation. Nested hits own independent frames, including same-source hits. */
public final class CombatContext implements AutoCloseable {
    private static final ThreadLocal<CombatContext> CURRENT = new ThreadLocal<>();
    private final CombatContext previous;
    private final LivingEntity target;
    private final DamageSource source;
    private final ServerPlayer attacker;
    private final boolean penetrating;
    private final TravailConfig.FluidProtection protection;
    private final TravailConfig.ReceivedTrigger receivedTrigger;
    private boolean externallyCancelled;
    private boolean immunityEvaluated;
    private boolean attackerOutputBlocked;
    private boolean victimInternallyImmune;
    private boolean feedbackHandled;
    private boolean committed;
    private boolean hurtEvaluated;
    private float protectedDamage;

    private CombatContext(LivingEntity target, DamageSource source) {
        this.previous = CURRENT.get();
        this.target = target;
        this.source = source;
        this.attacker = source.getEntity() instanceof ServerPlayer player ? player : null;
        this.protection = source.is(TrueDamage.FLUID) && TravailConfig.FLUID_EROSION_UNAVOIDABLE.get()
                ? TravailConfig.FLUID_PROTECTION.get() : TravailConfig.FluidProtection.STANDARD;
        this.penetrating = protection != TravailConfig.FluidProtection.STANDARD;
        this.receivedTrigger = TravailConfig.RECEIVED_TRIGGER.get();
        CURRENT.set(this);
    }

    public static CombatContext open(LivingEntity target, DamageSource source) {
        return new CombatContext(target, source);
    }

    public static CombatContext current(LivingEntity target, DamageSource source) {
        CombatContext context = CURRENT.get();
        return context != null && context.target == target && context.source == source ? context : null;
    }

    public static void recordHealthLoss(LivingEntity target, float before, float after) {
        CombatContext context = CURRENT.get();
        if (context != null && context.target == target && after < before) context.committed = true;
    }

    /** Called once after a normal return, after the frame has been removed by try-with-resources. */
    public void finish() {
        if (committed && !victimInternallyImmune && receivedTrigger == TravailConfig.ReceivedTrigger.HEALTH_LOSS
                && target instanceof ServerPlayer player) ReceivedMalice.trigger(player, source);
        if (committed && attacker != null && attacker != target) TravailEvents.processSuccessfulAttack(attacker);
    }

    @Override public void close() {
        if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
    }

    /** Internal immunity is distinct from an attacker's inability to deal damage. */
    public boolean prepareAttack(float amount) {
        if (!immunityEvaluated) {
            immunityEvaluated = true;
            if (target instanceof ServerPlayer player) {
                ItemStack diary = TravailCurios.stack(player);
                if (!diary.isEmpty() && LongTravailData.hasWitness(diary, TravailAspect.ABYSS)) {
                    victimInternallyImmune = abyssEnvironmentDamage(source)
                            || (amount > 0 && !source.is(DamageTypes.GENERIC_KILL)
                            && (player.isInFluidType() || player.isInWaterOrBubble() || player.isInLava())
                            && player.getRandom().nextDouble() < TravailConfig.ABYSS_CANCEL_CHANCE.get());
                }
            }
            if (attacker != null) {
                ItemStack diary = TravailCurios.stack(attacker);
                attackerOutputBlocked = !diary.isEmpty() && !LongTravailData.hasWitness(diary, TravailAspect.BOUNDLESS)
                        && TravailEvents.isFlyingAttack(attacker, source);
            }
        }
        if (!feedbackHandled) {
            feedbackHandled = true;
            if (!victimInternallyImmune && receivedTrigger == TravailConfig.ReceivedTrigger.ATTACK_ATTEMPT
                    && target instanceof ServerPlayer player) ReceivedMalice.trigger(player, source);
        }
        return !blocked();
    }

    /**
     * The damage set the Abyss aspect targets: the malice amplifies it, the witness negates it.
     * One shared predicate keeps the two sides from drifting apart.
     */
    private static boolean abyssEnvironmentDamage(DamageSource source) {
        return source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING)
                || source.is(DamageTypes.DROWN) || source.is(DamageTypes.FREEZE);
    }

    public boolean blocked() { return attackerOutputBlocked || victimInternallyImmune; }
    public boolean penetrating() { return penetrating; }
    public boolean overridesCancellation() { return protection == TravailConfig.FluidProtection.ENFORCED; }
    public static void observeCancellation(net.minecraftforge.eventbus.api.Event event, boolean cancelled) {
        CombatContext context = event instanceof net.minecraftforge.event.entity.living.LivingHurtEvent hurt
                ? current(hurt.getEntity(), hurt.getSource())
                : event instanceof net.minecraftforge.event.entity.living.LivingDamageEvent damage
                ? current(damage.getEntity(), damage.getSource()) : null;
        if (context != null && cancelled && context.protection == TravailConfig.FluidProtection.RESPECT_CANCELLATION)
            context.externallyCancelled = true;
    }

    /** Called at the first Hurt boundary; uses that stage's input (e.g. vanilla difficulty adjustment). */
    public float prepareHurt(float amount) {
        if (penetrating && !hurtEvaluated) {
            hurtEvaluated = true;
            protectedDamage = internalAmount(amount);
        }
        return penetrating ? protectedDamage : amount;
    }

    public float finishHurt(float external) {
        if (blocked() || externallyCancelled) return 0;
        return penetrating ? protect(external) : internalAmount(external);
    }

    public float finishDamage(float external) {
        return blocked() || externallyCancelled ? 0 : penetrating && hurtEvaluated ? protect(external) : external;
    }

    private float protect(float external) {
        // A zero approved by our own rules must remain zero, even if another mod increases it.
        if (protectedDamage <= 0) return 0;
        return Float.isFinite(external) ? Math.max(external, protectedDamage) : protectedDamage;
    }

    private float internalAmount(float amount) {
        if (!(amount > 0)) return 0;
        if (target instanceof ServerPlayer player) {
            ItemStack diary = TravailCurios.stack(player);
            if (!diary.isEmpty()) {
                if (!LongTravailData.hasWitness(diary, TravailAspect.FLOURISHING)
                        && player.getHealth() > player.getMaxHealth() * TravailConfig.FLOURISHING_HEALTH_THRESHOLD.get()) {
                    amount = multiply(amount, TravailConfig.FLOURISHING_DAMAGE_MULTIPLIER.get());
                }
                if (!LongTravailData.hasWitness(diary, TravailAspect.ABYSS) && abyssEnvironmentDamage(source)) {
                    amount = multiply(amount, TravailConfig.ABYSS_ENVIRONMENT_MULTIPLIER.get());
                }
            }
        }
        if (attacker != null) {
            ItemStack diary = TravailCurios.stack(attacker);
            if (!diary.isEmpty() && !LongTravailData.hasWitness(diary, TravailAspect.UNDERWORLD)) {
                amount = multiply(amount, 1.0 - TravailConfig.UNDERWORLD_DAMAGE_REDUCTION.get());
            }
        }
        return amount;
    }

    private static float multiply(float amount, double multiplier) {
        if (multiplier <= 0) return 0;
        double result = amount * multiplier;
        return (float) Math.max(0, Math.min(Float.MAX_VALUE, result));
    }
}

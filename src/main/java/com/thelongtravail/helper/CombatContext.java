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

// 每次服务端 hurt 调用独占上下文，嵌套伤害即使来源相同也使用独立上下文。
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
    private boolean ekiImmune, skipReceivedPenalty;
    private double ekiDamageBonus;
    private double icarusDamageBonus;
    private double swordLanternBonus;
    private final double witherBonus;
    private boolean icarusImmune;
    private final boolean floralTransfer;
    private final boolean floralPoison;
    private final boolean roseDamage;
    private final double floralBonus;
    private boolean feedbackHandled;
    private boolean committed;
    private boolean hurtEvaluated;
    private float protectedDamage;
    private final boolean execution;
    private boolean damageConfirmed;

    private CombatContext(LivingEntity target, DamageSource source) {
        this.previous = CURRENT.get();
        this.target = target;
        this.source = source;
        this.execution = com.thelongtravail.valley.AzraelExecution.matches(target, source);
        this.witherBonus = execution ? 0 : com.thelongtravail.underworld.UnderworldItems.bonus(target, source);
        Double transferredBonus = com.thelongtravail.flourishing.FloralCombat.claimTransfer(target, source);
        this.floralTransfer = transferredBonus != null;
        this.floralPoison = com.thelongtravail.flourishing.FloralEvents.claimPoison(target, source);
        this.roseDamage = com.thelongtravail.flourishing.FloralCombat.rose(source);
        this.floralBonus = execution ? 0 : floralTransfer ? transferredBonus : com.thelongtravail.flourishing.FloralCombat.bonus(target, source);
        this.attacker = source.getEntity() instanceof ServerPlayer player ? player : null;
        this.protection = roseDamage ? TravailConfig.FluidProtection.ENFORCED : source.is(TrueDamage.FLUID) && TravailConfig.FLUID_EROSION_UNAVOIDABLE.get()
                ? TravailConfig.FLUID_PROTECTION.get() : TravailConfig.FluidProtection.STANDARD;
        this.penetrating = protection != TravailConfig.FluidProtection.STANDARD;
        this.receivedTrigger = TravailConfig.RECEIVED_TRIGGER.get();
        if (!execution && !floralTransfer && !roseDamage && attacker != null && attacker != target && com.thelongtravail.abyss.RainState.ekiMalice(attacker))
            ekiDamageBonus = com.thelongtravail.config.RainConfig.DAMAGE.get();
        if (!execution && !floralTransfer && !roseDamage && attacker != null && attacker != target)
            icarusDamageBonus = com.thelongtravail.farreach.FarReachCombat.bonus(attacker);
        if (!execution && !floralTransfer && !roseDamage && attacker != null && attacker != target)
            swordLanternBonus = com.thelongtravail.valley.SwordLanternState.bonus(attacker, target);
        CURRENT.set(this);
    }

    public static CombatContext open(LivingEntity target, DamageSource source) {
        return new CombatContext(target, source);
    }

    public static CombatContext current(LivingEntity target, DamageSource source) {
        CombatContext context = CURRENT.get();
        return context != null && context.target == target && context.source == source ? context : null;
    }

    public boolean isFloralPoison() { return floralPoison; }

    public boolean isFloralTransfer() { return floralTransfer; }

    public static void recordHealthLoss(LivingEntity target, float before, float after) {
        CombatContext context = CURRENT.get();
        if (context != null && context.target == target && after < before) {
            context.committed = true;
            if (Float.isFinite(before) && Float.isFinite(after)) context.confirmDamage();
        }
    }

    public static void recordAbsorptionLoss(LivingEntity target, float before, float after) {
        CombatContext context = CURRENT.get();
        if (context != null && context.target == target && Float.isFinite(before) && Float.isFinite(after) && after < before)
            context.confirmDamage();
    }

    // 在首次实际扣除生命值或伤害吸收量时记录顺序，每次 hurt 调用最多判定一次，处决仍延后执行。
    private void confirmDamage() {
        if (damageConfirmed || execution || com.thelongtravail.valley.AzraelExecution.active()) return;
        damageConfirmed = true;
        CombatConsequences.confirmed(target,source,attacker,floralTransfer,roseDamage);
    }

    // 仅在正常返回后调用一次，此时 try-with-resources 已移除当前上下文。
    public void finish() {
        if (execution) return;
        if (committed && !icarusImmune && !victimInternallyImmune && !skipReceivedPenalty && receivedTrigger == TravailConfig.ReceivedTrigger.HEALTH_LOSS
                && target instanceof ServerPlayer player) ReceivedMalice.trigger(player, source);
        if (committed && !floralTransfer && !roseDamage && attacker != null && attacker != target) TravailEvents.processSuccessfulAttack(attacker);
        if (committed) com.thelongtravail.flourishing.FloralEvents.successful(target, source, floralTransfer);
    }

    @Override public void close() {
        if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
    }

    // 自身免疫与攻击者无法造成伤害是两种不同情况。
    public boolean prepareAttack(float amount) {
        if (execution) return true;
        if (roseDamage) return true;
        if (!immunityEvaluated) {
            immunityEvaluated = true;
            if (target instanceof ServerPlayer player) {
                icarusImmune = com.thelongtravail.farreach.FarReachCombat.immune(player, source);
                ItemStack diary = TravailCurios.stack(player);
                boolean ekiActive = com.thelongtravail.abyss.RainState.ekiState(player, diary)
                        != com.thelongtravail.abyss.RainState.EkiState.INACTIVE;
                boolean witness = !diary.isEmpty() && LongTravailData.hasWitness(diary, TravailAspect.ABYSS);
                if (ekiActive && !witness) skipReceivedPenalty = com.thelongtravail.config.RainConfig.MALICE_SKIP.get();
                boolean ekiEligible = ekiActive && witness && amount > 0 && !source.is(DamageTypes.GENERIC_KILL)
                        && !com.thelongtravail.config.RainConfig.IMMUNITY_EXCLUSIONS.get().contains(
                            source.typeHolder().unwrapKey().map(k -> k.location().toString()).orElse(""));
                if (ekiEligible) {
                    ekiImmune = player.getRandom().nextDouble() < com.thelongtravail.config.RainConfig.IMMUNITY.get();
                    if (ekiImmune) skipReceivedPenalty = com.thelongtravail.config.RainConfig.IMMUNE_SKIP.get();
                }
                if (!ekiImmune && (!ekiEligible || com.thelongtravail.config.RainConfig.FALLBACK.get())
                        && !diary.isEmpty() && LongTravailData.hasWitness(diary, TravailAspect.ABYSS)) {
                    victimInternallyImmune = CombatRules.abyssEnvironmentDamage(source)
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
            if (!icarusImmune && !victimInternallyImmune && !skipReceivedPenalty && receivedTrigger == TravailConfig.ReceivedTrigger.ATTACK_ATTEMPT
                    && target instanceof ServerPlayer player) ReceivedMalice.trigger(player, source);
        }
        return !blocked();
    }

    public boolean blocked() { return attackerOutputBlocked || victimInternallyImmune || ekiImmune || icarusImmune; }
    // 分担使用同一次攻击的增伤快照，两条伤害分支各应用一次。
    public double transferableBonus() { return ekiDamageBonus + floralBonus + icarusDamageBonus + swordLanternBonus; }
    public double outgoingBonus() { return transferableBonus() + witherBonus; }
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

    // 在首个 Hurt 边界调用，使用该阶段的输入值，包括原版难度调整。
    public float prepareHurt(float amount) {
        if (penetrating && !hurtEvaluated) {
            hurtEvaluated = true;
            protectedDamage = internalAmount(amount);
        }
        return penetrating ? protectedDamage : amount;
    }

    public float finishHurt(float external) {
        if (execution) return external;
        if (roseDamage) return protectedDamage;
        if (blocked() || externallyCancelled) return 0;
        return penetrating ? protect(external) : internalAmount(external);
    }

    public float finishDamage(float external) {
        if (execution) return external;
        if (roseDamage) return multiply(protectedDamage, 1 + witherBonus);
        float amount = blocked() || externallyCancelled ? 0 : penetrating && hurtEvaluated ? protect(external) : external;
        if (!penetrating) amount = com.thelongtravail.flourishing.FloralCombat.reduce(target, source, amount);
        return amount > 0 ? multiply(amount, 1 + outgoingBonus()) : amount;
    }

    private float protect(float external) {
        // 本模组规则判定为零的伤害必须保持为零，即使其他模组随后增大伤害。
        if (protectedDamage <= 0) return 0;
        return Float.isFinite(external) ? Math.max(external, protectedDamage) : protectedDamage;
    }

    private float internalAmount(float amount) {
        return CombatRules.internalAmount(target,attacker,roseDamage,source,amount);
    }

    private static float multiply(float amount,double multiplier){return CombatRules.multiply(amount,multiplier);}
}

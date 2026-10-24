package com.thelongtravail.helper;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.ModeEffectRules;
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
    private double maidBonus;
    private boolean maidImmune;
    private double icarusDamageBonus;
    private double lafiteDamageBonus;
    private double swordLanternBonus;
    private final double witherBonus;
    private boolean icarusImmune;
    private final boolean floralPoison;
    private final boolean roseDamage;
    private final double floralBonus;
    private boolean feedbackHandled;
    private boolean phosphophylliteReceived;
    private boolean committed;
    private boolean hurtEvaluated;
    private float protectedDamage;
    private final boolean execution;
    private boolean damageConfirmed;
    private boolean provocationRecorded;
    private final boolean quixoteStrike;
    private final com.thelongtravail.farreach.DonQuixoteCombat.Snapshot quixote;
    private double recordedDamage;
    private double relicHealthLoss;
    private final double relicBonus;
    private final ItemStack relicHealing;
    private final double growthBonus;
    private boolean outputApplied;
    private final com.thelongtravail.combat.AttributeDamage attributeDamage;
    private final boolean lorentzVoid;
    private final double lorentzBonus;
    private final boolean insightRetaliation;
    private boolean attributeOutputApplied;
    private final int previousInvulnerability;
    private final float previousLastHurt;
    private boolean nestedDamageConfirmed;
    private final com.thelongtravail.combat.DamageReplay.Frame replay;

    public boolean isReplay() {
        return replay != null;
    }

    private CombatContext(LivingEntity target, DamageSource source) {
        this.lorentzVoid = com.thelongtravail.boundless.LorentzFantasy.secondary(source);
        this.lorentzBonus =
                lorentzVoid ? 0 : com.thelongtravail.boundless.LorentzFantasy.bonus(target, source);
        this.previous = CURRENT.get();
        this.target = target;
        this.source = source;
        this.previousInvulnerability = target.invulnerableTime;
        this.previousLastHurt =
                ((com.thelongtravail.mixin.DamageReplayStateAccessor) target).travail$lastHurt();
        this.replay = com.thelongtravail.combat.DamageReplay.claim(target, source);
        this.execution = com.thelongtravail.valley.AzraelExecution.matches(target, source);
        this.quixoteStrike = com.thelongtravail.farreach.DonQuixoteCombat.secondary(source);
        this.insightRetaliation = com.thelongtravail.combat.InsightRetaliation.isInsight(source);
        this.attributeDamage =
                new com.thelongtravail.combat.AttributeDamage(
                        target,
                        source,
                        execution
                                || source.is(com.thelongtravail.valley.AzraelExecution.TYPE)
                                || source.is(com.thelongtravail.valley.AzraelExecution.SELF),
                        replay != null || quixoteStrike);
        this.quixote =
                lorentzVoid || execution || replay != null || quixoteStrike || insightRetaliation
                        ? null
                        : com.thelongtravail.farreach.DonQuixoteCombat.snapshot(target, source);
        this.witherBonus =
                execution
                        ? 0
                        : com.thelongtravail.underworld.UnderworldItems.bonus(target, source)
                                + com.thelongtravail.compat.maid.MaidAccessories.witherBonus(
                                        target, source);
        this.floralPoison = com.thelongtravail.flourishing.FloralEvents.claimPoison(target, source);
        this.roseDamage = com.thelongtravail.flourishing.FloralCombat.rose(source);
        this.floralBonus =
                execution ? 0 : com.thelongtravail.flourishing.FloralCombat.bonus(target, source);
        this.growthBonus =
                !execution
                                && !roseDamage
                                && source.getEntity() instanceof LivingEntity living
                                && living != target
                        ? com.thelongtravail.flourishing.Mistletoe.bonus(living)
                                + com.thelongtravail.flourishing.EverlastingFlower.bonus(living)
                        : 0;
        this.relicBonus =
                execution || replay != null || quixoteStrike || insightRetaliation || roseDamage
                        ? 0
                        : com.thelongtravail.underworld.DemonKingCombat.bonus(target, source);
        this.relicHealing =
                execution || replay != null || quixoteStrike || insightRetaliation || roseDamage
                        ? ItemStack.EMPTY
                        : com.thelongtravail.underworld.DemonKingCombat.healingStack(
                                target, source);
        this.attacker = source.getEntity() instanceof ServerPlayer player ? player : null;
        this.protection =
                roseDamage
                        ? TravailConfig.FluidProtection.ENFORCED
                        : source.is(TrueDamage.FLUID)
                                        && TravailConfig.FLUID_EROSION_UNAVOIDABLE.get()
                                ? TravailConfig.FLUID_PROTECTION.get()
                                : TravailConfig.FluidProtection.STANDARD;
        this.penetrating = protection != TravailConfig.FluidProtection.STANDARD;
        this.receivedTrigger = TravailConfig.RECEIVED_TRIGGER.get();
        if (!execution
                && !roseDamage
                && attacker != null
                && attacker != target
                && com.thelongtravail.abyss.RainState.ekiMalice(attacker))
            ekiDamageBonus = com.thelongtravail.config.RainConfig.DAMAGE.get();
        if (!execution && !roseDamage && attacker != null && attacker != target)
            icarusDamageBonus = com.thelongtravail.farreach.FarReachCombat.bonus(attacker);
        if (!execution && !roseDamage && attacker != null && attacker != target)
            lafiteDamageBonus = com.thelongtravail.abyss.MidnightLafite.bonus(attacker);
        if (!execution && !roseDamage && attacker != null && attacker != target)
            swordLanternBonus = com.thelongtravail.valley.SwordLanternState.bonus(attacker, target);
        if (!execution && !roseDamage)
            maidBonus = com.thelongtravail.compat.maid.MaidAccessories.bonus(target, source);
        CURRENT.set(this);
    }

    // 在原版修改 lastHurt、受伤冷却或分发事件前完成校验；仅允许实际处决实例使用非有限数值。
    public static boolean acceptsInput(LivingEntity target, DamageSource source, float amount) {
        return Float.isFinite(amount) && amount > 0
                || com.thelongtravail.valley.AzraelExecution.matches(target, source);
    }

    public static CombatContext open(LivingEntity target, DamageSource source) {
        return new CombatContext(target, source);
    }

    public static CombatContext current(LivingEntity target, DamageSource source) {
        CombatContext context = CURRENT.get();
        return context != null && context.target == target && context.source == source
                ? context
                : null;
    }

    public boolean isFloralPoison() {
        return floralPoison;
    }

    public static void recordHealthLoss(LivingEntity target, float before, float after) {
        CombatContext context = CURRENT.get();
        if (context != null && context.target == target && after < before) {
            context.committed = true;
            if (Float.isFinite(before) && Float.isFinite(after))
                context.relicHealthLoss += Math.max(0, Math.max(0, before) - Math.max(0, after));
            if (Float.isFinite(before) && Float.isFinite(after)) {
                context.recordedDamage += before - after;
                context.recordProvocation();
            }
            if (Float.isFinite(before) && Float.isFinite(after)) context.confirmDamage();
        }
    }

    public static void recordAbsorptionLoss(LivingEntity target, float before, float after) {
        CombatContext context = CURRENT.get();
        if (context != null
                && context.target == target
                && Float.isFinite(before)
                && Float.isFinite(after)
                && after < before) {
            context.recordedDamage += before - after;
            context.recordProvocation();
            context.confirmDamage();
        }
    }

    public static void recordDummyDamage(LivingEntity target, DamageSource source, float amount) {
        if (target.level().isClientSide || !Float.isFinite(amount) || amount <= 0) return;
        CombatContext context = current(target, source);
        if (context != null) {
            context.recordedDamage += amount;
            context.confirmDamage();
        }
    }

    private void recordProvocation() {
        if (!provocationRecorded) {
            provocationRecorded = true;
            com.thelongtravail.farreach.BabelProtection.damaged(target, source);
        }
    }

    private void confirmDamage() {
        for (CombatContext parent = previous; parent != null; parent = parent.previous)
            if (parent.target == target) parent.nestedDamageConfirmed = true;
        if (damageConfirmed || execution || com.thelongtravail.valley.AzraelExecution.active())
            return;
        damageConfirmed = true;
        if (quixoteStrike) return;
        if (replay != null) {
            replay.confirmed = true;
            return;
        }
        if (insightRetaliation) return;
        if (lorentzVoid) return;
        CombatConsequences.confirmed(target, source, attacker, roseDamage);
        com.thelongtravail.compat.maid.MaidAccessories.confirmed(target, source, roseDamage);
    }

    public void finish() {
        if (attributeDamage.zeroed() && !damageConfirmed && !nestedDamageConfirmed) {
            target.invulnerableTime = previousInvulnerability;
            ((com.thelongtravail.mixin.DamageReplayStateAccessor) target)
                    .travail$lastHurt(previousLastHurt);
        }
        if (execution || replay != null || quixoteStrike) return;
        if (insightRetaliation) return;
        if (lorentzVoid) {
            attributeDamage.finish();
            return;
        }
        if (damageConfirmed)
            com.thelongtravail.farreach.DonQuixoteCombat.finish(
                    target, source, quixote, recordedDamage);
        if (accessoryAttack) {
            com.thelongtravail.flourishing.FloralEvents.received(target, source);
            com.thelongtravail.compat.maid.MaidAccessories.retaliate(target, source);
        }
        if (committed
                && !maidImmune
                && !icarusImmune
                && !victimInternallyImmune
                && !skipReceivedPenalty
                && receivedTrigger == TravailConfig.ReceivedTrigger.HEALTH_LOSS)
            phosphophylliteReceived |= ReceivedMalice.trigger(target, source);
        if (phosphophylliteReceived) com.thelongtravail.valley.Phosphophyllite.received(target);
        // 造成伤害的统计包含生命值和伤害吸收量的损失；受击惩罚的 HEALTH_LOSS 仍只读取 committed。
        if (damageConfirmed && !roseDamage && attacker != null && attacker != target)
            TravailEvents.processSuccessfulAttack(attacker);
        if (damageConfirmed) {
            com.thelongtravail.flourishing.FloralEvents.successful(target, source);
            com.thelongtravail.compat.maid.MaidAccessories.successful(target, source);
        }
        if (damageConfirmed && !roseDamage && !source.is(DamageTypes.THORNS))
            com.thelongtravail.flourishing.EverlastingFlower.successful(
                    target, source, (float) Math.min(Float.MAX_VALUE, recordedDamage), false);
        com.thelongtravail.underworld.DemonKingCombat.heal(
                target, source, relicHealing, relicHealthLoss);
        attributeDamage.finish();
    }

    @Override
    public void close() {
        if (previous == null) CURRENT.remove();
        else CURRENT.set(previous);
    }

    private boolean accessoryAttack;

    public boolean prepareAttack(float amount) {
        if (quixoteStrike) return true;
        if (replay == null
                && !accessoryAttack
                && !execution
                && !roseDamage
                && !insightRetaliation
                && !lorentzVoid
                && !com.thelongtravail.valley.AzraelExecution.active()
                && amount > 0
                && Float.isFinite(amount)
                && target.isAlive()
                && !target.isSpectator()) {
            accessoryAttack = true;
            CombatConsequences.received(target, source);
        }
        if (execution) return true;
        if (roseDamage) return true;
        if (!immunityEvaluated) {
            immunityEvaluated = true;
            maidImmune =
                    com.thelongtravail.compat.maid.MaidAccessories.immune(target, source, amount);
            if (target instanceof ServerPlayer player) {
                icarusImmune = com.thelongtravail.farreach.FarReachCombat.immune(player, source);
                ItemStack diary = TravailCurios.stack(player);
                boolean ekiActive =
                        com.thelongtravail.abyss.RainState.ekiState(player, diary)
                                != com.thelongtravail.abyss.RainState.EkiState.INACTIVE;
                boolean witness =
                        !diary.isEmpty() && LongTravailData.hasWitness(diary, TravailAspect.ABYSS);
                if (ekiActive && !witness)
                    skipReceivedPenalty = com.thelongtravail.config.RainConfig.MALICE_SKIP.get();
                boolean ekiEligible =
                        ekiActive
                                && witness
                                && amount > 0
                                && !source.is(DamageTypes.GENERIC_KILL)
                                && !com.thelongtravail.config.RainConfig.IMMUNITY_EXCLUSIONS
                                        .get()
                                        .contains(
                                                source.typeHolder()
                                                        .unwrapKey()
                                                        .map(k -> k.location().toString())
                                                        .orElse(""));
                if (ekiEligible) {
                    ekiImmune =
                            player.getRandom().nextDouble()
                                    < com.thelongtravail.config.RainConfig.IMMUNITY.get();
                    if (ekiImmune)
                        skipReceivedPenalty =
                                com.thelongtravail.config.RainConfig.IMMUNE_SKIP.get();
                }
                if (!ekiImmune
                        && (!ekiEligible || com.thelongtravail.config.RainConfig.FALLBACK.get())
                        && !diary.isEmpty()
                        && LongTravailData.hasWitness(diary, TravailAspect.ABYSS)) {
                    victimInternallyImmune =
                            CombatRules.abyssEnvironmentDamage(source)
                                    || (amount > 0
                                            && !source.is(DamageTypes.GENERIC_KILL)
                                            && (player.isInFluidType()
                                                    || player.isInWaterOrBubble()
                                                    || player.isInLava())
                                            && player.getRandom().nextDouble()
                                                    < TravailConfig.ABYSS_CANCEL_CHANCE.get());
                }
            }
            if (attacker != null) {
                ItemStack diary = TravailCurios.stack(attacker);
                attackerOutputBlocked =
                        ModeEffectRules.diaryApplies(attacker)
                                && !diary.isEmpty()
                                && !LongTravailData.hasWitness(diary, TravailAspect.BOUNDLESS)
                                && TravailEvents.isFlyingAttack(attacker, source);
            }
        }
        if (replay == null && !feedbackHandled) {
            feedbackHandled = true;
            if (!lorentzVoid
                    && !maidImmune
                    && !icarusImmune
                    && !victimInternallyImmune
                    && !skipReceivedPenalty
                    && receivedTrigger == TravailConfig.ReceivedTrigger.ATTACK_ATTEMPT)
                phosphophylliteReceived |= ReceivedMalice.trigger(target, source);
        }
        return !blocked();
    }

    public boolean blocked() {
        return maidImmune
                || attackerOutputBlocked
                || victimInternallyImmune
                || ekiImmune
                || icarusImmune;
    }

    public double outgoingBonus() {
        if (quixoteStrike || insightRetaliation || lorentzVoid) return 0;
        return (quixote == null ? 0 : quixote.bonus())
                + ekiDamageBonus
                + floralBonus
                + icarusDamageBonus
                + lafiteDamageBonus
                + swordLanternBonus
                + maidBonus
                + growthBonus
                + witherBonus
                + relicBonus
                + lorentzBonus;
    }

    public boolean penetrating() {
        return penetrating;
    }

    public boolean overridesCancellation() {
        return protection == TravailConfig.FluidProtection.ENFORCED;
    }

    public static void observeCancellation(
            net.minecraftforge.eventbus.api.Event event, boolean cancelled) {
        CombatContext context =
                event instanceof net.minecraftforge.event.entity.living.LivingHurtEvent hurt
                        ? current(hurt.getEntity(), hurt.getSource())
                        : event
                                        instanceof
                                        net.minecraftforge.event.entity.living.LivingDamageEvent
                                                        damage
                                ? current(damage.getEntity(), damage.getSource())
                                : null;
        if (context != null && cancelled) context.attributeDamage.cancelled();
        if (context != null
                && cancelled
                && context.protection == TravailConfig.FluidProtection.RESPECT_CANCELLATION)
            context.externallyCancelled = true;
    }

    public float prepareHurt(float amount) {
        if (replay != null) return replay.amount;
        if (penetrating && !hurtEvaluated) {
            hurtEvaluated = true;
            protectedDamage = internalAmount(amount);
        }
        return penetrating ? protectedDamage : amount;
    }

    public float finishHurt(float external) {
        if (execution) return external;
        external = safeDamage(external);
        if (blocked() || externallyCancelled) return 0;
        if (quixoteStrike || replay != null) {
            attributeDamage.begin();
            return external;
        }
        float amount =
                roseDamage
                        ? protectedDamage
                        : penetrating ? protect(external) : internalAmount(external);
        if (amount > 0 && attributeDamage.begin() && !attributeOutputApplied) {
            attributeOutputApplied = true;
            amount = attributeDamage.critical(amount);
            if (penetrating) protectedDamage = amount;
        }
        return amount;
    }

    public float finishDamage(float external) {
        if (!execution) external = safeDamage(external);
        if (external <= 0 && !attributeDamage.zeroed() && !damageConfirmed)
            attributeDamage.cancelled();
        if (attributeDamage.active() && attributeDamage.defended()) {
            if (attributeDamage.zeroed() || blocked() || externallyCancelled) return 0;
            if (roseDamage) return protectedDamage;
            return penetrating && !attributeDamage.settled() ? protect(external) : external;
        }
        if (quixoteStrike) return external;
        if (replay != null) return blocked() || externallyCancelled ? 0 : external;
        if (execution) return external;
        if (roseDamage) return multiply(protectedDamage, 1 + witherBonus);
        float amount =
                blocked() || externallyCancelled
                        ? 0
                        : penetrating && hurtEvaluated ? protect(external) : external;
        return amount > 0 && !outputApplied ? multiply(amount, 1 + outgoingBonus()) : amount;
    }

    public float beforeAbsorption(float amount) {
        if (execution) return amount;
        if (!roseDamage && replay == null && !penetrating && !quixoteStrike) {
            amount = com.thelongtravail.underworld.DemonKingCombat.reduce(target, source, amount);
            amount = com.thelongtravail.farreach.DonQuixoteState.reduce(target, source, amount);
            if (!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))
                amount =
                        multiply(
                                amount,
                                (1 - com.thelongtravail.flourishing.Mistletoe.reduction(target))
                                        * (1
                                                - com.thelongtravail.flourishing.EverlastingFlower
                                                        .reduction(target)));
            if (!outputApplied && (growthBonus != 0 || quixote != null && quixote.bonus() != 0)) {
                amount = multiply(amount, 1 + outgoingBonus());
                outputApplied = true;
            }
        }
        if (replay == null && !quixoteStrike && !outputApplied) {
            amount = multiply(amount, roseDamage ? 1 + witherBonus : 1 + outgoingBonus());
            outputApplied = true;
        }
        if (!penetrating && replay == null && !quixoteStrike) {
            amount = com.thelongtravail.flourishing.FloralCombat.reduce(target, source, amount);
            amount =
                    com.thelongtravail.compat.maid.MaidAccessories.reduce(
                            target, source, amount, floralPoison);
        }
        if (!attributeDamage.begin()) return safeDamage(amount);
        if (penetrating && !attributeDamage.settled()) {
            amount = roseDamage ? amount : protect(amount);
        }
        amount = attributeDamage.defend(amount);
        // 强制伤害不得重新计入已被见切或霸主抵消的部分。
        if (penetrating && !attributeDamage.settled()) protectedDamage = amount;
        return amount;
    }

    private static float safeDamage(float amount) {
        return Float.isFinite(amount) && amount > 0 ? amount : 0;
    }

    private float protect(float external) {
        // 本模组规则判定为零的伤害必须保持为零，即使其他模组随后增大伤害。
        if (protectedDamage <= 0) return 0;
        return Float.isFinite(external) ? Math.max(external, protectedDamage) : protectedDamage;
    }

    private float internalAmount(float amount) {
        if (lorentzVoid) return amount;
        return CombatRules.internalAmount(
                target, insightRetaliation ? null : attacker, roseDamage, source, amount);
    }

    private static float multiply(float amount, double multiplier) {
        return CombatRules.multiply(amount, multiplier);
    }
}

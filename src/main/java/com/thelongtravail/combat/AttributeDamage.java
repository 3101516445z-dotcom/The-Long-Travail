package com.thelongtravail.combat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

public final class AttributeDamage {
    public static final TagKey<DamageType> BYPASSES =
            TagKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            "the_long_travail", "bypasses_combat_attributes"));
    private final LivingEntity target;
    private final DamageSource source;
    private final boolean excluded;
    private final boolean settled;
    private boolean started, active, defended, cancelled;
    private double insight, chance, retaliation;
    private float threshold;
    private float defendedAmount;

    public AttributeDamage(
            LivingEntity target, DamageSource source, boolean execution, boolean settled) {
        this.target = target;
        this.source = source;
        this.excluded =
                execution
                        || source.is(BYPASSES)
                        || source.is(com.thelongtravail.helper.TrueDamage.FLUID);
        this.settled = settled;
    }

    public boolean begin() {
        if (started) return active;
        started = true;
        if (excluded) return false;
        com.thelongtravail.underworld.UnderworldRelics.refresh(target);
        if (source.getEntity() instanceof LivingEntity actor)
            com.thelongtravail.boundless.LorentzFantasy.refresh(actor);
        if (source.getEntity() instanceof LivingEntity actor && actor != target)
            com.thelongtravail.underworld.UnderworldRelics.refresh(actor);
        double max = target.getMaxHealth();
        // 使用与原版伤害相同的 float 精度，确保伤害恰好等于小数阈值时仍能免疫。
        threshold =
                Double.isFinite(max) && max > 0
                        ? (float) (max * CombatAttributes.overlord(target))
                        : 0;
        if (!settled) {
            if (source.getEntity() != null && source.getEntity() != target)
                insight = CombatAttributes.insight(target);
            if (!com.thelongtravail.boundless.LorentzFantasy.secondary(source)
                    && source.getEntity() instanceof LivingEntity attacker
                    && attacker != target) chance = CombatAttributes.criticalChance(attacker);
        }
        active = threshold > 0 || insight > 0 || chance > 0;
        return active;
    }

    public boolean settled() {
        return settled;
    }

    public boolean active() {
        return active;
    }

    public boolean defended() {
        return defended;
    }

    public boolean zeroed() {
        return defended && defendedAmount <= 0;
    }

    public void cancelled() {
        cancelled = true;
    }

    public float critical(float amount) {
        if (!(amount > 0) || settled || chance <= 0) return amount;
        LivingEntity attacker = (LivingEntity) source.getEntity();
        return chance >= 1 || attacker.getRandom().nextDouble() < chance
                ? (float) Math.min(Float.MAX_VALUE, (double) amount * 2)
                : amount;
    }

    public float defend(float amount) {
        if (!begin()) return amount;
        if (defended) return defendedAmount;
        defended = true;
        if (!(amount > 0)) return defendedAmount = 0;
        if (!Float.isFinite(amount)) return defendedAmount = 0;
        if (insight > 0) {
            if (!InsightRetaliation.isRetaliation(source)) retaliation = insight;
            amount = (float) Math.max(0, amount - insight);
        }
        return defendedAmount = amount <= threshold ? 0 : amount;
    }

    public void finish() {
        if (!cancelled && retaliation > 0) {
            double amount = retaliation;
            retaliation = 0;
            InsightRetaliation.hit(target, source.getEntity(), amount);
        }
    }
}

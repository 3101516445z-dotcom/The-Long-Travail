package com.thelongtravail.underworld;

import com.thelongtravail.farreach.icarus.IcarusEnvironment;

import net.minecraft.tags.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;

public final class DemonKingCombat {
    public static double bonus(LivingEntity target, DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity actor)
                || actor == target
                || UnderworldRelics.equipped(actor, false).isEmpty()) return 0;
        return UnderworldRelics.phase(actor) == 1
                ? (target.isOnFire() ? UnderworldRelics.value("king.burningBonus") : 0)
                : actor.level().getBiome(actor.blockPosition()).is(BiomeTags.IS_NETHER)
                                || !IcarusEnvironment.openSky(actor)
                        ? UnderworldRelics.value("king.environmentBonus")
                        : 0;
    }

    public static double remainingMultiplier(LivingEntity target, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || UnderworldRelics.phase(target) != 1
                || UnderworldRelics.equipped(target, false).isEmpty()
                || !(source.getEntity() instanceof LivingEntity attacker)
                || attacker == target
                || attacker.getMobType() != MobType.UNDEAD) return 1;
        return com.thelongtravail.combat.DamageReduction.remaining(
                UnderworldRelics.value("king.undeadReduction"));
    }

    public static net.minecraft.world.item.ItemStack healingStack(
            LivingEntity target, DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity actor)
                || actor == target
                || UnderworldRelics.phase(actor) != 1
                || com.thelongtravail.combat.InsightRetaliation.isRetaliation(source))
            return net.minecraft.world.item.ItemStack.EMPTY;
        return UnderworldRelics.equipped(actor, false);
    }

    public static void heal(
            LivingEntity target,
            DamageSource source,
            net.minecraft.world.item.ItemStack original,
            double loss) {
        if (original.isEmpty()
                || !(loss > 0)
                || !Double.isFinite(loss)
                || !(source.getEntity() instanceof LivingEntity actor)
                || !actor.isAlive()
                || UnderworldRelics.phase(actor) != 1
                || UnderworldRelics.equipped(actor, false) != original) return;
        actor.heal(
                (float) Math.min(Float.MAX_VALUE, loss * UnderworldRelics.value("king.lifesteal")));
    }

    private DemonKingCombat() {}
}

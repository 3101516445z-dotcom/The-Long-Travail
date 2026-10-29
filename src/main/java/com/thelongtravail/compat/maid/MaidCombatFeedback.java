package com.thelongtravail.compat.maid;

import static com.thelongtravail.compat.maid.MaidAccess.identity;
import static com.thelongtravail.compat.maid.MaidAccess.owner;
import static com.thelongtravail.compat.maid.MaidAccessoryRules.canHarm;
import static com.thelongtravail.compat.maid.MaidAccessoryRules.crown;
import static com.thelongtravail.compat.maid.MaidAccessoryRules.has;
import static com.thelongtravail.compat.maid.MaidAccessoryRules.phase;
import static com.thelongtravail.compat.maid.MaidCombatCalculations.attacker;
import static com.thelongtravail.compat.maid.MaidRuntimeState.ledger;
import static com.thelongtravail.compat.maid.MaidRuntimeState.state;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.boundless.TimeStopExemptions;
import com.thelongtravail.boundless.TimeStopManager;
import com.thelongtravail.compat.maid.MaidAccessoryRuntime.Phase;
import com.thelongtravail.config.AzraelConfig;
import com.thelongtravail.config.BoundlessItemsConfig;
import com.thelongtravail.config.FarReachItemsConfig;
import com.thelongtravail.config.FlourishingItemsConfig;
import com.thelongtravail.config.ModeEffectRules;
import com.thelongtravail.flourishing.FloralCombat;
import com.thelongtravail.flourishing.Flower;
import com.thelongtravail.flourishing.FlowerData;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.valley.AzraelExecution;
import com.thelongtravail.valley.AzraelState;
import java.util.HashSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;

// 执行战斗与效果反馈；事件注册由运行适配入口负责。
public final class MaidCombatFeedback {
    public static void received(LivingEntity victim, DamageSource source) {
        var cause = attacker(source);
        if (victim instanceof EntityMaid m) {
            Entity entity =
                    source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
            if (entity != null && entity != m && cause != m) {
                var s = state(m);
                if (s.azrael == Phase.MALICE) s.armed = true;
                rollStar(m, false);
            }
        }
        MaidLanternCombat.received(victim, source);
    }

    public static void confirmed(LivingEntity victim, DamageSource source, boolean rose) {
        var cause = attacker(source);
        if (cause != null && cause != victim && !victim.isSpectator()) {
            rollStar(cause, true);
            if (!rose) {
                if (has(cause, ModRegistry.ICARUS.get())
                        && ModeEffectRules.ICARUS_FIRE.applies(victim)
                        && !victim.fireImmune()
                        && FarReachItemsConfig.IGNITE_SECONDS.get() > 0)
                    victim.setSecondsOnFire(FarReachItemsConfig.IGNITE_SECONDS.get());
                azrael(cause, victim);
            }
        }
        if (!rose) MaidLanternCombat.confirmed(victim, cause);
    }

    private static void azrael(EntityMaid m, LivingEntity victim) {
        if (AzraelExecution.active() || victim instanceof Player p && !canHarm(m, p)) return;
        var s = state(m);
        int outcome = 0;
        if (s.azrael == Phase.WITNESS)
            outcome =
                    AzraelState.witnessOutcome(
                            m.getRandom().nextDouble(),
                            AzraelConfig.TARGET.get(),
                            AzraelConfig.SELF.get());
        else if (s.azrael == Phase.MALICE && s.armed) {
            s.armed = false;
            outcome = m.getRandom().nextDouble() < AzraelConfig.MALICE.get() ? 1 : 0;
        }
        if (outcome == 0) return;
        var target = outcome == 2 ? m : victim;
        long generation = s.generation;
        var level = m.level();
        MaidRuntimeState.defer(
                () -> {
                    if (m.isAlive()
                            && !m.isRemoved()
                            && m.level() == level
                            && target.level() == level
                            && state(m) == s
                            && s.generation == generation) AzraelExecution.execute(m, target);
                });
    }

    private static void rollStar(EntityMaid m, boolean attacking) {
        if (!m.isAlive()
                || m.isRemoved()
                || TimeStopManager.frozen(m)
                || !has(m, ModRegistry.STAR_VOICE.get())
                || TimeStopManager.locked(identity(m))) return;
        var phase = phase(m, TravailAspect.BOUNDLESS);
        if (phase == Phase.INACTIVE || attacking != (phase == Phase.WITNESS)) return;
        boolean witness = phase == Phase.WITNESS;
        var tag = ledger(m);
        if (witness && (tag.getInt("StarCooldown") > 0 || tag.getInt("StarInterval") > 0)) return;
        if (!witness && tag.getInt("StarReceivedInterval") > 0) return;
        tag.putInt(
                witness ? "StarInterval" : "StarReceivedInterval",
                BoundlessItemsConfig.ticks(
                        witness ? "star.witnessInterval" : "star.maliceInterval"));
        MaidAccessoryLedger.get(m.getServer()).setDirty();
        if (m.getRandom().nextDouble()
                >= BoundlessItemsConfig.get(witness ? "star.witnessChance" : "star.maliceChance"))
            return;
        var p = owner(m);
        if (p == null) return;
        var snapshot = TimeStopExemptions.snapshot(p);
        if (snapshot.isEmpty()) return;
        var allies = new HashSet<>(snapshot.get());
        allies.add(m.getUUID());
        TimeStopManager.get((ServerLevel) m.level())
                .startCaster(
                        m,
                        witness,
                        m.level().getGameTime(),
                        m.position(),
                        BoundlessItemsConfig.get("star.radius"),
                        BoundlessItemsConfig.ticks(
                                witness ? "star.witnessDuration" : "star.maliceDuration"),
                        allies);
    }

    public static void successful(LivingEntity victim, DamageSource source) {
        if (FloralCombat.rose(source)) return;
        var m = attacker(source);
        if (m != null && m != victim) {
            apply(m, victim, Flower.LILY_OF_THE_VALLEY, MobEffects.POISON);
            apply(m, victim, Flower.WITHER_ROSE, MobEffects.WITHER);
        }
    }

    public static void retaliate(LivingEntity victim, DamageSource source) {
        if (com.thelongtravail.combat.InsightRetaliation.isRetaliation(source)) return;
        if (victim instanceof EntityMaid defender
                && source.getDirectEntity() instanceof LivingEntity other
                && other != defender
                && !source.is(DamageTypes.THORNS)
                && !source.is(DamageTypeTags.IS_PROJECTILE)
                && !source.is(DamageTypeTags.IS_EXPLOSION)) {
            var s = state(defender);
            long now = defender.level().getGameTime();
            if (FlowerData.active(crown(defender), Flower.ROSE_BUSH)
                    && ready(now, s.rose, Flower.ROSE_BUSH)
                    && ModeEffectRules.FLOWER_THORNS.applies(other)) {
                s.rose = now;
                var damage = FloralCombat.roseSource(defender);
                other.hurt(
                        damage,
                        FloralCombat.safe(
                                defender.getMaxHealth()
                                        * FlourishingItemsConfig.value(Flower.ROSE_BUSH)));
            }
        }
    }

    private static boolean ready(long now, long last, Flower flower) {
        return last == Long.MIN_VALUE
                || now - last
                        >= FlourishingItemsConfig.ticks(
                                FlourishingItemsConfig.extra(flower, "cooldownSeconds"));
    }

    private static void apply(EntityMaid m, LivingEntity victim, Flower flower, MobEffect effect) {
        if (!ModeEffectRules.FLOWER_EFFECTS.applies(victim) || !FlowerData.active(crown(m), flower))
            return;
        long now = m.level().getGameTime();
        var times = MaidRuntimeState.effectTimes(victim);
        times.entrySet().removeIf(e -> e.getValue() <= now);
        String key = identity(m) + ":" + flower.id;
        if (times.containsKey(key) || times.size() >= 256) return;
        times.put(
                key,
                now
                        + FlourishingItemsConfig.ticks(
                                FlourishingItemsConfig.extra(flower, "cooldownSeconds")));
        victim.addEffect(
                new MobEffectInstance(
                        effect,
                        FlourishingItemsConfig.ticks(
                                FlourishingItemsConfig.extra(flower, "durationSeconds")),
                        (int) FlourishingItemsConfig.value(flower) - 1),
                m);
    }

    public static MobEffectInstance extend(
            LivingEntity target, MobEffectInstance effect, Entity source) {
        var m = attacker(source);
        if (m == null
                || target.level().isClientSide
                || !ModeEffectRules.FLOWER_EFFECTS.applies(target)
                || effect.isInfiniteDuration()) return effect;
        var type = effect.getEffect();
        if (type != MobEffects.POISON
                && type != MobEffects.WITHER
                && type != MobEffects.MOVEMENT_SLOWDOWN
                && type != MobEffects.WEAKNESS) return effect;
        double value = FloralCombat.value(crown(m), Flower.LILAC);
        if (value <= 0) return effect;
        var copy = new MobEffectInstance(effect);
        ((com.thelongtravail.mixin.EffectDurationAccessor) copy)
                .travail$duration(
                        (int)
                                Math.min(
                                        Integer.MAX_VALUE,
                                        Math.ceil(effect.getDuration() * (1 + value))));
        return copy;
    }

    public static void heal(LivingHealEvent e) {
        if (e.getEntity() instanceof EntityMaid m && !m.level().isClientSide && e.getAmount() > 0)
            e.setAmount(
                    FloralCombat.safe(
                            e.getAmount() * (1 + FloralCombat.value(crown(m), Flower.PINK_TULIP))));
    }

    public static void effect(MobEffectEvent.Applicable e) {
        if (e.getEntity() instanceof EntityMaid m && has(m, ModRegistry.SWORD_AND_LANTERN.get())) {
            var effect = e.getEffectInstance().getEffect();
            if (effect == MobEffects.DARKNESS
                    || effect == MobEffects.BLINDNESS
                    || effect == MobEffects.CONFUSION) e.setResult(Event.Result.DENY);
        }
    }

    public static void death(LivingDeathEvent e) {
        if (e.getEntity().level().isClientSide || !(e.getEntity() instanceof Enemy)) return;
        var m = attacker(e.getSource());
        if (m == null) return;
        double value = FloralCombat.value(crown(m), Flower.PITCHER_PLANT);
        if (value <= 0) return;
        MaidRuntimeState.defer(
                () -> {
                    if (e.isCanceled() || e.getEntity().isAlive() || !m.isAlive() || m.isRemoved())
                        return;
                    var s = state(m);
                    long now = m.level().getGameTime();
                    if (ready(now, s.kill, Flower.PITCHER_PLANT)) {
                        s.kill = now;
                        m.heal(FloralCombat.safe(m.getMaxHealth() * value));
                    }
                });
    }

    private MaidCombatFeedback() {}
}

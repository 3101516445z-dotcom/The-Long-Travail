package com.thelongtravail.event;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.*;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

final class TravailEffectActions {
    private static final String FLYING_PROJECTILE = "LongTravailFlyingProjectile";
    private static final String ATTACK_CLEANSE_COOLDOWN = "AttackCleanseCooldown";
    private static final ThreadLocal<java.util.Set<java.util.UUID>> ATTACK_CLEANSE_ACTIVE = new ThreadLocal<>();
    private static final String VALLEY_WITNESS_COOLDOWN = "ValleyWitnessCooldown";
    static void processBeingAttacked(ServerPlayer player) {
        ItemStack travail = TravailCurios.stack(player);
        if (travail.isEmpty()) return;
        RandomSource random = player.getRandom();
        if (!LongTravailData.hasWitness(travail, TravailAspect.FAR_REACH)) {
            if (random.nextDouble() < TravailConfig.FAR_ALL_CLEAR_CHANCE.get()) removeAllBeneficial(player); else removeOneBeneficial(player);
        }
        if (!LongTravailData.hasWitness(travail, TravailAspect.DEEP_VALLEY)
                && random.nextDouble() < TravailConfig.VALLEY_VISUAL_DEPRIVATION_CHANCE.get()) {
            VisualDeprivation.start(player, secondsToTicks(TravailConfig.VALLEY_VISUAL_DEPRIVATION_DURATION_SECONDS.get()));
        }
    }

    static void processSuccessfulAttack(ServerPlayer player) {
        ItemStack travail = TravailCurios.stack(player);
        if (travail.isEmpty()) return;
        RandomSource random = player.getRandom();
        processAttackCleanse(player, travail, random);
        if (!LongTravailData.hasWitness(travail, TravailAspect.DEEP_VALLEY)) {
            if (random.nextDouble() < TravailConfig.VALLEY_STIFF_CHANCE.get()) {
                StiffState.start(player, secondsToTicks(TravailConfig.VALLEY_STIFF_DURATION_SECONDS.get()));
            }
        } else {
            long now = player.level().getGameTime();
            if (now < LongTravailData.getLong(travail, VALLEY_WITNESS_COOLDOWN)) return;
            LongTravailData.putLong(travail, VALLEY_WITNESS_COOLDOWN,
                    now + secondsToTicks(TravailConfig.VALLEY_WITNESS_ATTACK_COOLDOWN_SECONDS.get()));
            if (AutomaticEffectBudget.acquire(player)) EffectChanges.add(player, new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                    secondsToTicks(TravailConfig.VALLEY_SWIFTNESS_DURATION_SECONDS.get()), TravailConfig.VALLEY_SWIFTNESS_LEVEL.get() - 1));
            for (int i = 0; i < TravailConfig.VALLEY_RANDOM_EFFECT_COUNT.get(); i++) {
                grantRandomBeneficial(player, RuntimePools.current().valley(),
                        secondsToTicks(TravailConfig.VALLEY_RANDOM_EFFECT_DURATION_SECONDS.get()));
            }
        }
    }

    static void processAttackCleanse(ServerPlayer player, ItemStack travail, RandomSource random) {
        if (LongTravailData.hasWitness(travail, TravailAspect.FAR_REACH)) return;
        var active = ATTACK_CLEANSE_ACTIVE.get();
        var id = player.getUUID();
        if (active != null && active.contains(id)) return;
        double seconds = TravailConfig.FAR_ATTACK_COOLDOWN_SECONDS.get();
        int cooldown = seconds <= 0 ? 0 : secondsToTicks(seconds);
        long now = player.level().getGameTime();
        if (cooldown > 0 && now < LongTravailData.getLong(travail, ATTACK_CLEANSE_COOLDOWN)) return;
        if (random.nextDouble() >= TravailConfig.FAR_ATTACK_CLEAR_CHANCE.get()) return;
        if (active == null) {
            active = new java.util.HashSet<>();
            ATTACK_CLEANSE_ACTIVE.set(active);
        }
        active.add(id);
        try {
            // 先预留冷却；零冷却仅阻止清除回调内的重入，不限制独立攻击。
            LongTravailData.putLong(travail, ATTACK_CLEANSE_COOLDOWN, now + cooldown);
            removeOneBeneficial(player);
        } finally {
            active.remove(id);
            if (active.isEmpty()) ATTACK_CLEANSE_ACTIVE.remove();
        }
    }

    static void processFarReachWitness(ServerPlayer player) {
        int actions = TravailConfig.FAR_WITNESS_ACTION_COUNT.get();
        int removed = CleanseRotation.remove(player, actions, () -> AutomaticEffectBudget.acquire(player));
        for (int i = removed; i < actions; i++) grantRandomBeneficial(player, RuntimePools.current().far(),
                secondsToTicks(TravailConfig.FAR_WITNESS_POSITIVE_DURATION_SECONDS.get()));
    }

    static boolean isFlyingAttack(ServerPlayer player, DamageSource source) {
        Entity direct = source.getDirectEntity();
        return direct instanceof Projectile projectile
                ? projectile.getPersistentData().getBoolean(FLYING_PROJECTILE)
                : isFlying(player);
    }

    static boolean isFlying(ServerPlayer player) {
        return player.getAbilities().flying || player.isFallFlying();
    }

    static void removeOneBeneficial(ServerPlayer player) {
        List<MobEffect> effects = beneficialEffects(player);
        if (!effects.isEmpty()) EffectChanges.remove(player, effects.get(player.getRandom().nextInt(effects.size())), true);
    }

    static void removeAllBeneficial(ServerPlayer player) {
        beneficialEffects(player).forEach(effect -> EffectChanges.remove(player, effect, true));
    }

    static List<MobEffect> beneficialEffects(ServerPlayer player) {
        return player.getActiveEffects().stream().filter(instance -> instance.getEffect().getCategory() == MobEffectCategory.BENEFICIAL)
                .map(MobEffectInstance::getEffect).distinct().toList();
    }

    static void grantRandomBeneficial(ServerPlayer player, WeightedTable<EffectEntry> configured, int duration) {
        if (configured.entries().isEmpty() || !AutomaticEffectBudget.acquire(player)) return;
        EffectEntry selected = configured.choose(player.getRandom());
        if (selected == null) return;
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(selected.id());
        int amplifier = player.getRandom().nextInt(selected.maxLevel());
        EffectChanges.add(player, new MobEffectInstance(effect, duration, amplifier));
    }

    static int configuredMaxLevel(MobEffect effect) {
        ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        if (id == null) return TravailConfig.FAR_WITNESS_FALLBACK_MAX_LEVEL.get();
        return RuntimePools.current().maxLevels().getOrDefault(id, TravailConfig.FAR_WITNESS_FALLBACK_MAX_LEVEL.get());
    }

    static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) return;
        ItemStack travail = TravailCurios.stack(player);
        if (travail.isEmpty()) return;
        if (!LongTravailData.hasWitness(travail, TravailAspect.FLOURISHING)) {
            event.setAmount(event.getAmount() * (1.0F - TravailConfig.FLOURISHING_HEALING_REDUCTION.get().floatValue()));
            return;
        }
        double bonus = FlourishingBonus.calculate(PlayerJourneyData.discoveredBiomeCount(player),
                TravailConfig.FLOURISHING_BONUS_PER_BIOME.get(), TravailConfig.MAX_HEALING_BONUS.get());
        event.setAmount(safeMultiply(event.getAmount(), 1.0F + (float) bonus));
    }

    static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MobEffect effect = event.getEffectInstance().getEffect();
        if (effect != MobEffects.DARKNESS && effect != MobEffects.BLINDNESS) return;
        ItemStack travail = TravailCurios.stack(player);
        if (!travail.isEmpty() && LongTravailData.hasWitness(travail, TravailAspect.DEEP_VALLEY)) {
            event.setResult(Event.Result.DENY);
        }
    }

    static void onEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MobEffectInstance added = event.getEffectInstance();
        EffectChanges.externalChange(player, added.getEffect());
        if (added.getEffect().getCategory() != MobEffectCategory.BENEFICIAL) return;
        ItemStack travail = TravailCurios.stack(player);
        if (travail.isEmpty() || !LongTravailData.hasWitness(travail, TravailAspect.FAR_REACH)) return;
        ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(added.getEffect());
        if (effectId != null && RuntimePools.current().levelBonusBlacklist().contains(effectId)) return;
        int maxLevel = configuredMaxLevel(added.getEffect());
        // Added 事件早于外层 addEffect 写入或合并实例。
        // 须直接升级传入实例；递归调用 addEffect 的结果会在首次添加时被覆盖。
        EffectUpgrade.apply(added, TravailConfig.FAR_WITNESS_LEVEL_BONUS.get(), maxLevel);
    }

    static void onJump(LivingEvent.LivingJumpEvent event) {
        if (StiffState.active(event.getEntity())) {
            Vec3 motion = event.getEntity().getDeltaMovement();
            if (motion.y > 0.0D) event.getEntity().setDeltaMovement(motion.x, 0.0D, motion.z);
        }
    }

    static void onProjectileCreated(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Projectile projectile)
                || !(projectile.getOwner() instanceof ServerPlayer player)) return;
        if (isFlying(player)) projectile.getPersistentData().putBoolean(FLYING_PROJECTILE, true);
    }

    static float safeMultiply(float amount, float multiplier) {
        double result = (double) amount * multiplier;
        return !Double.isFinite(result) || result >= Float.MAX_VALUE ? Float.MAX_VALUE : (float) Math.max(0.0D, result);
    }

    static int secondsToTicks(double seconds) {
        if (seconds <= 0.0D) return 1;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, Math.round(seconds * 20.0D)));
    }

    private TravailEffectActions() {}
}

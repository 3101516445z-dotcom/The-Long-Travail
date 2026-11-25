package com.thelongtravail.flourishing;

import static com.thelongtravail.combat.DamageReduction.remaining;

import com.thelongtravail.compat.maidspell.MaidSpellFlowers;
import com.thelongtravail.config.FlourishingItemsConfig;
import com.thelongtravail.integration.MaidIntegration;
import com.thelongtravail.runtime.scheduling.TickDeadline;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;

public final class FloralCombat {
    private static final int MAX_FLOWER_COOLDOWNS_PER_TARGET = 256;

    public static boolean hasFlowerIntervalElapsed(long lastTick, long now, Flower flower) {
        int interval =
                FlourishingItemsConfig.ticks(
                        FlourishingItemsConfig.extra(flower, "cooldownSeconds"));
        return TickDeadline.hasElapsed(lastTick, now, interval);
    }

    /** 调用方传入该目标的冷却记录和攻击者标识，同一朵花按攻击者分别计时。 */
    public static boolean tryStartFlowerCooldown(
            Map<String, Long> deadlines, UUID attackerId, Flower flower, long now) {
        deadlines.values().removeIf(deadline -> deadline <= now);
        String key = attackerId + ":" + flower.id;
        if (deadlines.containsKey(key) || deadlines.size() >= MAX_FLOWER_COOLDOWNS_PER_TARGET)
            return false;

        int duration =
                FlourishingItemsConfig.ticks(
                        FlourishingItemsConfig.extra(flower, "cooldownSeconds"));
        deadlines.put(key, now + duration);
        return true;
    }

    private static final class Rose extends DamageSource {
        Rose(LivingEntity actor) {
            super(
                    com.thelongtravail.combat.DirectStrikeDamage.source(actor.level(), actor, actor)
                            .typeHolder(),
                    actor);
        }
    }

    public static DamageSource roseSource(LivingEntity actor) {
        return new Rose(actor);
    }

    // 保留旧类型键供已有数据包使用；本模组新伤害统一由直击伤害来源工厂创建。
    public static final ResourceKey<DamageType> ROSE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation("the_long_travail", "rose_retaliation"));

    public static boolean rose(DamageSource s) {
        s = com.thelongtravail.combat.DamageIdentity.original(s);
        return s instanceof Rose || s.is(ROSE);
    }

    public static double bonus(LivingEntity victim, DamageSource source) {
        if (rose(source)) return 0;
        Entity direct = source.getEntity();
        ServerPlayer player = Affection.attacker(direct);
        if (player == null) player = Affection.attacker(source.getDirectEntity());
        double result = 0;
        if (player != null && player != victim) {
            ItemStack crown = Affection.equipped(player, true);
            result += crownBonus(player, crown, source, victim);
            if (direct == player) {
                LivingEntity pet = Affection.target(player);
                if (pet != null)
                    result +=
                            FlourishingItemsConfig.get("affection.playerDamageBonus")
                                    * Affection.lost(pet);
            }
        }
        if (direct instanceof LivingEntity living && !(living instanceof ServerPlayer)) {
            ServerPlayer master = Affection.master(living);
            if (master != null)
                result +=
                        FlourishingItemsConfig.get("affection.servantDamageBonus")
                                * Affection.lost(master);
        }
        return result;
    }

    public static double remainingMultiplier(
            LivingEntity victim, DamageSource source, boolean poison) {
        if (rose(source)) return 1;
        double multiplier = 1;
        if (victim instanceof ServerPlayer p) {
            ItemStack crown = Affection.equipped(p, true);
            multiplier = crownMultiplier(p, crown, source, poison);
            LivingEntity pet = Affection.target(p);
            if (pet != null)
                multiplier *=
                        remaining(
                                FlourishingItemsConfig.get("affection.playerDamageReduction")
                                        * Affection.lost(pet));
        } else {
            ServerPlayer master = Affection.master(victim);
            if (master != null)
                multiplier *=
                        remaining(
                                FlourishingItemsConfig.get("affection.servantDamageReduction")
                                        * Affection.lost(master));
        }
        return multiplier;
    }

    public static double crownBonus(
            LivingEntity wearer, ItemStack crown, DamageSource source, LivingEntity victim) {
        double bonus = value(crown, Flower.POPPY);
        if (wearer.getHealth()
                < wearer.getMaxHealth()
                        * FlourishingItemsConfig.extra(Flower.RED_TULIP, "healthThreshold"))
            bonus += value(crown, Flower.RED_TULIP);
        if (source.is(DamageTypeTags.IS_PROJECTILE)) bonus += value(crown, Flower.CORNFLOWER);
        if (sunny(wearer)) bonus += value(crown, Flower.SUNFLOWER);
        return bonus + MaidSpellFlowers.damageBonus(crown, wearer, victim);
    }

    // 保持 double 精度，调用方叠加其他减伤后再转换为最终伤害。
    public static double crownMultiplier(
            LivingEntity wearer, ItemStack crown, DamageSource source, boolean poison) {
        double multiplier = remaining(MaidSpellFlowers.reduction(crown, wearer));
        if (poison) multiplier *= remaining(value(crown, Flower.ALLIUM));
        if (source.is(DamageTypeTags.IS_FALL))
            multiplier *= remaining(value(crown, Flower.AZURE_BLUET));
        if (source.is(DamageTypeTags.IS_FIRE))
            multiplier *= remaining(value(crown, Flower.TORCHFLOWER));
        if (wearer.getHealth()
                < wearer.getMaxHealth()
                        * FlourishingItemsConfig.extra(Flower.WHITE_TULIP, "healthThreshold"))
            multiplier *= remaining(value(crown, Flower.WHITE_TULIP));
        return multiplier;
    }

    public static double healingBonus(LivingEntity wearer) {
        ItemStack crown =
                wearer instanceof ServerPlayer player
                        ? Affection.equipped(player, true)
                        : MaidIntegration.springCrown(wearer);
        if (crown.isEmpty()) return 0;
        return value(crown, Flower.PINK_TULIP) + MaidSpellFlowers.healingBonus(crown, wearer);
    }

    public static void roseHit(ServerPlayer player, LivingEntity attacker) {
        if (!com.thelongtravail.config.ModeEffectRules.FLOWER_THORNS.applies(attacker)) return;
        DamageSource source = roseSource(player);
        attacker.hurt(
                source,
                safe(player.getMaxHealth() * FlourishingItemsConfig.value(Flower.ROSE_BUSH)));
    }

    public static double value(ItemStack crown, Flower f) {
        return FlowerData.active(crown, f) ? FlourishingItemsConfig.value(f) : 0;
    }

    public static boolean sunny(ServerPlayer p) {
        return sunny((LivingEntity) p);
    }

    public static double crownMovementBonus(LivingEntity wearer, ItemStack crown) {
        double speed = value(crown, Flower.PINK_PETALS);
        if (FlowerData.active(crown, Flower.SUNFLOWER) && sunny(wearer))
            speed += FlourishingItemsConfig.extra(Flower.SUNFLOWER, "speedBonus");
        return speed;
    }

    private static boolean sunny(LivingEntity p) {
        return p.level().isDay() && p.level().canSeeSky(p.blockPosition());
    }

    public static float safe(double amount) {
        return (float) Math.max(0, Math.min(Float.MAX_VALUE, amount));
    }

    private FloralCombat() {}
}

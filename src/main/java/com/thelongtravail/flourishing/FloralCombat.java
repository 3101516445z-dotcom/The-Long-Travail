package com.thelongtravail.flourishing;

import com.thelongtravail.config.FlourishingItemsConfig;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;

public final class FloralCombat {
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
            result += crownBonus(player, crown, source);
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

    public static float reduce(LivingEntity victim, DamageSource source, float amount) {
        if (rose(source)) return amount;
        double multiplier = 1;
        if (victim instanceof ServerPlayer p) {
            ItemStack crown = Affection.equipped(p, true);
            var context = com.thelongtravail.helper.CombatContext.current(victim, source);
            multiplier =
                    crownReduction(p, crown, source, context != null && context.isFloralPoison());
            LivingEntity pet = Affection.target(p);
            if (pet != null)
                multiplier *=
                        1
                                - FlourishingItemsConfig.get("affection.playerDamageReduction")
                                        * Affection.lost(pet);
        } else {
            ServerPlayer master = Affection.master(victim);
            if (master != null)
                multiplier *=
                        1
                                - FlourishingItemsConfig.get("affection.servantDamageReduction")
                                        * Affection.lost(master);
        }
        return safe(amount * multiplier);
    }

    public static double crownBonus(LivingEntity wearer, ItemStack crown, DamageSource source) {
        double bonus = value(crown, Flower.POPPY);
        if (wearer.getHealth()
                < wearer.getMaxHealth()
                        * FlourishingItemsConfig.extra(Flower.RED_TULIP, "healthThreshold"))
            bonus += value(crown, Flower.RED_TULIP);
        if (source.is(DamageTypeTags.IS_PROJECTILE)) bonus += value(crown, Flower.CORNFLOWER);
        if (sunny(wearer)) bonus += value(crown, Flower.SUNFLOWER);
        return bonus;
    }

    // 保持 double 精度，调用方叠加其他减伤后再转换为最终伤害。
    public static double crownReduction(
            LivingEntity wearer, ItemStack crown, DamageSource source, boolean poison) {
        double multiplier = 1;
        if (poison) multiplier *= 1 - value(crown, Flower.ALLIUM);
        if (source.is(DamageTypeTags.IS_FALL)) multiplier *= 1 - value(crown, Flower.AZURE_BLUET);
        if (source.is(DamageTypeTags.IS_FIRE)) multiplier *= 1 - value(crown, Flower.TORCHFLOWER);
        if (wearer.getHealth()
                < wearer.getMaxHealth()
                        * FlourishingItemsConfig.extra(Flower.WHITE_TULIP, "healthThreshold"))
            multiplier *= 1 - value(crown, Flower.WHITE_TULIP);
        return multiplier;
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

    private static boolean sunny(LivingEntity p) {
        return p.level().isDay() && p.level().canSeeSky(p.blockPosition());
    }

    public static float safe(double amount) {
        return (float) Math.max(0, Math.min(Float.MAX_VALUE, amount));
    }

    private FloralCombat() {}
}

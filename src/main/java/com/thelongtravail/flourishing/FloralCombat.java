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
    public static final ResourceKey<DamageType> ROSE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation("the_long_travail", "rose_retaliation"));

    public static boolean rose(DamageSource s) {
        s = com.thelongtravail.combat.DamageIdentity.original(s);
        return s.is(ROSE);
    }

    public static double bonus(LivingEntity victim, DamageSource source) {
        if (rose(source)) return 0;
        Entity direct = source.getEntity();
        ServerPlayer player = Affection.attacker(direct);
        if (player == null) player = Affection.attacker(source.getDirectEntity());
        double result = 0;
        if (player != null && player != victim) {
            ItemStack crown = Affection.equipped(player, true);
            result += value(crown, Flower.POPPY);
            if (player.getHealth()
                    < player.getMaxHealth()
                            * FlourishingItemsConfig.extra(Flower.RED_TULIP, "healthThreshold"))
                result += value(crown, Flower.RED_TULIP);
            if (source.is(DamageTypeTags.IS_PROJECTILE)) result += value(crown, Flower.CORNFLOWER);
            if (sunny(player)) result += value(crown, Flower.SUNFLOWER);
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
            if (context != null && context.isFloralPoison())
                multiplier *= 1 - value(crown, Flower.ALLIUM);
            if (source.is(DamageTypeTags.IS_FALL))
                multiplier *= 1 - value(crown, Flower.AZURE_BLUET);
            if (source.is(DamageTypeTags.IS_FIRE))
                multiplier *= 1 - value(crown, Flower.TORCHFLOWER);
            if (p.getHealth()
                    < p.getMaxHealth()
                            * FlourishingItemsConfig.extra(Flower.WHITE_TULIP, "healthThreshold"))
                multiplier *= 1 - value(crown, Flower.WHITE_TULIP);
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

    public static void roseHit(ServerPlayer player, LivingEntity attacker) {
        if (!com.thelongtravail.config.ModeEffectRules.FLOWER_THORNS.applies(attacker)) return;
        DamageSource source =
                new DamageSource(
                        player.serverLevel()
                                .registryAccess()
                                .registryOrThrow(Registries.DAMAGE_TYPE)
                                .getHolderOrThrow(ROSE),
                        player);
        attacker.hurt(
                source,
                safe(player.getMaxHealth() * FlourishingItemsConfig.value(Flower.ROSE_BUSH)));
    }

    public static double value(ItemStack crown, Flower f) {
        return FlowerData.active(crown, f) ? FlourishingItemsConfig.value(f) : 0;
    }

    public static boolean sunny(ServerPlayer p) {
        return p.level().isDay() && p.level().canSeeSky(p.blockPosition());
    }

    public static float safe(double amount) {
        return (float) Math.max(0, Math.min(Float.MAX_VALUE, amount));
    }

    private FloralCombat() {}
}

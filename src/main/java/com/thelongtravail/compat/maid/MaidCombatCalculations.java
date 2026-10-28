package com.thelongtravail.compat.maid;

import static com.thelongtravail.compat.maid.MaidAccessoryRules.crown;
import static com.thelongtravail.compat.maid.MaidAccessoryRules.eki;
import static com.thelongtravail.compat.maid.MaidAccessoryRules.has;
import static com.thelongtravail.compat.maid.MaidAccessoryRules.phase;
import static com.thelongtravail.compat.maid.MaidAccessoryRules.sunny;
import static com.thelongtravail.compat.maid.MaidAccessoryRules.tokaido;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.compat.maid.MaidAccessoryRuntime.Phase;
import com.thelongtravail.config.FarReachItemsConfig;
import com.thelongtravail.config.FlourishingItemsConfig;
import com.thelongtravail.config.RainConfig;
import com.thelongtravail.config.UnderworldItemsConfig;
import com.thelongtravail.farreach.IcarusEnvironment;
import com.thelongtravail.flourishing.FloralCombat;
import com.thelongtravail.flourishing.Flower;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

// 计算伤害与免疫规则；短路判断顺序和随机数消耗属于行为约定。
public final class MaidCombatCalculations {
    // 安全门面的调用签名只暴露原版类型，避免缺少女仆模组时验证 EntityMaid 的继承关系。
    public static LivingEntity damageActor(DamageSource source) {
        return attacker(source);
    }

    public static EntityMaid attacker(Entity source) {
        Set<Entity> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (source != null && seen.add(source)) {
            if (source instanceof EntityMaid m) return m;
            if (source instanceof Projectile projectile) source = projectile.getOwner();
            else return null;
        }
        return null;
    }

    static EntityMaid attacker(DamageSource source) {
        var m = attacker(source.getEntity());
        return m == null ? attacker(source.getDirectEntity()) : m;
    }

    public static double bonus(LivingEntity victim, DamageSource source) {
        var m = attacker(source);
        if (m == null || m == victim || m.level().isClientSide) return 0;
        var crown = crown(m);
        double value = FloralCombat.value(crown, Flower.POPPY);
        if (m.getHealth()
                < m.getMaxHealth()
                        * FlourishingItemsConfig.extra(Flower.RED_TULIP, "healthThreshold"))
            value += FloralCombat.value(crown, Flower.RED_TULIP);
        if (source.is(DamageTypeTags.IS_PROJECTILE))
            value += FloralCombat.value(crown, Flower.CORNFLOWER);
        if (sunny(m)) value += FloralCombat.value(crown, Flower.SUNFLOWER);
        if (eki(m) == Phase.MALICE) value += RainConfig.DAMAGE.get();
        if (has(m, ModRegistry.ICARUS.get())) {
            var phase = phase(m, TravailAspect.FAR_REACH);
            if (phase == Phase.WITNESS && IcarusEnvironment.openSky(m))
                value += FarReachItemsConfig.get("icarus.witnessBonus");
            if (phase == Phase.MALICE)
                value +=
                        IcarusEnvironment.sunlight(m.level().getDayTime())
                                * FarReachItemsConfig.get(
                                        IcarusEnvironment.sunny(m.level())
                                                        && IcarusEnvironment.openSky(m)
                                                ? "icarus.sunnyPeak"
                                                : "icarus.malicePeak");
        }
        return value
                + MaidLanternCombat.bonus(m, victim)
                + com.thelongtravail.abyss.MidnightLafite.bonus(m);
    }

    public static double witherBonus(LivingEntity victim, DamageSource source) {
        var m = attacker(source);
        if (m == null
                || !has(m, ModRegistry.THOUSAND_YEARS.get())
                || phase(m, TravailAspect.UNDERWORLD) != Phase.MALICE) return 0;
        var wither = victim.getEffect(MobEffects.WITHER);
        return wither == null
                ? 0
                : ((double) wither.getAmplifier() + 1) * UnderworldItemsConfig.WITHER_BONUS.get();
    }

    public static boolean immune(LivingEntity victim, DamageSource source, float amount) {
        if (!(victim instanceof EntityMaid m) || m.level().isClientSide) return false;
        if (has(m, ModRegistry.ICARUS.get())) {
            if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.FLY_INTO_WALL))
                return true;
            if (phase(m, TravailAspect.FAR_REACH) == Phase.WITNESS) {
                if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC))
                    return true;
                for (String entry : FarReachItemsConfig.EXTRA_IMMUNITIES.get()) {
                    boolean tag = entry.startsWith("#");
                    var id = ResourceLocation.tryParse(tag ? entry.substring(1) : entry);
                    if (id != null
                            && (tag
                                    ? source.is(TagKey.create(Registries.DAMAGE_TYPE, id))
                                    : source.typeHolder()
                                            .unwrapKey()
                                            .map(k -> k.location().equals(id))
                                            .orElse(false))) return true;
                }
            }
        }
        if (source.is(DamageTypeTags.IS_FIRE) && RainConfig.FIRE.get() && tokaido(m)) return true;
        return amount > 0
                && !source.is(DamageTypes.GENERIC_KILL)
                && eki(m) == Phase.WITNESS
                && !RainConfig.IMMUNITY_EXCLUSIONS
                        .get()
                        .contains(
                                source.typeHolder()
                                        .unwrapKey()
                                        .map(k -> k.location().toString())
                                        .orElse(""))
                && m.getRandom().nextDouble() < RainConfig.IMMUNITY.get();
    }

    public static float reduce(
            LivingEntity victim, DamageSource source, float amount, boolean poison) {
        if (!(victim instanceof EntityMaid m) || FloralCombat.rose(source)) return amount;
        var crown = crown(m);
        double multiplier = 1;
        if (poison) multiplier *= 1 - FloralCombat.value(crown, Flower.ALLIUM);
        if (source.is(DamageTypeTags.IS_FALL))
            multiplier *= 1 - FloralCombat.value(crown, Flower.AZURE_BLUET);
        if (source.is(DamageTypeTags.IS_FIRE))
            multiplier *= 1 - FloralCombat.value(crown, Flower.TORCHFLOWER);
        if (m.getHealth()
                < m.getMaxHealth()
                        * FlourishingItemsConfig.extra(Flower.WHITE_TULIP, "healthThreshold"))
            multiplier *= 1 - FloralCombat.value(crown, Flower.WHITE_TULIP);
        return FloralCombat.safe(amount * multiplier);
    }

    private MaidCombatCalculations() {}
}

package com.thelongtravail.farreach;

import com.thelongtravail.config.FarReachItemsConfig;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

// 装备资格与主题阶段由调用方判定，环境查询使用实际佩戴者。
public final class IcarusCombat {
    public static double witnessBonus(LivingEntity wearer) {
        return IcarusEnvironment.openSky(wearer)
                ? FarReachItemsConfig.get("icarus.witnessBonus")
                : 0;
    }

    public static double maliceBonus(LivingEntity wearer, double sunlight) {
        return sunlight
                * FarReachItemsConfig.get(
                        IcarusEnvironment.sunny(wearer.level()) && IcarusEnvironment.openSky(wearer)
                                ? "icarus.sunnyPeak"
                                : "icarus.malicePeak");
    }

    public static boolean baseImmune(DamageSource source) {
        return source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.FLY_INTO_WALL);
    }

    public static boolean witnessImmune(DamageSource source) {
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) return true;
        for (String entry : FarReachItemsConfig.EXTRA_IMMUNITIES.get()) {
            boolean tag = entry.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? entry.substring(1) : entry);
            if (id != null
                    && (tag
                            ? source.is(TagKey.create(Registries.DAMAGE_TYPE, id))
                            : source.typeHolder()
                                    .unwrapKey()
                                    .map(k -> k.location().equals(id))
                                    .orElse(false))) return true;
        }
        return false;
    }

    private IcarusCombat() {}
}

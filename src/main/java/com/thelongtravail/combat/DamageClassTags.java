package com.thelongtravail.combat;

import com.thelongtravail.TheLongTravail;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

// 公共伤害分类只描述类型集合，效果数值与结算策略由调用方决定。
public final class DamageClassTags {
    public static final TagKey<DamageType> PHYSICAL = tag("physical_damage");
    public static final TagKey<DamageType> MAGIC = tag("magic_damage");

    private static TagKey<DamageType> tag(String path) {
        return TagKey.create(
                Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(TheLongTravail.MODID, path));
    }

    private DamageClassTags() {}
}

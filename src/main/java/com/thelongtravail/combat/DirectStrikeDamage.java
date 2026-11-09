package com.thelongtravail.combat;

import com.thelongtravail.TheLongTravail;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

// 直击伤害的类型键与来源工厂；穿透防御由类型标签定义，吸收生命由伤害桥接跳过，保留正常受伤结算流程。
public final class DirectStrikeDamage {
    // 使用调用方在原攻击结算前记录的生命值，避免追加伤害时重新取值。
    public static double fromHealthSnapshot(double health, double fraction) {
        if (!Double.isFinite(health)
                || health <= 0
                || !Double.isFinite(fraction)
                || fraction <= 0) return 0;
        return Math.min(Float.MAX_VALUE, health * fraction);
    }

    public static final ResourceKey<DamageType> TYPE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(TheLongTravail.MODID, "direct_strike"));

    // 从目标所在世界的注册表创建来源；本方法不造成伤害，调用方应在服务端通过目标的 hurt 方法结算。
    //
    // 参数 level 目标所在的世界
    // 参数 directEntity 直接造成伤害的实体；近战传攻击者，间接攻击传投射物，无来源时传 null
    // 参数 causingEntity 归属的攻击者；间接攻击传投射物的拥有者，无归属时传 null
    // 返回值：保留直接来源与攻击者归属的直击伤害来源
    public static DamageSource source(
            Level level, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
        return new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(TYPE),
                directEntity,
                causingEntity);
    }

    private DirectStrikeDamage() {}
}

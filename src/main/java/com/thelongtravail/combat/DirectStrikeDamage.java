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

/** 直击伤害的类型键与来源工厂；穿透范围由伤害类型标签定义，保留正常受伤结算流程。 */
public final class DirectStrikeDamage {
    public static final ResourceKey<DamageType> TYPE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(TheLongTravail.MODID, "direct_strike"));

    /**
     * 从目标所在世界的注册表创建来源；本方法不造成伤害，调用方应在服务端通过目标的 hurt 方法结算。
     *
     * @param level 目标所在的世界
     * @param directEntity 直接造成伤害的实体；近战传攻击者，间接攻击传投射物，无来源时传 null
     * @param causingEntity 归属的攻击者；间接攻击传投射物的拥有者，无归属时传 null
     * @return 保留直接来源与攻击者归属的直击伤害来源
     */
    public static DamageSource source(
            Level level, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
        return new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TYPE),
                directEntity,
                causingEntity);
    }

    private DirectStrikeDamage() {}
}

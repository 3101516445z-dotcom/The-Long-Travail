package com.thelongtravail.combat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** 穿透类型与原始攻击身份分离；原来源实例用于重放、反击和处决认领。 */
public final class DamageIdentity {
    private static final class Converted extends DamageSource {
        private final DamageSource original;

        Converted(LivingEntity target, DamageSource original) {
            super(
                    DirectStrikeDamage.source(
                                    target.level(),
                                    original.getDirectEntity(),
                                    original.getEntity())
                            .typeHolder(),
                    original.getDirectEntity(),
                    original.getEntity(),
                    original.sourcePositionRaw());
            this.original = original;
        }
    }

    public static DamageSource original(DamageSource source) {
        return source instanceof Converted converted ? converted.original : source;
    }

    public static DamageSource convert(LivingEntity target, DamageSource source) {
        return source.is(DirectStrikeDamage.TYPE)
                ? source
                : new Converted(target, original(source));
    }

    private DamageIdentity() {}
}

package com.thelongtravail.combat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;

// 服务端伤害结算造成实际生命值损失后，通过 Forge 事件总线通知订阅者。
public final class DamageHealthLossEvent extends LivingEvent {
    private final DamageSource source;
    private final double amount;

    DamageHealthLossEvent(LivingEntity target, DamageSource source, double amount) {
        super(target);
        this.source = source;
        this.amount = amount;
    }

    public DamageSource source() {
        return source;
    }

    public double amount() {
        return amount;
    }
}

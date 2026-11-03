package com.thelongtravail.underworld;

import net.minecraft.world.entity.*;

// 仅为同步攻击附魔回调中的当前目标提供类型视角；嵌套作用域按栈恢复。
public final class AttackMobTypeContext implements AutoCloseable {
    private static final ThreadLocal<AttackMobTypeContext> CURRENT = new ThreadLocal<>();
    private final AttackMobTypeContext previous;
    private final LivingEntity target;

    private AttackMobTypeContext(LivingEntity target) {
        previous = CURRENT.get();
        this.target = target;
        CURRENT.set(this);
    }

    public static AttackMobTypeContext open(LivingEntity attacker, Entity target) {
        return new AttackMobTypeContext(
                attacker != null
                                && attacker != target
                                && target instanceof LivingEntity living
                                && UnderworldItems.seesUndead(attacker)
                        ? living
                        : null);
    }

    public static AttackMobTypeContext suspend() {
        return new AttackMobTypeContext(null);
    }

    public static MobType resolve(LivingEntity queried, MobType actual) {
        AttackMobTypeContext current = CURRENT.get();
        return current != null && current.target == queried ? MobType.UNDEAD : actual;
    }

    @Override
    public void close() {
        if (previous == null) CURRENT.remove();
        else CURRENT.set(previous);
    }
}

package com.thelongtravail.runtime.effects;

import com.thelongtravail.mixin.effects.EffectDurationAccessor;

import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.IntUnaryOperator;

// 先验证完整隐藏链，再复制；拒绝异常链时不修改输入或交付部分副本。
public final class EffectCopies {
    public static final int MAX_CHAIN_LENGTH = 128;

    public static final class InvalidChain extends IllegalArgumentException {
        private InvalidChain(String message) {
            super(message);
        }
    }

    public static MobEffectInstance copy(MobEffectInstance incoming) {
        return copy(incoming, duration -> duration);
    }

    public static MobEffectInstance shortened(MobEffectInstance incoming, double multiplier) {
        return copy(
                incoming,
                duration -> duration > 0 ? (int) Math.ceil(duration * multiplier) : duration);
    }

    private static MobEffectInstance copy(MobEffectInstance incoming, IntUnaryOperator duration) {
        var chain = new ArrayList<MobEffectInstance>();
        Set<MobEffectInstance> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var node = incoming;
                node != null;
                node = ((EffectDurationAccessor) node).travail$hiddenEffect()) {
            if (!seen.add(node)) throw new InvalidChain("Cyclic hidden effect chain");
            if (chain.size() >= MAX_CHAIN_LENGTH)
                throw new InvalidChain("Hidden effect chain exceeds " + MAX_CHAIN_LENGTH);
            chain.add(node);
        }
        MobEffectInstance result = null;
        for (int index = chain.size() - 1; index >= 0; index--) {
            var original = chain.get(index);
            var next = new MobEffectInstance(original);
            ((EffectDurationAccessor) next)
                    .travail$duration(duration.applyAsInt(original.getDuration()));
            ((EffectDurationAccessor) next).travail$hiddenEffect(result);
            result = next;
        }
        return result;
    }

    private EffectCopies() {}
}

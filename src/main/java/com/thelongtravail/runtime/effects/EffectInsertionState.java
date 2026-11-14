package com.thelongtravail.runtime.effects;

import com.thelongtravail.mixin.effects.EffectAttributesAccessor;
import com.thelongtravail.mixin.effects.EffectDurationAccessor;
import com.thelongtravail.mixin.effects.LivingEffectsAccessor;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;

import java.util.*;

// 只恢复本次效果、标准属性和健康状态；不能撤销外部回调的世界、掉落或其他实体副作用。
final class EffectInsertionState {
    private record Node(
            MobEffectInstance original, MobEffectInstance details, MobEffectInstance hidden) {
        void restore() {
            var access = (EffectDurationAccessor) original;
            access.travail$restoreDetails(details);
            access.travail$hiddenEffect(hidden);
        }
    }

    private record AttributeState(AttributeInstance original, AttributeInstance saved) {}

    private final LivingEntity target;
    private final MobEffect effect;
    private final Map<MobEffect, MobEffectInstance> effects;
    private final MobEffectInstance previous;
    private final List<Node> chain = new ArrayList<>();
    private final Map<Attribute, AttributeState> attributes = new LinkedHashMap<>();
    private final EffectAttributesAccessor attributeAccess;
    private final float health;
    private final float absorption;
    private final boolean marked;

    EffectInsertionState(LivingEntity target, MobEffect effect) {
        this.target = target;
        this.effect = effect;
        effects = ((LivingEffectsAccessor) target).travail$activeEffects();
        previous = effects.get(effect);
        Set<MobEffectInstance> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var node = previous; node != null; ) {
            if (!seen.add(node) || chain.size() >= EffectCopies.MAX_CHAIN_LENGTH)
                throw new IllegalArgumentException("Cannot snapshot invalid hidden effect chain");
            var hidden = ((EffectDurationAccessor) node).travail$hiddenEffect();
            chain.add(new Node(node, new MobEffectInstance(node), hidden));
            node = hidden;
        }
        attributeAccess = (EffectAttributesAccessor) target.getAttributes();
        attributeAccess
                .travail$instances()
                .forEach(
                        (key, original) -> {
                            var saved = new AttributeInstance(key, ignored -> {});
                            // NBT save/load 不包含瞬时 modifier，使用原版完整复制方法。
                            saved.replaceFrom(original);
                            attributes.put(key, new AttributeState(original, saved));
                        });
        health = target.getHealth();
        absorption = target.getAbsorptionAmount();
        marked = InsertedEffectSync.isMarked(target, effect);
    }

    void restore() {
        var failures = new ArrayList<RuntimeException>();
        for (var node : chain) attempt(failures, node::restore);
        attempt(
                failures,
                () -> {
                    if (previous == null) effects.remove(effect);
                    else effects.put(effect, previous);
                    if (effects.get(effect) != previous)
                        throw new IllegalStateException("Effect storage rejected rollback");
                    ((LivingEffectsAccessor) target).travail$effectsDirty(true);
                });
        var current = attributeAccess.travail$instances();
        for (var entry : attributes.entrySet())
            attempt(
                    failures,
                    () -> {
                        var state = entry.getValue();
                        current.put(entry.getKey(), state.original());
                        state.original().replaceFrom(state.saved());
                    });
        // 回调可能首次实例化某个默认属性；恢复默认值并保留实例，确保同步与既有引用有效。
        for (var entry : new ArrayList<>(current.entrySet())) {
            if (attributes.containsKey(entry.getKey())) continue;
            attempt(
                    failures,
                    () -> {
                        var defaults =
                                attributeAccess
                                        .travail$supplier()
                                        .createInstance(ignored -> {}, entry.getKey());
                        if (defaults == null)
                            throw new IllegalStateException(
                                    "No default for attribute created during effect callback");
                        entry.getValue().replaceFrom(defaults);
                    });
        }
        attempt(
                failures,
                () -> {
                    if (target.getHealth() != health) target.setHealth(health);
                    if (target.getAbsorptionAmount() != absorption)
                        target.setAbsorptionAmount(absorption);
                });
        // 修正已发送的中间效果状态，不调用添加/更新/移除回调。
        if (failures.isEmpty())
            attempt(failures, () -> InsertedEffectSync.restore(target, effect, previous, marked));
        if (!failures.isEmpty()) {
            var failure =
                    new IllegalStateException(
                            "Effect insertion rollback incomplete", failures.get(0));
            for (int i = 1; i < failures.size(); i++) failure.addSuppressed(failures.get(i));
            throw failure;
        }
    }

    private static void attempt(List<RuntimeException> failures, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException failure) {
            failures.add(failure);
        }
    }
}

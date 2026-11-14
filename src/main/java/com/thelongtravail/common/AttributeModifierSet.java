package com.thelongtravail.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.*;

public final class AttributeModifierSet {
    // 固定来源由调用方持有，避免不同饰品交替更新时重复计算同一个 UUID。
    public static final class Source {
        private final ResourceLocation key;
        private final UUID id;

        public Source(ResourceLocation key) {
            this.key = Objects.requireNonNull(key);
            this.id = UUID.nameUUIDFromBytes(key.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static volatile Source lastSource;

    private static Source source(ResourceLocation key) {
        Source cached = lastSource;
        if (cached != null && cached.key.equals(key)) return cached;
        Source resolved = new Source(key);
        lastSource = resolved;
        return resolved;
    }

    public record Modifier(double amount, AttributeModifier.Operation operation) {
        public Modifier {
            if (!Double.isFinite(amount)) throw new IllegalArgumentException("Non-finite modifier");
            Objects.requireNonNull(operation);
        }
    }

    public static void reconcile(
            LivingEntity entity, ResourceLocation source, double amount, Set<String> excluded) {
        reconcile(entity, source(source), amount, excluded);
    }

    public static void reconcile(
            LivingEntity entity, Source source, double amount, Set<String> excluded) {
        UUID id = source.id;
        ResourceLocation sourceKey = source.key;
        Modifier bonus =
                Double.isFinite(amount) && amount != 0
                        ? new Modifier(amount, AttributeModifier.Operation.MULTIPLY_TOTAL)
                        : null;
        boolean healthReduced = false;
        for (Attribute attribute : ForgeRegistries.ATTRIBUTES.getValues()) {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null) continue;
            Modifier wanted = null;
            if (bonus != null) {
                var key = ForgeRegistries.ATTRIBUTES.getKey(attribute);
                if (key != null && !excluded.contains(key.toString())) wanted = bonus;
            }
            healthReduced |= reconcile(instance, attribute, sourceKey, id, wanted);
        }
        if (healthReduced && entity.getHealth() > entity.getMaxHealth())
            entity.setHealth(entity.getMaxHealth());
    }

    public static void reconcile(
            LivingEntity entity, ResourceLocation source, Map<Attribute, Modifier> desired) {
        reconcile(entity, source(source), desired);
    }

    public static void reconcile(
            LivingEntity entity, Source source, Map<Attribute, Modifier> desired) {
        UUID id = source.id;
        ResourceLocation sourceKey = source.key;
        boolean healthReduced = false;
        for (Attribute attribute : ForgeRegistries.ATTRIBUTES.getValues()) {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null) continue;
            healthReduced |= reconcile(instance, attribute, sourceKey, id, desired.get(attribute));
        }
        if (healthReduced && entity.getHealth() > entity.getMaxHealth())
            entity.setHealth(entity.getMaxHealth());
    }

    private static boolean reconcile(
            AttributeInstance instance,
            Attribute attribute,
            ResourceLocation source,
            UUID id,
            Modifier wanted) {
        var old = instance.getModifier(id);
        if (wanted == null && old == null) return false;
        if (wanted != null
                && old != null
                && old.getAmount() == wanted.amount()
                && old.getOperation() == wanted.operation()) return false;
        double before = instance.getValue();
        if (old != null) instance.removeModifier(id);
        if (wanted != null)
            instance.addTransientModifier(
                    new AttributeModifier(
                            id, source.toString(), wanted.amount(), wanted.operation()));
        return attribute == Attributes.MAX_HEALTH && instance.getValue() < before;
    }

    public static void clear(LivingEntity entity, ResourceLocation source) {
        reconcile(entity, source, Map.of());
    }

    public static void clear(LivingEntity entity, Source source) {
        reconcile(entity, source, Map.of());
    }

    private AttributeModifierSet() {}
}

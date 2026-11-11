package com.thelongtravail.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.*;

public final class AttributeModifierSet {
    private record SourceId(ResourceLocation source, UUID id) {}

    private static volatile SourceId lastSource;

    private static UUID sourceId(ResourceLocation source) {
        SourceId cached = lastSource;
        if (cached != null && cached.source.equals(source)) return cached.id;
        UUID id = UUID.nameUUIDFromBytes(source.toString().getBytes(StandardCharsets.UTF_8));
        lastSource = new SourceId(source, id);
        return id;
    }

    public record Modifier(double amount, AttributeModifier.Operation operation) {
        public Modifier {
            if (!Double.isFinite(amount)) throw new IllegalArgumentException("Non-finite modifier");
            Objects.requireNonNull(operation);
        }
    }

    public static void reconcile(
            LivingEntity entity, ResourceLocation source, double amount, Set<String> excluded) {
        UUID id = sourceId(source);
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
            healthReduced |= reconcile(instance, attribute, source, id, wanted);
        }
        if (healthReduced && entity.getHealth() > entity.getMaxHealth())
            entity.setHealth(entity.getMaxHealth());
    }

    public static void reconcile(
            LivingEntity entity, ResourceLocation source, Map<Attribute, Modifier> desired) {
        UUID id = sourceId(source);
        boolean healthReduced = false;
        for (Attribute attribute : ForgeRegistries.ATTRIBUTES.getValues()) {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null) continue;
            healthReduced |= reconcile(instance, attribute, source, id, desired.get(attribute));
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

    private AttributeModifierSet() {}
}

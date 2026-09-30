package com.thelongtravail.data;

import net.minecraft.resources.ResourceLocation;

public record EffectEntry(ResourceLocation id, int maxLevel, int weight) {
    public static EffectEntry parse(String text) {
        String[] parts = text.split("\\|");
        if (parts.length < 3) return null;
        ResourceLocation id = ResourceLocation.tryParse(parts[0].trim());
        if (id == null) return null;
        try {
            int maxLevel = Integer.parseInt(parts[1].trim());
            int weight = Integer.parseInt(parts[2].trim());
            return maxLevel > 0 && weight > 0 ? new EffectEntry(id, Math.min(maxLevel, 256), weight) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}

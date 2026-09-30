package com.thelongtravail.data;

import net.minecraft.resources.ResourceLocation;

public record WeightedEntry(ResourceLocation id, String displayName, int weight) {
    public static WeightedEntry parse(String text) {
        String[] parts = text.split("\\|");
        if (parts.length < 2) return null;
        ResourceLocation id = ResourceLocation.tryParse(parts[0].trim());
        if (id == null) return null;
        try {
            String displayName = parts.length >= 3 ? parts[1].trim() : "";
            int weight = Integer.parseInt(parts[parts.length >= 3 ? 2 : 1].trim());
            return weight > 0 ? new WeightedEntry(id, displayName, weight) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}

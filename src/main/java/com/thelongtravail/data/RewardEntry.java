package com.thelongtravail.data;

import net.minecraft.resources.ResourceLocation;

public record RewardEntry(ResourceLocation id, int weight, int minCount, int maxCount) {
    public static RewardEntry parse(String text) {
        String[] parts = text.split("\\|");
        if (parts.length < 4) return null;
        ResourceLocation id = ResourceLocation.tryParse(parts[0].trim());
        if (id == null) return null;
        try {
            int weight = Integer.parseInt(parts[1].trim());
            int min = Integer.parseInt(parts[2].trim());
            int max = Integer.parseInt(parts[3].trim());
            if (weight <= 0 || min <= 0 || max < min) return null;
            if (max > 4096) com.thelongtravail.TheLongTravail.LOGGER.warn("Capping reward count at 4096: {}", text);
            return new RewardEntry(id, weight, Math.min(min, 4096), Math.min(max, 4096));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}

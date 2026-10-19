package com.thelongtravail.common;

import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;

public final class TagItemSampler {
    public static ItemStack choose(TagKey<Item> tag, RandomSource random, int count) {
        return choose(tag, java.util.List.of(), random, count);
    }

    public static ItemStack choose(
            TagKey<Item> tag, java.util.Collection<? extends String> extraIds, RandomSource random, int count) {
        if (count <= 0) return ItemStack.EMPTY;
        java.util.Set<Item> candidates = new java.util.LinkedHashSet<>();
        var manager = ForgeRegistries.ITEMS.tags();
        if (manager != null) manager.getTag(tag).forEach(candidates::add);
        for (String value : extraIds) {
            var id = net.minecraft.resources.ResourceLocation.tryParse(value);
            if (id != null && ForgeRegistries.ITEMS.containsKey(id)) candidates.add(ForgeRegistries.ITEMS.getValue(id));
        }
        candidates.remove(null);
        candidates.remove(Items.AIR);
        var items = new java.util.ArrayList<>(candidates);
        return items.isEmpty() ? ItemStack.EMPTY : new ItemStack(items.get(random.nextInt(items.size())), count);
    }

    private TagItemSampler() {}
}

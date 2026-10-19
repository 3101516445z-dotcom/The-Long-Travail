package com.thelongtravail.flourishing;

import com.thelongtravail.config.FlourishingItemsConfig;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class FlowerData {
    private static final String KEY = "TravailFlowers";

    public static List<Flower> read(ItemStack stack) {
        List<Flower> result = new ArrayList<>();
        if (stack.hasTag())
            for (Tag tag : stack.getTag().getList(KEY, Tag.TAG_STRING)) {
                Flower f = Flower.parse(tag.getAsString());
                if (f != null && !result.contains(f) && result.size() < 3) result.add(f);
            }
        return result;
    }

    public static void write(ItemStack stack, List<Flower> flowers) {
        ListTag list = new ListTag();
        flowers.stream().distinct().limit(3).forEach(f -> list.add(StringTag.valueOf(f.id)));
        stack.getOrCreateTag().put(KEY, list);
    }

    public static boolean active(ItemStack stack, Flower f) {
        if (!FlourishingItemsConfig.ENABLED.get(f).get() || !stack.hasTag()) {
            return false;
        }
        long seen = 0;
        int count = 0;
        for (Tag tag : stack.getTag().getList(KEY, Tag.TAG_STRING)) {
            Flower flower = Flower.parse(tag.getAsString());
            if (flower == null) continue;
            long bit = 1L << flower.ordinal();
            if ((seen & bit) != 0) continue;
            if (flower == f) return true;
            seen |= bit;
            if (++count == 3) break;
        }
        return false;
    }

    private FlowerData() {}
}

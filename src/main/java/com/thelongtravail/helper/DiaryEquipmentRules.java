package com.thelongtravail.helper;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.data.LongTravailData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class DiaryEquipmentRules {
    public static boolean canEquip(String slotIdentifier) {
        return "travel_diary".equals(slotIdentifier);
    }

    public static boolean canUnequip(LivingEntity wearer, ItemStack diary) {
        if (!(wearer instanceof Player player)) return false;
        return player.isCreative() || hasAllWitnesses(diary);
    }

    public static boolean hasAllWitnesses(ItemStack diary) {
        if (diary.isEmpty()) return false;

        for (TravailAspect aspect : TravailAspect.values())
            if (!LongTravailData.hasWitness(diary, aspect)) return false;
        return true;
    }

    private DiaryEquipmentRules() {}
}

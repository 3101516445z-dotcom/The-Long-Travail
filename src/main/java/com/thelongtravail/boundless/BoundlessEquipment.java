package com.thelongtravail.boundless;

import com.thelongtravail.equipment.CurioLookup;
import com.thelongtravail.equipment.AccessoryEquipRules;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.world.entity.player.Player;

public final class BoundlessEquipment {
    public static boolean equipped(Player p, boolean dream) {
        if (!AccessoryEquipRules.hasDiary(p)) return false;
        return CurioLookup.hasFunctional(
                p, dream ? ModRegistry.FREE_AND_EASY_WANDERING : ModRegistry.STAR_VOICE);
    }

    private BoundlessEquipment() {}
}

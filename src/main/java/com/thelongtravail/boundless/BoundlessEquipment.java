package com.thelongtravail.boundless;

import com.thelongtravail.helper.CurioLookup;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.world.entity.player.Player;

public final class BoundlessEquipment {
    public static boolean equipped(Player p, boolean dream) {
        if (!dream && TravailCurios.stack(p).isEmpty()) return false;
        return CurioLookup.hasFunctional(
                p,
                dream ? ModRegistry.FREE_AND_EASY_WANDERING.get() : ModRegistry.STAR_VOICE.get(),
                context -> dream || context.identifier().equals("curio"));
    }

    private BoundlessEquipment() {}
}

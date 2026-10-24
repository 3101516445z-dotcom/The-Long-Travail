package com.thelongtravail.farreach;

import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.helper.CurioLookup;
import net.minecraft.world.entity.player.Player;

public final class FarReachEquipment {
    public static boolean equipped(Player player, boolean gold) {
        return player.isAlive()
                && !player.isSpectator()
                && CurioLookup.hasFunctional(
                        player,
                        gold ? ModRegistry.GOLDEN_AGE.get() : ModRegistry.ICARUS.get(),
                        context -> context.identifier().equals(gold ? "charm" : "back"));
    }

    private FarReachEquipment() {}
}

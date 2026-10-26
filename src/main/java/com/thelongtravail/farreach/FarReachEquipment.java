package com.thelongtravail.farreach;

import com.thelongtravail.helper.CurioLookup;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.world.entity.player.Player;

public final class FarReachEquipment {
    public static boolean equipped(Player player, boolean gold) {
        if (gold)
            return player instanceof net.minecraft.server.level.ServerPlayer p
                    && com.thelongtravail.api.CarryingApi.isCarrying(
                            p, ModRegistry.GOLDEN_AGE.get());
        return player.isAlive()
                && !player.isSpectator()
                && CurioLookup.hasFunctional(
                        player,
                        ModRegistry.ICARUS.get(),
                        context -> context.identifier().equals("back"));
    }

    private FarReachEquipment() {}
}

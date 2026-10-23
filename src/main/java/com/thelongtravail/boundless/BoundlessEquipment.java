package com.thelongtravail.boundless;

import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.helper.TravailCurios;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;

public final class BoundlessEquipment {
    public static boolean equipped(Player p, boolean dream) {
        if (!dream && TravailCurios.stack(p).isEmpty()) return false;
        return CuriosApi.getCuriosInventory(p)
                .map(
                        h ->
                                h
                                        .findCurios(
                                                dream
                                                        ? ModRegistry.FREE_AND_EASY_WANDERING.get()
                                                        : ModRegistry.STAR_VOICE.get())
                                        .stream()
                                        .anyMatch(
                                                r ->
                                                        !r.slotContext().cosmetic()
                                                                && (dream
                                                                        || r.slotContext()
                                                                                .identifier()
                                                                                .equals(
                                                                                        "necklace"))))
                .orElse(false);
    }

    private BoundlessEquipment() {}
}

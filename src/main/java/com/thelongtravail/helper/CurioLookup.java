package com.thelongtravail.helper;

import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

/** Live equipment lookup only; prerequisites and restoration policy belong to callers. */
public final class CurioLookup {
    public static boolean hasFunctional(Player player, Item item, Predicate<SlotContext> slot) {
        return CuriosApi.getCuriosInventory(player)
                .map(inv -> inv.findCurios(item).stream()
                        .anyMatch(result -> !result.slotContext().cosmetic()
                                && slot.test(result.slotContext())))
                .orElse(false);
    }

    private CurioLookup() {}
}

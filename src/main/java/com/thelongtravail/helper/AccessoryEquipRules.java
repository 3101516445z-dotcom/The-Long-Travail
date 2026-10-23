package com.thelongtravail.helper;

import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

public final class AccessoryEquipRules {
    public static boolean hasEquipped(Player player, String slot, Item item) {
        var scope = AccessoryRestore.current(player);
        if (scope != null) return scope.contains(slot, item);
        return CuriosApi.getCuriosInventory(player)
                .map(
                        inv ->
                                inv.findCurios(item).stream()
                                        .anyMatch(
                                                r ->
                                                        !r.slotContext().cosmetic()
                                                                && r.slotContext()
                                                                        .identifier()
                                                                        .equals(slot)))
                .orElse(false);
    }

    public static boolean hasDiary(Player player) {
        return hasEquipped(player, "travel_diary", ModRegistry.LONG_TRAVAIL.get());
    }

    public static boolean unique(SlotContext context, Item item) {
        return unique(context, item, false);
    }

    public static boolean unique(SlotContext context, Item item, boolean includeCosmetics) {
        if (!(context.entity() instanceof Player player)) return false;
        var scope = AccessoryRestore.current(player);
        if (scope != null) return scope.unique(context, item);
        return CuriosApi.getCuriosInventory(player)
                .map(
                        inv ->
                                inv.findCurios(item).stream()
                                        .filter(
                                                r ->
                                                        includeCosmetics
                                                                || !r.slotContext().cosmetic())
                                        .noneMatch(
                                                r ->
                                                        !r.slotContext()
                                                                        .identifier()
                                                                        .equals(
                                                                                context
                                                                                        .identifier())
                                                                || r.slotContext().index()
                                                                        != context.index()))
                .orElse(false);
    }

    private AccessoryEquipRules() {}
}

package com.thelongtravail.equipment;

import net.minecraftforge.registries.RegistryObject;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

public final class AccessoryEquipRules {
    public static boolean hasEquipped(
            Player player,
            RegistryObject<Item> item) {
        return item.isPresent() && hasEquipped(player, item.get());
    }

    public static boolean hasEquipped(
            Player player,
            String slot,
            RegistryObject<Item> item) {
        return item.isPresent() && hasEquipped(player, slot, item.get());
    }

    public static boolean hasEquipped(Player player, Item item) {
        var scope = AccessoryRestore.current(player);
        if (scope != null) return scope.contains(item);
        return CurioLookup.hasFunctional(player, item);
    }

    public static boolean hasEquipped(Player player, String slot, Item item) {
        var scope = AccessoryRestore.current(player);
        if (scope != null) return scope.contains(slot, item) || scope.contains("curio", item);
        return CurioLookup.hasFunctional(player, item, context -> matchesSlot(context, slot));
    }

    public static boolean matchesSlot(SlotContext context, String declared) {
        return !context.cosmetic()
                && (context.identifier().equals(declared) || context.identifier().equals("curio"));
    }

    public static boolean hasDiary(Player player) {
        var item = ModRegistry.LONG_TRAVAIL.get();
        var scope = AccessoryRestore.current(player);
        return scope != null
                ? scope.contains("travel_diary", item)
                : CurioLookup.hasFunctional(
                        player, item, context -> context.identifier().equals("travel_diary"));
    }

    public static boolean canEquipUnique(SlotContext context, Item item) {
        return !context.cosmetic() && unique(context, item);
    }

    public static boolean canEquipUnique(SlotContext context, Item item, String declared) {
        return matchesSlot(context, declared) && unique(context, item);
    }

    public static boolean canEquipWithDiary(SlotContext context, Item item) {
        return !context.cosmetic()
                && context.entity() instanceof Player player
                && hasDiary(player)
                && unique(context, item);
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

package com.thelongtravail.equipment;

import net.minecraftforge.registries.RegistryObject;
import java.util.Optional;

import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;

// 仅查询当前装备；佩戴资格和恢复规则由调用方判断。
public final class CurioLookup {
    public static boolean hasFunctional(
            Player player,
            RegistryObject<Item> item) {
        return item.isPresent() && hasFunctional(player, item.get());
    }

    public static boolean hasFunctional(
            Player player,
            RegistryObject<Item> item,
            Predicate<SlotContext> slot) {
        return item.isPresent() && hasFunctional(player, item.get(), slot);
    }

    public static boolean hasFunctional(Player player, Item item) {
        return findFunctional(player, item).isPresent();
    }

    public static boolean hasFunctional(Player player, Item item, Predicate<SlotContext> slot) {
        return findFunctional(player, item, result -> slot.test(result.slotContext())).isPresent();
    }

    public static Optional<SlotResult> findFunctional(Player player, Item item) {
        return CuriosApi.getCuriosInventory(player)
                .resolve()
                .flatMap(
                        inventory -> {
                            var first = inventory.findFirstCurio(item);
                            if (first.isEmpty() || !first.get().slotContext().cosmetic())
                                return first;
                            return inventory.findCurios(item).stream()
                                    .filter(result -> !result.slotContext().cosmetic())
                                    .findFirst();
                        });
    }

    public static Optional<SlotResult> findFunctional(
            Player player, Item item, Predicate<SlotResult> filter) {
        return CuriosApi.getCuriosInventory(player)
                .resolve()
                .flatMap(
                        inventory ->
                                inventory.findCurios(item).stream()
                                        .filter(
                                                result ->
                                                        !result.slotContext().cosmetic()
                                                                && filter.test(result))
                                        .findFirst());
    }

    private CurioLookup() {}
}

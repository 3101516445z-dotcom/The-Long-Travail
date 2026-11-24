package com.thelongtravail.valley.lantern;

import com.thelongtravail.equipment.AccessoryEquipRules;

import java.util.function.Predicate;
import com.thelongtravail.equipment.CurioLookup;

import com.thelongtravail.registry.RegisteredItems;

import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class LanternSources {
    private static final String DISABLED = "LanternLightDisabled";

    public static boolean enabled(ItemStack stack) {
        return RegisteredItems.is(stack, ModRegistry.SWORD_AND_LANTERN)
                && (!stack.hasTag() || !stack.getTag().getBoolean(DISABLED));
    }

    public static boolean toggle(ItemStack stack) {
        boolean next = !enabled(stack);
        if (next) {
            if (stack.hasTag()) stack.getTag().remove(DISABLED);
        } else stack.getOrCreateTag().putBoolean(DISABLED, true);
        return next;
    }

    public static boolean equipped(Player player) {
        return hasEquippedLantern(player, LanternSources::enabled);
    }

    static boolean hasEquippedLantern(Player player, Predicate<ItemStack> matches) {
        if (!player.isAlive() || player.isSpectator()) return false;
        return ModRegistry.SWORD_AND_LANTERN.isPresent()
                && CurioLookup.findFunctional(
                                player,
                                ModRegistry.SWORD_AND_LANTERN.get(),
                                result ->
                                        AccessoryEquipRules.matchesSlot(
                                                        result.slotContext(), "belt")
                                                && matches.test(result.stack()))
                        .isPresent();
    }

    public static boolean held(Player player) {
        return player.isAlive()
                && !player.isSpectator()
                && (enabled(player.getMainHandItem()) || enabled(player.getOffhandItem()));
    }

    public static boolean dropped(ItemEntity item) {
        return item.isAlive() && !item.isRemoved() && enabled(item.getItem());
    }

    private LanternSources() {}
}

package com.thelongtravail.farreach.icarus;

import com.thelongtravail.equipment.CurioLookup;

import com.thelongtravail.equipment.AccessoryEquipRules;

import com.thelongtravail.registry.RegisteredItems;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class IcarusFlight {
    private static final String DISABLED = "IcarusFlightDisabled";

    private IcarusFlight() {}

    public static boolean enabled(ItemStack stack) {
        return RegisteredItems.is(stack, ModRegistry.ICARUS)
                && (!stack.hasTag() || !stack.getTag().getBoolean(DISABLED));
    }

    public static boolean toggle(ItemStack stack) {
        if (!RegisteredItems.is(stack, ModRegistry.ICARUS)) return false;
        boolean next = !enabled(stack);
        if (next) {
            if (stack.hasTag()) stack.getTag().remove(DISABLED);
        } else stack.getOrCreateTag().putBoolean(DISABLED, true);
        return next;
    }

    public static boolean equipped(LivingEntity entity) {
        return !enabledStack(entity, false).isEmpty();
    }

    public static boolean allowed(LivingEntity entity) {
        return !com.thelongtravail.compat.caelus.IcarusCaelusCompat.denied(entity);
    }

    public static ItemStack visibleStack(LivingEntity entity) {
        return enabledStack(entity, true);
    }

    private static ItemStack enabledStack(LivingEntity entity, boolean visibleOnly) {
        if (!ModRegistry.ICARUS.isPresent()
                || !(entity instanceof Player player)
                || !player.isAlive()
                || player.isSpectator()
                || (visibleOnly && player.isInvisible())) return ItemStack.EMPTY;
        return CurioLookup.findFunctional(
                        player,
                        ModRegistry.ICARUS.get(),
                        r ->
                                AccessoryEquipRules.matchesSlot(r.slotContext(), "back")
                                        && (!visibleOnly || r.slotContext().visible())
                                        && enabled(r.stack()))
                .map(r -> r.stack())
                .orElse(ItemStack.EMPTY);
    }
}

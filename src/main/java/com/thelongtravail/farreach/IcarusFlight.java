package com.thelongtravail.farreach;

import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

public final class IcarusFlight {
    private static final String DISABLED = "IcarusFlightDisabled";

    private IcarusFlight() {}

    public static boolean enabled(ItemStack stack) {
        return stack.is(ModRegistry.ICARUS.get())
                && (!stack.hasTag() || !stack.getTag().getBoolean(DISABLED));
    }

    public static boolean toggle(ItemStack stack) {
        if (!stack.is(ModRegistry.ICARUS.get())) return false;
        boolean next = !enabled(stack);
        if (next) {
            if (stack.hasTag()) stack.getTag().remove(DISABLED);
        } else stack.getOrCreateTag().putBoolean(DISABLED, true);
        return next;
    }

    public static boolean equipped(LivingEntity entity) {
        return entity instanceof Player player
                && player.isAlive()
                && !player.isSpectator()
                && CuriosApi.getCuriosInventory(player)
                        .map(
                                inv ->
                                        inv.findCurios(ModRegistry.ICARUS.get()).stream()
                                                .anyMatch(
                                                        r ->
                                                                !r.slotContext().cosmetic()
                                                                        && r.slotContext()
                                                                                .identifier()
                                                                                .equals("back")
                                                                        && enabled(r.stack())))
                        .orElse(false);
    }

    public static boolean allowed(LivingEntity entity) {
        return !com.thelongtravail.compat.caelus.IcarusCaelusCompat.denied(entity);
    }

    public static ItemStack visibleStack(LivingEntity entity) {
        if (!(entity instanceof Player player) || !equipped(player) || player.isInvisible())
            return ItemStack.EMPTY;
        return CuriosApi.getCuriosInventory(player)
                .map(
                        inv ->
                                inv.findCurios(ModRegistry.ICARUS.get()).stream()
                                        .filter(
                                                r ->
                                                        !r.slotContext().cosmetic()
                                                                && r.slotContext()
                                                                        .identifier()
                                                                        .equals("back")
                                                                && r.slotContext().visible()
                                                                && enabled(r.stack()))
                                        .map(r -> r.stack())
                                        .findFirst()
                                        .orElse(ItemStack.EMPTY))
                .orElse(ItemStack.EMPTY);
    }
}

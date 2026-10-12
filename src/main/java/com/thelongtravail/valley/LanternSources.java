package com.thelongtravail.valley;

import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

// 手持与掉落状态只提供视觉照明，不赋予饰品的战斗能力。
public final class LanternSources {
    private static final String DISABLED = "LanternLightDisabled";

    public static boolean enabled(ItemStack stack) {
        return stack.is(ModRegistry.SWORD_AND_LANTERN.get())
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
        return player.isAlive()
                && !player.isSpectator()
                && top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                        .map(
                                inv ->
                                        inv.findCurios(ModRegistry.SWORD_AND_LANTERN.get()).stream()
                                                .anyMatch(
                                                        r ->
                                                                !r.slotContext().cosmetic()
                                                                        && r.slotContext()
                                                                                .identifier()
                                                                                .equals("belt")
                                                                        && enabled(r.stack())))
                        .orElse(false);
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

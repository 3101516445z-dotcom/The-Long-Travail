package com.thelongtravail.compat.fluid;

import com.thelongtravail.abyss.PerfectFluidRecipe;
import com.thelongtravail.mixin.FluidCraftingGridAccessor;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Supplier;

public final class FluidCrafting {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    public static boolean supported(CraftingContainer grid) {
        if (!(grid instanceof FluidCraftingGridAccessor accessor)) return false;
        var menu = accessor.travail$menu();
        return menu != null
                && (menu.getClass() == InventoryMenu.class
                        || menu.getClass() == CraftingMenu.class);
    }

    public static <T> T take(
            AbstractContainerMenu menu, Player player, Supplier<T> action, T rejected) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || ACTIVE.get()
                || (menu.getClass() != InventoryMenu.class
                        && menu.getClass() != CraftingMenu.class)) return action.get();
        if (!(menu.getSlot(1).container instanceof CraftingContainer grid)) return action.get();
        ItemStack source = ItemStack.EMPTY;
        int fluidSlot = -1;
        // 这里只捕获预检异常，普通配方执行时的异常仍向外传递。
        boolean external;
        try {
            var recipe =
                    player.level()
                            .getRecipeManager()
                            .getRecipeFor(RecipeType.CRAFTING, grid, player.level());
            if (recipe.isEmpty() && !menu.getSlot(0).getItem().isEmpty()) {
                for (int i = 0; i < grid.getContainerSize(); i++)
                    if (grid.getItem(i).is(ModRegistry.PERFECT_FLUID.get()))
                        throw new IllegalStateException(
                                "Perfect Fluid result no longer has a matching recipe");
            }
            external =
                    recipe.isPresent()
                            && recipe.get() instanceof PerfectFluidRecipe
                            && !menu.getSlot(0).getItem().isEmpty();
            if (external) {
                for (int i = 0; i < grid.getContainerSize(); i++) {
                    ItemStack stack = grid.getItem(i);
                    if (stack.is(ModRegistry.PERFECT_FLUID.get())) fluidSlot = i;
                    else if (!stack.isEmpty()) source = stack;
                }
                external =
                        !source.isEmpty()
                                && fluidSlot >= 0
                                && FluidCopyGraph.hasReferences(
                                        source.save(new net.minecraft.nbt.CompoundTag()));
            }
        } catch (RuntimeException failure) {
            return rejected(menu, player, rejected, "identification", failure);
        }
        if (!external) return action.get();
        int before = grid.getItem(fluidSlot).getCount();
        List<ItemStack> slots = null;
        List<ItemStack> inventory = new ArrayList<>();
        ItemStack carried = ItemStack.EMPTY;
        FluidCopyGraph.Prepared prepared = null;
        boolean menuChanged = false;
        String stage = "snapshot";
        ACTIVE.set(true);
        try {
            slots = menu.slots.stream().map(s -> s.getItem().copy()).toList();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++)
                inventory.add(player.getInventory().getItem(i).copy());
            carried = menu.getCarried().copy();
            stage = "preparation";
            prepared = FluidCopyGraph.prepare(serverPlayer.serverLevel(), source);
            stage = "commit";
            prepared.commit();
            stage = "collection";
            menuChanged = true;
            menu.getSlot(0).set(prepared.output());
            T result = action.get();
            if (grid.getItem(fluidSlot).getCount() == before) {
                prepared.rollback();
                menu.getSlot(0).set(slots.get(0));
            }
            return result;
        } catch (Exception failure) {
            if (prepared != null)
                try {
                    prepared.rollback();
                } catch (Exception rollback) {
                    failure.addSuppressed(rollback);
                }
            if (menuChanged) {
                // 第三方槽位回调失败时，仍须继续恢复其他槽位。
                for (int i = 1; i < slots.size(); i++)
                    try {
                        menu.getSlot(i).set(slots.get(i));
                    } catch (RuntimeException restore) {
                        failure.addSuppressed(restore);
                    }
                for (int i = 0; i < inventory.size(); i++)
                    try {
                        player.getInventory().setItem(i, inventory.get(i));
                    } catch (RuntimeException restore) {
                        failure.addSuppressed(restore);
                    }
                // 恢复材料会刷新预览及预留的唯一标识，因此最后恢复输出槽。
                try {
                    menu.getSlot(0).set(slots.get(0));
                } catch (RuntimeException restore) {
                    failure.addSuppressed(restore);
                }
                try {
                    menu.setCarried(carried);
                } catch (RuntimeException restore) {
                    failure.addSuppressed(restore);
                }
            }
            return rejected(menu, player, rejected, stage, failure);
        } finally {
            ACTIVE.remove();
        }
    }

    private static <T> T rejected(
            AbstractContainerMenu menu, Player player, T result, String stage, Exception failure) {
        try {
            player.displayClientMessage(
                    Component.translatable("message.the_long_travail.perfect_fluid.copy_failed"),
                    false);
        } catch (RuntimeException notification) {
            failure.addSuppressed(notification);
        }
        try {
            menu.broadcastChanges();
        } catch (RuntimeException notification) {
            failure.addSuppressed(notification);
        }
        FluidCopyFailures.report(stage, failure);
        return result;
    }

    private FluidCrafting() {}
}

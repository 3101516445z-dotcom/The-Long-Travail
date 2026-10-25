package com.thelongtravail.menu;

import com.thelongtravail.item.NameColoring;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ColoringMenu extends AbstractContainerMenu {
    private final Player owner;
    private final DataSlot selected = DataSlot.standalone();
    private final SimpleContainer result = new SimpleContainer(1);
    private final SimpleContainer input =
            new SimpleContainer(1) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    updateResult();
                }
            };

    public ColoringMenu(int id, Inventory inventory) {
        super(ModRegistry.COLORING_MENU.get(), id);
        owner = inventory.player;
        selected.set(-1);
        addDataSlot(selected);
        addSlot(new Slot(input, 0, 81, 47));
        addSlot(
                new Slot(result, 0, 187, 47) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }
                });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 62 + col * 18, 163 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 62 + col * 18, 221));
    }

    public int selection() {
        return selected.get();
    }

    private void updateResult() {
        if (owner == null || owner.level().isClientSide) return;
        ItemStack preview = ItemStack.EMPTY;
        if (NameColoring.valid(selected.get()) && !input.isEmpty()) {
            preview = input.getItem(0).copy();
            NameColoring.apply(preview, selected.get());
        }
        result.setItem(0, preview);
    }

    @Override
    public boolean clickMenuButton(Player player, int choice) {
        if (player != owner || player.containerMenu != this || !NameColoring.valid(choice))
            return false;
        if (!player.level().isClientSide) {
            selected.set(choice);
            updateResult();
            broadcastChanges();
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && player.isAlive();
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (slot != 1) {
            super.clicked(slot, button, type, player);
            return;
        }
        // 输出槽仅展示预览，实际领取由服务端提交。
        if (player != owner || player.level().isClientSide || result.isEmpty()) return;
        if (type == ClickType.QUICK_MOVE) {
            quickMoveStack(player, 1);
        } else if (type == ClickType.PICKUP && (button == 0 || button == 1)) {
            ItemStack output = result.getItem(0);
            ItemStack cursor = getCarried();
            if (!cursor.isEmpty() && !ItemStack.isSameItemSameTags(cursor, output)) return;
            int room = output.getMaxStackSize() - (cursor.isEmpty() ? 0 : cursor.getCount());
            int amount = Math.min(room, button == 1 ? 1 : output.getCount());
            if (amount <= 0) return;
            if (cursor.isEmpty()) setCarried(output.copyWithCount(amount));
            else cursor.grow(amount);
            input.removeItem(0, amount);
        }
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (player != owner || player.level().isClientSide || index < 0 || index >= slots.size())
            return ItemStack.EMPTY;
        if (index == 1) {
            ItemStack moved = result.getItem(0).copy();
            int before = moved.getCount();
            if (!moveItemStackTo(moved, 2, slots.size(), true)) return ItemStack.EMPTY;
            input.removeItem(0, before - moved.getCount());
            broadcastChanges();
            return ItemStack.EMPTY; // 阻止原版快速移动循环继续领取重新生成的预览。
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (!(index == 0
                ? moveItemStackTo(stack, 2, slots.size(), true)
                : moveItemStackTo(stack, 0, 1, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            result.clearContent();
            clearContainer(player, input);
        }
    }
}

package com.thelongtravail.helper;

import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;
import java.util.UUID;
import java.util.function.IntConsumer;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

public final class TravailCurios {
    private static final UUID EXTRA_CURIO_SLOTS = UUID.fromString("b241372a-b39e-479d-b497-f46f92ee55fa");

    // 槽位加成由单一所有者管理；普通 NBT 刷新不能撤销已占用的槽位。
    public static void syncExtraSlots(ServerPlayer player, int desired) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> handler.getStacksHandler("curio").ifPresent(slots -> {
            reconcileExtraSlots(slots, desired, count -> evacuateAddedCurioSlots(player, count));
        }));
    }

    public static void reconcileExtraSlots(ICurioStacksHandler slots, int desired, IntConsumer evacuate) {
            var current = slots.getModifiers().get(EXTRA_CURIO_SLOTS);
            // 迁移旧临时修饰符，避免下一刻被缓存的移除操作撤销。
            slots.getCachedModifiers().removeIf(modifier -> EXTRA_CURIO_SLOTS.equals(modifier.getId()));
            int applied = current == null ? 0 : Math.max(0, (int) current.getAmount());
            if (current != null && current.getAmount() == desired
                    && current.getOperation() == AttributeModifier.Operation.ADDITION) {
                slots.getPermanentModifiers().add(current);
                return;
            }
            if (applied > desired) evacuate.accept(applied - desired);
            if (current != null) slots.removeModifier(EXTRA_CURIO_SLOTS);
            if (desired > 0) slots.addPermanentModifier(new AttributeModifier(EXTRA_CURIO_SLOTS,
                    "Long Travail extra slots", desired, AttributeModifier.Operation.ADDITION));
    }

    public static Optional<SlotResult> find(Player player) {
        return CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(handler -> handler.findFirstCurio(ModRegistry.LONG_TRAVAIL.get()));
    }

    public static ItemStack stack(Player player) {
        return find(player).map(SlotResult::stack).orElse(ItemStack.EMPTY);
    }

    public static void evacuateAddedCurioSlots(ServerPlayer player, int addedSlots) {
        if (addedSlots <= 0) return;
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> handler.getStacksHandler("curio").ifPresent(stacksHandler -> {
            var stacks = stacksHandler.getStacks();
            int firstAddedSlot = Math.max(0, stacks.getSlots() - addedSlots);
            for (int slot = firstAddedSlot; slot < stacks.getSlots(); slot++) {
                ItemStack contained = stacks.getStackInSlot(slot);
                if (contained.isEmpty()) continue;
                ItemStack moving = contained.copy();
                stacks.setStackInSlot(slot, ItemStack.EMPTY);
                returnToInventoryOrDrop(player, moving);
            }
        }));
    }

    // Inventory.add 在创造模式下可能静默丢弃溢出物品，返还装备时不能使用。
    public static void returnToInventoryOrDrop(Player player, ItemStack stack) {
        ItemStack remainder = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(
                new net.minecraftforge.items.wrapper.PlayerMainInvWrapper(player.getInventory()), stack, false);
        if (!remainder.isEmpty()) player.drop(remainder, false);
    }

    private TravailCurios() {}
}

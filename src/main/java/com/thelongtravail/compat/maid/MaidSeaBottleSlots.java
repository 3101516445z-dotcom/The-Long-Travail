package com.thelongtravail.compat.maid;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.thelongtravail.abyss.SeaBottle;
import com.thelongtravail.config.ThemeItemConfig;
import com.thelongtravail.equipment.TravailCurios;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

// 装备操作结束后校正 Curios 栏位；女仆原生饰品容器保持原容量。
public final class MaidSeaBottleSlots {
    private static final Set<EntityMaid> PENDING = Collections.newSetFromMap(new WeakHashMap<>());

    public static void changed(EntityMaid maid) {
        if (maid.level().isClientSide || !PENDING.add(maid)) return;
        MaidRuntimeState.defer(
                () -> {
                    PENDING.remove(maid);
                    if (!maid.isRemoved() && maid.isAlive()) reconcile(maid);
                });
    }

    public static void reconcile(EntityMaid maid) {
        if (maid.level().isClientSide) return;
        int desired =
                MaidAccessoryRules.has(maid, ModRegistry.SEA_IN_A_BOTTLE)
                        ? (int) ThemeItemConfig.get("sea_in_a_bottle.extraSlots")
                        : 0;
        TravailCurios.syncExtraSlots(
                maid, desired, SeaBottle.SLOTS, count -> evacuate(maid, count));
    }

    private record Removed(ItemStack stack, top.theillusivec4.curios.api.SlotContext context) {}

    private static void evacuate(EntityMaid maid, int count) {
        CuriosApi.getCuriosInventory(maid)
                .ifPresent(
                        handler ->
                                handler.getStacksHandler("curio")
                                        .ifPresent(
                                                slots -> {
                                                    var removed =
                                                            new java.util.ArrayList<Removed>();
                                                    // 先收集并清空功能槽与外观槽，再执行可能改变栏位修饰符的回调。
                                                    for (var stacks :
                                                            java.util.List.of(
                                                                    slots.getStacks(),
                                                                    slots.getCosmeticStacks())) {
                                                        boolean cosmetic =
                                                                stacks == slots.getCosmeticStacks();
                                                        int end = stacks.getSlots();
                                                        for (int i = Math.max(0, end - count);
                                                                i < end;
                                                                i++) {
                                                            ItemStack moving =
                                                                    stacks.getStackInSlot(i).copy();
                                                            if (moving.isEmpty()) continue;
                                                            var context =
                                                                    new top.theillusivec4.curios.api
                                                                            .SlotContext(
                                                                            "curio",
                                                                            maid,
                                                                            i,
                                                                            cosmetic,
                                                                            slots.getRenders()
                                                                                                    .size()
                                                                                            > i
                                                                                    && slots.getRenders()
                                                                                            .get(
                                                                                                    i));
                                                            removed.add(
                                                                    new Removed(moving, context));
                                                            stacks.setStackInSlot(
                                                                    i, ItemStack.EMPTY);
                                                            stacks.setPreviousStackInSlot(
                                                                    i, ItemStack.EMPTY);
                                                        }
                                                    }
                                                    for (var entry : removed) {
                                                        var moving = entry.stack();
                                                        var context = entry.context();
                                                        if (!context.cosmetic()) {
                                                            var modifiers =
                                                                    CuriosApi.getAttributeModifiers(
                                                                            context,
                                                                            CuriosApi.getSlotUuid(
                                                                                    context),
                                                                            moving);
                                                            var slotModifiers =
                                                                    com.google.common.collect
                                                                            .HashMultimap
                                                                            .<String,
                                                                                    net.minecraft
                                                                                            .world
                                                                                            .entity
                                                                                            .ai
                                                                                            .attributes
                                                                                            .AttributeModifier>
                                                                                    create();
                                                            for (var modifier :
                                                                    modifiers.entries()) {
                                                                if (modifier.getKey()
                                                                        instanceof
                                                                        top.theillusivec4.curios.api
                                                                                                .SlotAttribute
                                                                                        slotAttribute)
                                                                    slotModifiers.put(
                                                                            slotAttribute
                                                                                    .getIdentifier(),
                                                                            modifier.getValue());
                                                            }
                                                            maid.getAttributes()
                                                                    .removeAttributeModifiers(
                                                                            modifiers);
                                                            handler.removeSlotModifiers(
                                                                    slotModifiers);
                                                            CuriosApi.getCurio(moving)
                                                                    .ifPresent(
                                                                            curio ->
                                                                                    curio.onUnequip(
                                                                                            context,
                                                                                            ItemStack
                                                                                                    .EMPTY));
                                                        }
                                                        ItemStack remainder =
                                                                ItemHandlerHelper.insertItemStacked(
                                                                        maid.getMaidInv(),
                                                                        moving,
                                                                        false);
                                                        if (!remainder.isEmpty())
                                                            maid.spawnAtLocation(remainder);
                                                    }
                                                }));
    }

    static void clear() {
        PENDING.clear();
    }

    private MaidSeaBottleSlots() {}
}

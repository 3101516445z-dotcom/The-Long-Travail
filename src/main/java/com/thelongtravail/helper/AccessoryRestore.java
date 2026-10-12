package com.thelongtravail.helper;

import net.minecraft.nbt.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import top.theillusivec4.curios.api.SlotContext;
import java.util.*;

// 在 Curios 提交新槽位表之前，临时提供待恢复背包的查询视图。
public final class AccessoryRestore implements AutoCloseable {
    private static final ThreadLocal<AccessoryRestore> CURRENT = new ThreadLocal<>();

    private record Position(String slot, int index, boolean cosmetic) {}

    private final AccessoryRestore previous;
    private final LivingEntity wearer;
    private final Map<String, Set<Item>> equipped = new HashMap<>();
    private final Map<Item, Position> accepted = new IdentityHashMap<>();

    public AccessoryRestore(LivingEntity wearer, Tag raw) {
        this.wearer = wearer;
        previous = CURRENT.get();
        if (raw instanceof CompoundTag root)
            for (Tag entry : root.getList("Curios", Tag.TAG_COMPOUND)) {
                CompoundTag slot = (CompoundTag) entry;
                for (Tag item :
                        slot.getCompound("StacksHandler")
                                .getCompound("Stacks")
                                .getList("Items", Tag.TAG_COMPOUND)) {
                    ItemStack stack = ItemStack.of((CompoundTag) item);
                    if (!stack.isEmpty())
                        equipped.computeIfAbsent(
                                        slot.getString("Identifier"), key -> new HashSet<>())
                                .add(stack.getItem());
                }
            }
        CURRENT.set(this);
    }

    static AccessoryRestore current(LivingEntity wearer) {
        var scope = CURRENT.get();
        return scope != null && scope.wearer == wearer ? scope : null;
    }

    boolean contains(String slot, Item item) {
        return equipped.getOrDefault(slot, Set.of()).contains(item);
    }

    // 槽位、主人、消耗状态与前置条件全部校验通过后才能调用。
    boolean unique(SlotContext context, Item item) {
        var position = new Position(context.identifier(), context.index(), context.cosmetic());
        var first = accepted.putIfAbsent(item, position);
        return first == null || first.equals(position);
    }

    @Override
    public void close() {
        if (previous == null) CURRENT.remove();
        else CURRENT.set(previous);
    }
}

package com.thelongtravail.compat.fluid;

import static com.thelongtravail.compat.fluid.FluidReflection.*;

import com.thelongtravail.abyss.copy.CopyStorage;
import com.thelongtravail.abyss.copy.FluidCopyGraph;

import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

import java.util.*;

// AE2 15.x 直接存储资源键和长整型数量，不能按序列化物品堆叠处理。
final class FluidAeStore {
    private static final Set<String> BASIC =
            Set.of(
                    "appeng.items.storage.BasicStorageCell",
                    "appeng.items.tools.powered.PortableCellItem");

    static void transform(CopyStorage.Context graph, CompoundTag data, int depth) throws Exception {
        ItemStack stack = ItemStack.of(data);
        String implementation = stack.getItem().getClass().getName();
        CompoundTag tag = data.getCompound("tag");
        if (type("appeng.api.implementations.items.ISpatialStorageCell")
                .isInstance(stack.getItem())) {
            if (tag.contains("plot_id"))
                throw new IllegalStateException("Bound spatial cell cannot be copied");
        } else if (implementation.equals("appeng.items.storage.CreativeCellItem")) {
            Object config =
                    call(
                            stack.getItem(),
                            "getConfigInventory",
                            new Class<?>[] {ItemStack.class},
                            stack);
            for (Object key : (Set<?>) call(config, "keySet")) {
                if (type("appeng.api.stacks.AEItemKey").isInstance(key)) {
                    ItemStack sample = (ItemStack) call(key, "toStack");
                    CompoundTag serialized = sample.save(new CompoundTag());
                    if (FluidCopyGraph.hasReferences(serialized)
                            || com.thelongtravail.abyss.PerfectFluidCopies.clearFluid(serialized))
                        throw new IllegalStateException(
                                "Creative cell supplies a protected resource");
                }
            }
        } else if (BASIC.contains(implementation)) {
            rewrite(graph, stack, tag, depth);
        } else if (!implementation.equals("appeng.items.storage.ViewCellItem")) {
            throw new IllegalStateException(
                    "Unsupported AE2 cell implementation: " + implementation);
        }
        // list 保存的是过滤条件，不是实际库存；不得复制其中的唯一标识或清除资源键。
        for (String key : new ArrayList<>(tag.getAllKeys()))
            if (!Set.of("keys", "amts", "ic", "list").contains(key))
                graph.transform(tag.get(key), depth + 2);
        for (String key : new ArrayList<>(data.getAllKeys()))
            if (!key.equals("tag")) graph.transform(data.get(key), depth + 1);
    }

    private static void rewrite(
            CopyStorage.Context graph, ItemStack stack, CompoundTag tag, int depth)
            throws Exception {
        boolean keysPresent = tag.contains("keys"), amountsPresent = tag.contains("amts");
        if (!keysPresent && !amountsPresent && !tag.contains("ic")) return;
        if (!tag.contains("keys", Tag.TAG_LIST)
                || !tag.contains("amts", Tag.TAG_LONG_ARRAY)
                || !tag.contains("ic", Tag.TAG_LONG))
            throw new IllegalStateException("Malformed AE2 inventory fields");
        ListTag keys = tag.getList("keys", Tag.TAG_COMPOUND);
        long[] amounts = tag.getLongArray("amts");
        if (keys.size() != amounts.length
                || (keys.isEmpty() && ((ListTag) tag.get("keys")).size() != 0))
            throw new IllegalStateException("Mismatched AE2 keys and amounts");
        Class<?> aeKey = type("appeng.api.stacks.AEKey"),
                itemKey = type("appeng.api.stacks.AEItemKey");
        Object channel = call(stack.getItem(), "getKeyType");
        Map<Object, Long> merged = new LinkedHashMap<>();
        long originalTotal = 0, total = 0;
        for (int i = 0; i < keys.size(); i++) {
            graph.account(keys.getCompound(i), depth + 3);
            long amount = amounts[i];
            if (amount <= 0) throw new IllegalStateException("Invalid AE2 resource amount");
            originalTotal = Math.addExact(originalTotal, amount);
            Object key =
                    call(
                            aeKey,
                            "fromTagGeneric",
                            new Class<?>[] {CompoundTag.class},
                            keys.getCompound(i));
            if (key == null || !channel.equals(call(key, "getType")))
                throw new IllegalStateException("Unknown or wrong AE2 resource channel");
            if (itemKey.isInstance(key)) {
                ItemStack sample = (ItemStack) call(key, "toStack");
                CompoundTag serialized = sample.save(new CompoundTag());
                graph.transform(serialized, depth + 4);
                sample = ItemStack.of(serialized);
                if (sample.isEmpty()) continue;
                key = call(itemKey, "of", new Class<?>[] {ItemStack.class}, sample);
                if (key == null) throw new IllegalStateException("Cannot rebuild AE2 item key");
            } else if (!type("appeng.api.stacks.AEFluidKey").isInstance(key)) {
                throw new IllegalStateException("Unsupported AE2 resource key");
            }
            merged.put(key, Math.addExact(merged.getOrDefault(key, 0L), amount));
            total = Math.addExact(total, amount);
        }
        if (originalTotal != tag.getLong("ic"))
            throw new IllegalStateException("Incorrect AE2 stored total");
        if (merged.isEmpty()) {
            tag.remove("keys");
            tag.remove("amts");
            tag.remove("ic");
        } else {
            ListTag result = new ListTag();
            long[] counts = new long[merged.size()];
            int index = 0;
            for (var entry : merged.entrySet()) {
                result.add((Tag) call(entry.getKey(), "toTagGeneric"));
                counts[index++] = entry.getValue();
            }
            tag.put("keys", result);
            tag.putLongArray("amts", counts);
            tag.putLong("ic", total);
        }
        // 通过 AE2 回读校验，不能调用插入接口，以免分区或销毁升级影响复制结果。
        ItemStack verification = stack.copy();
        verification.setTag(tag.copy());
        Object inventory =
                call(
                        type("appeng.me.cells.BasicCellInventory"),
                        "createInventory",
                        new Class<?>[] {
                            ItemStack.class, type("appeng.api.storage.cells.ISaveProvider")
                        },
                        verification,
                        null);
        Object counter = type("appeng.api.stacks.KeyCounter").getConstructor().newInstance();
        call(inventory, "getAvailableStacks", new Class<?>[] {counter.getClass()}, counter);
        if ((long) call(inventory, "getStoredItemCount") != total
                || (long) call(inventory, "getStoredItemTypes") != merged.size())
            throw new IllegalStateException("AE2 inventory roundtrip failed");
        for (var entry : merged.entrySet())
            if ((long) call(counter, "get", new Class<?>[] {aeKey}, entry.getKey())
                    != entry.getValue())
                throw new IllegalStateException("AE2 resource roundtrip failed");
    }

    private FluidAeStore() {}
}

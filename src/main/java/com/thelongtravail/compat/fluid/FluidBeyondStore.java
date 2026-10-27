package com.thelongtravail.compat.fluid;

import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import java.util.*;
import static com.thelongtravail.compat.fluid.FluidReflection.*;

// Beyond Dimensions 0.7.30: 只处理物质球的实际库存，不解释过滤样本或网络绑定。
final class FluidBeyondStore {
    private static final String API = "com.wintercogs.beyonddimensions.api.storage.key.";
    private static final String ITEM = API + "impl.ItemStackKey";
    private static final Set<String> IMPLEMENTATIONS =
            Set.of(
                    ITEM,
                    API + "impl.FluidStackKey",
                    API + "impl.EnergyStackKey",
                    "com.wintercogs.beyonddimensions.integration.module.botania.storage.ManaStackKey",
                    "com.wintercogs.beyonddimensions.integration.module.ars.storage.SourceStackKey",
                    "com.wintercogs.beyonddimensions.integration.module.mekanism.storage.GasStackKey",
                    "com.wintercogs.beyonddimensions.integration.module.mekanism.storage.InfusionStackKey",
                    "com.wintercogs.beyonddimensions.integration.module.mekanism.storage.PigmentStackKey",
                    "com.wintercogs.beyonddimensions.integration.module.mekanism.storage.SlurryStackKey");

    static boolean handles(CompoundTag item) {
        return ModList.get().isLoaded("beyonddimensions")
                && item.contains("Count", Tag.TAG_ANY_NUMERIC)
                && item.getString("id").equals("beyonddimensions:matter_compress_ball");
    }

    static void transform(FluidCopyGraph graph, CompoundTag data, int depth) throws Exception {
        if (data.contains("tag") && !data.contains("tag", Tag.TAG_COMPOUND))
            throw new IllegalStateException("Malformed matter ball tag");
        CompoundTag tag = data.getCompound("tag");
        if (tag.contains("stack_list")) {
            if (!(tag.get("stack_list") instanceof ListTag entries)
                    || (!entries.isEmpty() && entries.getElementType() != Tag.TAG_COMPOUND))
                throw new IllegalStateException("Malformed matter ball inventory");
            ListTag result = new ListTag();
            for (Tag entry : entries) {
                CompoundTag raw = (CompoundTag) entry;
                // 解码前先计入遍历预算，避免异常或过深的数据绕过限制。
                graph.account(raw, depth + 3);
                CompoundTag normalized = normalize(raw);
                Object decoded =
                        call(
                                type(API + "KeyAmount"),
                                "deserializeNBT",
                                new Class<?>[] {CompoundTag.class},
                                normalized.copy());
                Object key = call(decoded, "key");
                long amount = (long) call(decoded, "amount");
                if (amount <= 0
                        || !IMPLEMENTATIONS.contains(key.getClass().getName())
                        || (boolean) call(key, "isEmpty"))
                    throw new IllegalStateException("Invalid or unsupported matter ball resource");
                CompoundTag canonical = encode(key, amount);
                // 上游解码可能将缺失 ID 转为空资源或丢弃字段，须核对往返结果。
                if (!canonical.equals(normalized))
                    throw new IllegalStateException(
                            "Matter ball resource cannot roundtrip losslessly");
                if (key.getClass().getName().equals(ITEM)) {
                    ItemStack sample =
                            (ItemStack)
                                    call(
                                            key,
                                            "copyStackWithCount",
                                            new Class<?>[] {long.class},
                                            1L);
                    Object reconstructed =
                            type(ITEM).getConstructor(ItemStack.class).newInstance(sample);
                    if (!encode(reconstructed, amount).equals(canonical))
                        throw new IllegalStateException(
                                "Matter ball item sample lost resource data");
                    CompoundTag serialized = sample.save(new CompoundTag());
                    graph.transform(serialized, depth + 6);
                    ItemStack output = ItemStack.of(serialized);
                    if (output.isEmpty())
                        continue; // 转换结果为空时移除对应存储条目。
                    key = type(ITEM).getConstructor(ItemStack.class).newInstance(output);
                }
                CompoundTag encoded = encode(key, amount);
                Object verified =
                        call(
                                type(API + "KeyAmount"),
                                "deserializeNBT",
                                new Class<?>[] {CompoundTag.class},
                                encoded.copy());
                if ((long) call(verified, "amount") != amount
                        || !encode(call(verified, "key"), amount).equals(encoded))
                    throw new IllegalStateException("Matter ball output roundtrip failed");
                // 保留条目顺序及各自的 long 数量，不跨资源求和限制总量。
                result.add(encoded);
            }
            tag.put("stack_list", result);
        }
        for (String key : tag.getAllKeys().toArray(new String[0]))
            if (!key.equals("stack_list")) graph.transform(tag.get(key), depth + 2);
        for (String key : data.getAllKeys().toArray(new String[0]))
            if (!key.equals("tag")) graph.transform(data.get(key), depth + 1);
    }

    private static CompoundTag encode(Object key, long amount) throws Exception {
        Class<?> keyAmount = type(API + "KeyAmount");
        Object pair =
                keyAmount
                        .getConstructor(type(API + "IStackKey"), long.class)
                        .newInstance(key, amount);
        return (CompoundTag) call(keyAmount, "serializeNBT", new Class<?>[] {keyAmount}, pair);
    }

    private static CompoundTag normalize(CompoundTag raw) {
        // 数量仅接受整数类型，避免 getLong 静默截断浮点数。
        String count = raw.contains("amount") ? "amount" : "Amount";
        Tag quantity = raw.get(count);
        if (quantity == null || quantity.getId() < Tag.TAG_BYTE || quantity.getId() > Tag.TAG_LONG)
            throw new IllegalStateException("Missing or nonintegral matter ball amount");
        CompoundTag normalized = new CompoundTag();
        normalized.putLong("amount", ((NumericTag) quantity).getAsLong());
        if (raw.contains("key", Tag.TAG_COMPOUND)) {
            requireKeys(raw, Set.of("key", count));
            CompoundTag key = raw.getCompound("key");
            requireKeys(key, Set.of("type", "stack"));
            if (!key.contains("type", Tag.TAG_STRING) || !key.contains("stack", Tag.TAG_COMPOUND))
                throw new IllegalStateException("Malformed matter ball key");
            normalized.put("key", key.copy());
        } else if (raw.contains("Type", Tag.TAG_STRING)
                && raw.contains("Stack", Tag.TAG_COMPOUND)) {
            requireKeys(raw, Set.of("Type", count, "Stack"));
            CompoundTag old = raw.getCompound("Stack"), payload = new CompoundTag();
            String kind = raw.getString("Type");
            if (kind.equals("beyonddimensions:stack_type/item")) {
                requireKeys(old, Set.of("id", "Count", "tag", "ForgeCaps"));
                if (!old.contains("id", Tag.TAG_STRING))
                    throw new IllegalStateException("Malformed legacy matter ball item");
                payload.putString("item", old.getString("id"));
                copyCompound(old, payload, "tag", "tag");
                copyCompound(old, payload, "ForgeCaps", "caps");
            } else {
                // 未知旧格式直接拒绝，不能静默转换为空资源。
                throw new IllegalStateException("Unsupported legacy matter ball resource layout");
            }
            CompoundTag key = new CompoundTag();
            key.putString("type", kind);
            key.put("stack", payload);
            normalized.put("key", key);
        } else throw new IllegalStateException("Unsupported matter ball entry layout");
        return normalized;
    }

    private static void copyCompound(CompoundTag from, CompoundTag to, String old, String next) {
        if (!from.contains(old)) return;
        if (!from.contains(old, Tag.TAG_COMPOUND))
            throw new IllegalStateException("Malformed legacy matter ball payload");
        to.put(next, from.getCompound(old).copy());
    }

    private static void requireKeys(CompoundTag tag, Set<String> allowed) {
        if (!allowed.containsAll(tag.getAllKeys()))
            throw new IllegalStateException("Unknown matter ball fields");
    }

    private FluidBeyondStore() {}
}

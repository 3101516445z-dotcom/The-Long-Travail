package com.thelongtravail.compat.fluid;

import com.refinedmods.refinedstorage.api.storage.disk.IStorageDisk;
import com.refinedmods.refinedstorage.api.storage.disk.IStorageDiskManager;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.refinedmods.refinedstorage.util.StackUtils;
import com.thelongtravail.abyss.copy.CopyStorage;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.*;

final class FluidRsStore {
    static com.refinedmods.refinedstorage.api.IRSAPI api() {
        return API.instance();
    }

    static IStorageDiskManager manager(ServerLevel level) {
        return api().getStorageDiskManager(level);
    }

    static IStorageDisk<?> disk(IStorageDiskManager manager, UUID id) {
        return manager.get(id);
    }

    static void add(IStorageDiskManager manager, UUID id, IStorageDisk<?> disk) {
        manager.set(id, disk);
        manager.markForSaving();
    }

    static void remove(IStorageDiskManager manager, UUID id) {
        manager.remove(id);
        manager.markForSaving();
    }

    static boolean identifies(Object item) {
        return item instanceof com.refinedmods.refinedstorage.api.storage.disk.IStorageDiskProvider;
    }

    static CopyStorage.Write prepare(CopyStorage.Context graph, CopyStorage.Pending pending)
            throws Exception {
        var manager = manager(graph.level());
        var source = disk(manager, pending.source().key().id());
        if (source == null) throw new IllegalStateException("Missing RS source inventory");
        String implementation = source.getClass().getName();
        if (!Set.of(
                        "com.refinedmods.refinedstorage.apiimpl.storage.disk.ItemStorageDisk",
                        "com.refinedmods.refinedstorage.apiimpl.storage.disk.FluidStorageDisk",
                        "com.huanghuang.rsintegration.feature.resonance.disk.ResonanceDiskWrapper")
                .contains(implementation))
            throw new IllegalStateException(
                    "Unsupported RS disk implementation: " + implementation);
        CompoundTag data = source.writeToNbt().copy();
        // RS 使用整型 Quantity 保存数量，不能按物品堆叠的字节型 Count 处理。
        if (data.contains("Items", Tag.TAG_LIST)) {
            ListTag original = data.getList("Items", Tag.TAG_COMPOUND), items = new ListTag();
            for (Tag child : original) {
                ItemStack stack = StackUtils.deserializeStackFromNbt((CompoundTag) child);
                if (stack.isEmpty())
                    throw new IllegalStateException("Unreadable RS inventory entry");
                ItemStack transformed = graph.item(stack);
                if (!transformed.isEmpty()) items.add(StackUtils.serializeStackToNbt(transformed));
            }
            data.put("Items", items);
        }
        var factory = api().getStorageDiskRegistry().get(source.getFactoryId());
        if (factory == null) throw new IllegalStateException("RS disk factory missing");
        var copy = factory.createFromNbt(graph.level(), data);
        if (copy == source || !copy.getClass().equals(source.getClass()))
            throw new IllegalStateException("RS factory changed disk type");
        if (disk(manager, pending.id()) != null)
            throw new IllegalStateException("RS UUID collision");
        return new CopyStorage.Write() {
            boolean owned;

            public void commit() throws Exception {
                if (disk(manager, pending.id()) != null)
                    throw new IllegalStateException("RS UUID collision");
                owned = true;
                add(manager, pending.id(), copy);
            }

            public void rollback() throws Exception {
                if (owned) remove(manager, pending.id());
            }
        };
    }

    private FluidRsStore() {}
}

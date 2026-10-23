package com.thelongtravail.compat.fluid;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import static com.thelongtravail.compat.fluid.FluidReflection.*;

final class FluidRsStore {
    static Object api() throws Exception {
        return call(type("com.refinedmods.refinedstorage.apiimpl.API"), "instance");
    }

    static Object manager(ServerLevel level) throws Exception {
        return call(api(), "getStorageDiskManager", new Class<?>[] {ServerLevel.class}, level);
    }

    static Object disk(Object manager, UUID id) throws Exception {
        return call(manager, "get", new Class<?>[] {UUID.class}, id);
    }

    static void add(Object manager, UUID id, Object disk) throws Exception {
        call(
                manager,
                "set",
                new Class<?>[] {
                    UUID.class, type("com.refinedmods.refinedstorage.api.storage.disk.IStorageDisk")
                },
                id,
                disk);
        call(manager, "markForSaving");
    }

    static void remove(Object manager, UUID id) throws Exception {
        call(manager, "remove", new Class<?>[] {UUID.class}, id);
        call(manager, "markForSaving");
    }

    static FluidCopyGraph.Write prepare(FluidCopyGraph graph, FluidCopyGraph.Pending pending)
            throws Exception {
        Object manager = manager(graph.level());
        Object source = disk(manager, pending.source().key().id());
        if (source == null) throw new IllegalStateException("Missing RS source inventory");
        String implementation = source.getClass().getName();
        if (!Set.of(
                        "com.refinedmods.refinedstorage.apiimpl.storage.disk.ItemStorageDisk",
                        "com.refinedmods.refinedstorage.apiimpl.storage.disk.FluidStorageDisk",
                        "com.huanghuang.rsintegration.resonance.disk.ResonanceDiskWrapper")
                .contains(implementation))
            throw new IllegalStateException(
                    "Unsupported RS disk implementation: " + implementation);
        CompoundTag data = ((CompoundTag) call(source, "writeToNbt")).copy();
        // RS 使用整型 Quantity 保存数量，不能按物品堆叠的字节型 Count 处理。
        if (data.contains("Items", Tag.TAG_LIST)) {
            ListTag original = data.getList("Items", Tag.TAG_COMPOUND), items = new ListTag();
            Class<?> utils = type("com.refinedmods.refinedstorage.util.StackUtils");
            for (Tag child : original) {
                ItemStack stack =
                        (ItemStack)
                                call(
                                        utils,
                                        "deserializeStackFromNbt",
                                        new Class<?>[] {CompoundTag.class},
                                        child);
                if (stack.isEmpty())
                    throw new IllegalStateException("Unreadable RS inventory entry");
                ItemStack transformed = graph.item(stack);
                if (!transformed.isEmpty())
                    items.add(
                            (Tag)
                                    call(
                                            utils,
                                            "serializeStackToNbt",
                                            new Class<?>[] {ItemStack.class},
                                            transformed));
            }
            data.put("Items", items);
        }
        Object registry = call(api(), "getStorageDiskRegistry");
        Object factory =
                call(
                        registry,
                        "get",
                        new Class<?>[] {ResourceLocation.class},
                        call(source, "getFactoryId"));
        if (factory == null) throw new IllegalStateException("RS disk factory missing");
        Object copy =
                call(
                        factory,
                        "createFromNbt",
                        new Class<?>[] {ServerLevel.class, CompoundTag.class},
                        graph.level(),
                        data);
        if (copy == source || !copy.getClass().equals(source.getClass()))
            throw new IllegalStateException("RS factory changed disk type");
        if (disk(manager, pending.id()) != null)
            throw new IllegalStateException("RS UUID collision");
        return new FluidCopyGraph.Write() {
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

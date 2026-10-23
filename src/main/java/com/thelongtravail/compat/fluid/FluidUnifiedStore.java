package com.thelongtravail.compat.fluid;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fluids.FluidStack;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiConsumer;
import static com.thelongtravail.compat.fluid.FluidReflection.*;

// RSI 1.5.1 在 RS 中只保存引用，实际数据位于 DiskFileStore。
final class FluidUnifiedStore {
    private static final String RS = "com.huanghuang.rsintegration.disk.rs.";
    private static final String CORE = "com.huanghuang.rsintegration.disk.core.";
    private static final String FILE =
            "com.huanghuang.rsintegration.disk.persistence.DiskFileStore";

    @SuppressWarnings("unchecked")
    static FluidCopyGraph.Write prepare(FluidCopyGraph graph, FluidCopyGraph.Pending pending)
            throws Exception {
        Object manager =
                call(
                        type(RS + "UnifiedDiskManager"),
                        "get",
                        new Class<?>[] {ServerLevel.class},
                        graph.level());
        UUID world = (UUID) call(manager, "worldId");
        var identity = pending.source().identity();
        if (!(Boolean) call(manager, "enabled")
                || identity.getInt("Format") != 1
                || !identity.hasUUID("World")
                || !identity.getUUID("World").equals(world)
                || (int) get(manager, "activeBackups") != 0)
            throw new IllegalStateException(
                    "Unified disk unavailable, wrong world/format, or backup active");
        Object files = get(manager, "files");
        Map<UUID, Object> entries = (Map<UUID, Object>) get(manager, "entries");
        UUID old = pending.source().key().id();
        Object known = entries.get(old);
        Object original = known == null ? null : get(known, "core");
        if (original == null) {
            Object manifest = call(files, "manifest", new Class<?>[] {UUID.class}, old);
            Object limits = call(manifest, "limits");
            original =
                    call(
                            files,
                            "load",
                            new Class<?>[] {UUID.class, UUID.class, int.class, int.class},
                            world,
                            old,
                            call(limits, "items"),
                            call(limits, "fluids"));
        }
        if (!world.equals(get(original, "worldId")) || !old.equals(get(original, "diskId")))
            throw new IllegalStateException("Unified disk identity mismatch");
        UUID owner = (UUID) get(original, "owner");
        Class<?> coreType = type(CORE + "UnifiedDiskCore");
        Object copy =
                coreType.getConstructor(
                                UUID.class,
                                UUID.class,
                                UUID.class,
                                type(CORE + "UnifiedDiskCore$Limits"))
                        .newInstance(world, pending.id(), owner, get(original, "limits"));
        copyTable(graph, get(original, "items"), copy, false);
        copyTable(graph, get(original, "fluids"), copy, true);
        Object snapshot = call(type(FILE + "$Snapshot"), "freeze", new Class<?>[] {coreType}, copy);
        Object rs = FluidRsStore.manager(graph.level());
        Path parent =
                graph.level()
                        .getServer()
                        .getWorldPath(LevelResource.ROOT)
                        .resolve("data/rs_integration/unified_disks")
                        .toAbsolutePath()
                        .normalize();
        Path directory = parent.resolve(pending.id().toString()).normalize();
        if (!directory.getParent().equals(parent)
                || Files.exists(directory)
                || entries.containsKey(pending.id())
                || FluidRsStore.disk(rs, pending.id()) != null)
            throw new IllegalStateException("Unified UUID collision");
        Object root =
                type(RS + "UnifiedDiskRoot")
                        .getConstructor(
                                type(RS + "UnifiedDiskManager"), UUID.class, UUID.class, UUID.class)
                        .newInstance(manager, pending.id(), world, owner);
        return new FluidCopyGraph.Write() {
            boolean owned;

            public void commit() throws Exception {
                if (Files.exists(directory)
                        || entries.containsKey(pending.id())
                        || FluidRsStore.disk(rs, pending.id()) != null)
                    throw new IllegalStateException("Unified UUID collision");
                owned = true;
                Object saved =
                        call(
                                files,
                                "save",
                                new Class<?>[] {type(FILE + "$Snapshot"), type(FILE + "$Manifest")},
                                snapshot,
                                null);
                var constructor =
                        type(RS + "UnifiedDiskManager$Entry").getDeclaredConstructor(UUID.class);
                constructor.setAccessible(true);
                Object entry = constructor.newInstance(pending.id());
                field(entry, "core").set(entry, copy);
                field(entry, "manifest").set(entry, call(saved, "manifest"));
                entries.put(pending.id(), entry);
                FluidRsStore.add(rs, pending.id(), root);
            }

            public void rollback() throws Exception {
                if (!owned) return;
                FluidRsStore.remove(rs, pending.id());
                entries.remove(pending.id());
                // 只清理本次事务新建的唯一标识目录，不得修改源目录。
                if (Files.exists(directory)) {
                    try (var paths = Files.walk(directory)) {
                        for (Path path : paths.sorted(Comparator.reverseOrder()).toList())
                            Files.delete(path);
                    }
                }
            }
        };
    }

    private static void copyTable(FluidCopyGraph graph, Object table, Object copy, boolean fluid)
            throws Exception {
        if ((int) call(table, "quarantinedSize") != 0)
            throw new IllegalStateException("Cannot copy quarantined unified inventory");
        List<Map.Entry<Object, Integer>> values = new ArrayList<>();
        call(
                table,
                "forEach",
                new Class<?>[] {BiConsumer.class},
                (BiConsumer<Object, Integer>) (key, amount) -> values.add(Map.entry(key, amount)));
        for (var value : values) {
            int amount = value.getValue();
            int accepted;
            if (fluid) {
                FluidStack stack =
                        (FluidStack)
                                call(value.getKey(), "fluidStack", new Class<?>[] {int.class}, 1);
                accepted =
                        (int)
                                call(
                                        copy,
                                        "insertFluid",
                                        new Class<?>[] {FluidStack.class, int.class, boolean.class},
                                        stack,
                                        amount,
                                        true);
            } else {
                ItemStack source =
                        (ItemStack)
                                call(value.getKey(), "itemStack", new Class<?>[] {int.class}, 1);
                ItemStack stack = graph.item(source);
                if (stack.isEmpty()) continue;
                accepted =
                        (int)
                                call(
                                        copy,
                                        "insertItem",
                                        new Class<?>[] {ItemStack.class, int.class, boolean.class},
                                        stack,
                                        amount,
                                        true);
            }
            if (accepted != amount)
                throw new IllegalStateException("Unified copy would truncate inventory quantities");
        }
    }

    private FluidUnifiedStore() {}
}

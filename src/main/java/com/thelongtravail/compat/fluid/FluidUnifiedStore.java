package com.thelongtravail.compat.fluid;

import com.huanghuang.rsintegration.storage.disk.core.*;
import com.huanghuang.rsintegration.storage.disk.persistence.DiskFileStore;
import com.huanghuang.rsintegration.storage.disk.rs.*;
import com.thelongtravail.abyss.copy.CopyStorage;
import com.thelongtravail.compat.mixin.fluid.UnifiedEntryInvoker;
import com.thelongtravail.compat.mixin.fluid.UnifiedManagerAccessor;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fluids.FluidStack;

import java.nio.file.*;
import java.util.*;

// RSI 1.5.3 在 RS 中只保存引用，实际数据位于 DiskFileStore。
final class FluidUnifiedStore {
    @SuppressWarnings("unchecked")
    static CopyStorage.Write prepare(CopyStorage.Context graph, CopyStorage.Pending pending)
            throws Exception {
        var manager = UnifiedDiskManager.get(graph.level());
        var access = (UnifiedManagerAccessor) (Object) manager;
        UUID world = manager.worldId();
        var identity = pending.source().identity();
        if (!manager.enabled()
                || identity.getInt("Format") != 1
                || !identity.hasUUID("World")
                || !identity.getUUID("World").equals(world)
                || access.travail$activeBackups() != 0)
            throw new IllegalStateException(
                    "Unified disk unavailable, wrong world/format, or backup active");
        var files = access.travail$files();
        var entries = access.travail$entries();
        UUID old = pending.source().key().id();
        var known = entries.get(old);
        var original = known == null ? null : known.core;
        if (original == null) {
            var manifest = files.manifest(old);
            var limits = manifest.limits();
            original = files.load(world, old, limits.items(), limits.fluids());
        }
        if (!world.equals(original.worldId) || !old.equals(original.diskId))
            throw new IllegalStateException("Unified disk identity mismatch");
        UUID owner = original.owner;
        var copy = new UnifiedDiskCore(world, pending.id(), owner, original.limits);
        copyTable(graph, original.items, copy, false);
        copyTable(graph, original.fluids, copy, true);
        for (var kind : com.huanghuang.rsintegration.magic.MagicResourceKind.values()) {
            long amount = original.magicAmount(kind);
            if (copy.insertMagic(kind, amount, true) != amount)
                throw new IllegalStateException("Unified copy would truncate magic resources");
        }
        var snapshot = DiskFileStore.Snapshot.freeze(copy);
        var rs = FluidRsStore.manager(graph.level());
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
        var root = new UnifiedDiskRoot(manager, pending.id(), world, owner);
        return new CopyStorage.Write() {
            boolean owned;

            public void commit() throws Exception {
                if (access.travail$activeBackups() != 0)
                    throw new IllegalStateException("Unified backup active");
                if (Files.exists(directory)
                        || entries.containsKey(pending.id())
                        || FluidRsStore.disk(rs, pending.id()) != null)
                    throw new IllegalStateException("Unified UUID collision");
                owned = true;
                var saved = files.save(snapshot, null);
                var entry = UnifiedEntryInvoker.travail$create(pending.id());
                entry.core = copy;
                entry.manifest = saved.manifest();
                entries.put(pending.id(), entry);
                FluidRsStore.add(rs, pending.id(), root);
            }

            public void rollback() throws Exception {
                if (!owned) return;
                try {
                    UnifiedCopyCleanup.clean(
                            pending.id(),
                            directory,
                            () -> FluidRsStore.remove(rs, pending.id()),
                            () -> entries.remove(pending.id()));
                } catch (java.io.IOException failure) {
                    // 残留需人工处理，每个失败的新身份都须记录，不依赖外层限频提示。
                    com.thelongtravail.TheLongTravail.LOGGER.warn(
                            "Copied unified disk cleanup incomplete; no automatic retry: {}",
                            pending.id(),
                            failure);
                    throw failure;
                }
            }
        };
    }

    private static void copyTable(
            CopyStorage.Context graph, ResourceTable table, UnifiedDiskCore copy, boolean fluid)
            throws Exception {
        if (table.quarantinedSize() != 0)
            throw new IllegalStateException("Cannot copy quarantined unified inventory");
        List<Map.Entry<FrozenKey, Long>> values = new ArrayList<>();
        table.forEach((key, amount) -> values.add(Map.entry(key, amount)));
        for (var value : values) {
            long amount = value.getValue();
            long accepted;
            if (fluid) {
                FluidStack stack = value.getKey().fluidStack(1);
                accepted = copy.insertFluid(stack, amount, true);
            } else {
                ItemStack source = value.getKey().itemStack(1);
                ItemStack stack = graph.item(source);
                if (stack.isEmpty()) continue;
                accepted = copy.insertItem(stack, amount, true);
            }
            if (accepted != amount)
                throw new IllegalStateException("Unified copy would truncate inventory quantities");
        }
    }

    private FluidUnifiedStore() {}
}

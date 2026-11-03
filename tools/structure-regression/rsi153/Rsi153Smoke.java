package travail.smoke;

import static travail.smoke.RsFluidSmoke.*;

import com.huanghuang.rsintegration.magic.MagicResourceKind;
import com.huanghuang.rsintegration.storage.disk.core.*;
import com.huanghuang.rsintegration.storage.disk.rs.UnifiedDiskManager;
import com.thelongtravail.abyss.copy.FluidCopyGraph;
import com.thelongtravail.compat.mixin.fluid.UnifiedManagerAccessor;

import net.minecraft.nbt.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import java.nio.file.*;
import java.util.*;

/** Real RSI 1.5.3 stores, including quantities that cannot fit in ItemStack counts. */
public final class Rsi153Smoke {
    static UnifiedDiskCore data(ItemStack stack) throws Exception {
        return (UnifiedDiskCore) core(stack);
    }

    static CompoundTag snapshot(UnifiedDiskCore core) {
        CompoundTag result = new CompoundTag();
        result.putUUID("Owner", core.owner);
        result.putString("Limits", core.limits.toString());
        List<String> items = new ArrayList<>(), fluids = new ArrayList<>();
        core.items.forEach((k, n) -> items.add(k.itemStack(1).save(new CompoundTag()) + "=" + n));
        core.fluids.forEach(
                (k, n) -> fluids.add(k.fluidStack(1).writeToNBT(new CompoundTag()) + "=" + n));
        Collections.sort(items);
        Collections.sort(fluids);
        result.putString("Items", items.toString());
        result.putString("Fluids", fluids.toString());
        result.putLongArray("Magic", core.magicSnapshot());
        return result;
    }

    static void reject(ItemStack stack) throws Exception {
        var before = stack.save(new CompoundTag());
        boolean rejected = false;
        try {
            FluidCopyGraph.prepare(level, stack);
        } catch (Exception expected) {
            rejected = true;
        }
        check(
                rejected && before.equals(stack.save(new CompoundTag())),
                "invalid disk rejects without source mutation");
    }

    public static void run() throws Exception {
        long started = System.nanoTime();
        var manager = UnifiedDiskManager.get(level);
        var access = (UnifiedManagerAccessor) (Object) manager;
        Path restart = Path.of("rsi153-restart.nbt");
        if (Files.exists(restart)) {
            var saved = NbtIo.readCompressed(restart.toFile());
            var stack = ItemStack.of(saved.getCompound("Stack"));
            check(
                    snapshot(data(stack)).equals(saved.getCompound("Expected")),
                    "fresh process reads long amounts and all magic");
            Files.writeString(
                    Path.of("matrix-result.txt"),
                    "MATRIX_PASS RSI153_RESTART checks=" + BackpackFluidSmoke.checks.size());
            return;
        }
        ItemStack source = unified();
        var original = data(source);
        long huge = (long) Integer.MAX_VALUE + 123456789L;
        check(
                original.insertItem(new ItemStack(Items.EMERALD), huge, true) == huge,
                "long item insert");
        check(
                original.insertFluid(new FluidStack(Fluids.LAVA, 1), huge + 77, true) == huge + 77,
                "long fluid insert");
        for (var kind : MagicResourceKind.values()) {
            long amount = Math.min(huge + kind.ordinal(), original.magicCapacity(kind));
            check(original.insertMagic(kind, amount, true) == amount, "magic insert " + kind);
        }
        check(
                original.goetySouls() == original.magicAmount(MagicResourceKind.SOUL),
                "souls alias same resource");
        var expected = snapshot(original);
        ItemStack last = ItemStack.EMPTY;
        for (int i = 0; i < 300; i++) {
            last = craft(source, ClickType.PICKUP, (i & 1) == 0);
            check(!id(source).equals(id(last)), "independent identity");
            check(expected.equals(snapshot(data(last))), "exact long and magic copy " + i);
            check(expected.equals(snapshot(original)), "source retained " + i);
        }
        var loaded = access.travail$files().load(manager.worldId(), id(last));
        check(expected.equals(snapshot(loaded)), "committed files preserve complete inventory");
        data(last).extractMagic(MagicResourceKind.SOUL, 1, true);
        check(expected.equals(snapshot(original)), "magic mutation independent");
        // Both prepare-time and commit-time backup protection.
        var pending = FluidCopyGraph.prepare(level, source);
        var beforeFiles = new HashSet<>(manager.savedDiskIds());
        try (var lease = manager.beginBackup()) {
            reject(source);
            boolean failed = false;
            try {
                pending.commit();
            } catch (Exception e) {
                failed = true;
            }
            check(failed, "backup beginning after prepare prevents commit");
        }
        check(
                beforeFiles.equals(new HashSet<>(manager.savedDiskIds())),
                "backup rejection creates no files");
        for (String field : List.of("World", "Format", "Id")) {
            var bad = source.copy();
            if (field.equals("Format")) bad.getTag().putInt(field, 999);
            else bad.getTag().putUUID(field, UUID.randomUUID());
            reject(bad);
        }
        // A real cross-store failure after all commits must remove the new RSI directory.
        failures();
        beforeFiles = new HashSet<>(manager.savedDiskIds());
        var prepared = FluidCopyGraph.prepare(level, source);
        prepared.commit();
        check(
                !beforeFiles.equals(new HashSet<>(manager.savedDiskIds())),
                "committed clone persisted");
        prepared.rollback();
        check(
                beforeFiles.equals(new HashSet<>(manager.savedDiskIds())),
                "rollback removes only clone files");
        check(expected.equals(snapshot(original)), "rollback preserves source");
        // RSI's real file writer fails after creating pages but before committing the manifest.
        var files = access.travail$files();
        var hook = files.getClass().getDeclaredField("beforeCommit");
        hook.setAccessible(true);
        var oldHook = hook.get(files);
        var failedWrite = FluidCopyGraph.prepare(level, source);
        var directory =
                level.getServer()
                        .getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                        .resolve("data/rs_integration/unified_disks")
                        .resolve(id(failedWrite.output()).toString());
        try {
            hook.set(
                    files,
                    (com.huanghuang.rsintegration.storage.disk.persistence.DiskFileStore
                                    .BeforeCommit)
                            () -> {
                                throw new java.io.IOException("injected before manifest commit");
                            });
            boolean failed = false;
            try {
                failedWrite.commit();
            } catch (Exception e) {
                failed = true;
            }
            check(failed && !Files.exists(directory), "partial file write cleans new directory");
            check(
                    !access.travail$entries().containsKey(id(failedWrite.output())),
                    "partial file write leaves no cache entry");
            check(expected.equals(snapshot(original)), "partial file write preserves source");
        } finally {
            hook.set(files, oldHook);
        }
        var damaged = unified();
        var damagedCore = data(damaged);
        damagedCore.items.restoreQuarantined(
                0,
                new ResourceTable.QuarantinedResource(
                        FrozenKey.Kind.ITEM,
                        new net.minecraft.resources.ResourceLocation("missing:resource"),
                        new byte[] {1}),
                1,
                1L,
                1L);
        reject(damaged);
        var max = unified();
        check(
                data(max).insertItem(new ItemStack(Items.IRON_INGOT), Long.MAX_VALUE, true)
                        == Long.MAX_VALUE,
                "long max insert");
        var maxCopy = craft(max, ClickType.PICKUP, false);
        check(snapshot(data(max)).equals(snapshot(data(maxCopy))), "long max exact copy");
        var fixture = new CompoundTag();
        fixture.put("Stack", last.save(new CompoundTag()));
        fixture.put("Expected", snapshot(data(last)));
        NbtIo.writeCompressed(fixture, restart.toFile());
        Files.write(Path.of("rsi153-checks.txt"), BackpackFluidSmoke.checks);
        Files.writeString(
                Path.of("matrix-result.txt"),
                "MATRIX_PASS RSI153 copies=300 checks="
                        + BackpackFluidSmoke.checks.size()
                        + " elapsedMs="
                        + (System.nanoTime() - started) / 1000000);
    }
}

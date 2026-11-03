package com.thelongtravail.compat.fluid;

import static com.thelongtravail.abyss.copy.CopyStorage.*;

import com.thelongtravail.abyss.copy.CopyStorage;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

// 识别器独立保留；移除某个实现时将该 Entry 的实现设为 null。
public final class FluidStorageAdapters {
    public static void initialize() {
        CopyStorage.install(
                List.of(
                        new Entry(
                                "beyond",
                                3,
                                FluidStorageAdapters::beyond,
                                false,
                                new Adapter() {
                                    public void transform(
                                            Context context, CompoundTag data, int depth)
                                            throws Exception {
                                        com.thelongtravail.compat.DirectCalls.storage(
                                                () -> {
                                                    FluidBeyondStore.transform(
                                                            context, data, depth);
                                                    return null;
                                                });
                                    }
                                }),
                        new Entry(
                                "ae",
                                2,
                                FluidStorageAdapters::ae,
                                true,
                                new Adapter() {
                                    public void transform(
                                            Context context, CompoundTag data, int depth)
                                            throws Exception {
                                        com.thelongtravail.compat.DirectCalls.storage(
                                                () -> {
                                                    FluidAeStore.transform(context, data, depth);
                                                    return null;
                                                });
                                    }
                                }),
                        new Entry(
                                "backpack",
                                0,
                                FluidStorageAdapters::backpack,
                                false,
                                new Adapter() {
                                    public Reference reference(CompoundTag data) {
                                        CompoundTag tag = data.getCompound("tag");
                                        return new Reference(
                                                new Key("backpack", tag.getUUID("contentsUuid")),
                                                tag,
                                                "contentsUuid",
                                                "");
                                    }

                                    public Write prepare(Context context, Pending pending)
                                            throws Exception {
                                        return guarded(
                                                com.thelongtravail.compat.DirectCalls.storage(
                                                        () ->
                                                                FluidBackpackStore.prepare(
                                                                        context, pending)));
                                    }
                                }),
                        new Entry(
                                "rs",
                                1,
                                FluidStorageAdapters::rs,
                                false,
                                new Adapter() {
                                    public Reference reference(CompoundTag data) {
                                        CompoundTag tag = data.getCompound("tag");
                                        return new Reference(
                                                new Key("rs", tag.getUUID("Id")),
                                                tag,
                                                "Id",
                                                data.getString("id")
                                                                .equals(
                                                                        "rs_integration:unified_storage_disk")
                                                        ? "unified"
                                                        : "");
                                    }

                                    public Write prepare(Context context, Pending pending)
                                            throws Exception {
                                        return guarded(
                                                com.thelongtravail.compat.DirectCalls.storage(
                                                        () ->
                                                                pending.source()
                                                                                .variant()
                                                                                .equals("unified")
                                                                        ? FluidUnifiedStore.prepare(
                                                                                context, pending)
                                                                        : FluidRsStore.prepare(
                                                                                context, pending)));
                                    }
                                })));
    }

    private static boolean backpack(CompoundTag data) {
        return data.contains("Count", Tag.TAG_ANY_NUMERIC)
                && ModList.get().isLoaded("sophisticatedbackpacks")
                && data.getString("id").startsWith("sophisticatedbackpacks:")
                && data.getCompound("tag").hasUUID("contentsUuid");
    }

    private static boolean rs(CompoundTag data) {
        if (!data.contains("Count", Tag.TAG_ANY_NUMERIC)
                || !ModList.get().isLoaded("refinedstorage")
                || !data.getCompound("tag").hasUUID("Id")) return false;
        var id = ResourceLocation.tryParse(data.getString("id"));
        Object item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        try {
            return com.thelongtravail.compat.DirectCalls.call(() -> FluidRsStore.identifies(item));
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("RS disk API unavailable", failure);
        }
    }

    private static boolean beyond(CompoundTag data) {
        return ModList.get().isLoaded("beyonddimensions")
                && data.contains("Count", Tag.TAG_ANY_NUMERIC)
                && data.getString("id").equals("beyonddimensions:matter_compress_ball");
    }

    private static boolean ae(CompoundTag data) {
        if (!data.contains("Count", Tag.TAG_ANY_NUMERIC) || !ModList.get().isLoaded("ae2"))
            return false;
        var id = ResourceLocation.tryParse(data.getString("id"));
        var item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        if (item == null) return false;
        try {
            return com.thelongtravail.compat.DirectCalls.call(() -> FluidAeStore.identifies(item));
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("AE2 cell API unavailable", failure);
        }
    }

    private static Write guarded(Write write) {
        return new Write() {
            public void commit() throws Exception {
                com.thelongtravail.compat.DirectCalls.storage(
                        () -> {
                            write.commit();
                            return null;
                        });
            }

            public void rollback() throws Exception {
                com.thelongtravail.compat.DirectCalls.storage(
                        () -> {
                            write.rollback();
                            return null;
                        });
            }
        };
    }

    private FluidStorageAdapters() {}
}

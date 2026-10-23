package com.thelongtravail.compat.fluid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;
import static com.thelongtravail.compat.fluid.FluidReflection.*;

final class FluidBackpackStore {
    @SuppressWarnings("unchecked")
    static FluidCopyGraph.Write prepare(FluidCopyGraph graph, FluidCopyGraph.Pending pending)
            throws Exception {
        Object store =
                call(type("net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage"), "get");
        Map<UUID, CompoundTag> contents = (Map<UUID, CompoundTag>) get(store, "backpackContents");
        CompoundTag source = contents.get(pending.source().key().id());
        if (source == null) throw new IllegalStateException("Missing source backpack inventory");
        if (contents.containsKey(pending.id()))
            throw new IllegalStateException("Backpack UUID collision");
        CompoundTag copy = source.copy();
        graph.transform(copy, 0);
        return new FluidCopyGraph.Write() {
            boolean owned;

            public void commit() throws Exception {
                if (contents.containsKey(pending.id()))
                    throw new IllegalStateException("Backpack UUID collision");
                owned = true;
                call(
                        store,
                        "setBackpackContents",
                        new Class<?>[] {UUID.class, CompoundTag.class},
                        pending.id(),
                        copy);
                ((SavedData) store).setDirty();
            }

            public void rollback() throws Exception {
                if (!owned) return;
                call(store, "removeBackpackContents", new Class<?>[] {UUID.class}, pending.id());
                ((SavedData) store).setDirty();
            }
        };
    }

    private FluidBackpackStore() {}
}

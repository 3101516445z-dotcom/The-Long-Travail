package com.thelongtravail.compat.fluid;

import com.thelongtravail.abyss.copy.CopyStorage;
import com.thelongtravail.compat.mixin.fluid.BackpackStorageAccessor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;

import java.util.*;

final class FluidBackpackStore {
    @SuppressWarnings("unchecked")
    static CopyStorage.Write prepare(CopyStorage.Context graph, CopyStorage.Pending pending)
            throws Exception {
        var store = BackpackStorage.get();
        Map<UUID, CompoundTag> contents = ((BackpackStorageAccessor) store).travail$contents();
        CompoundTag source = contents.get(pending.source().key().id());
        if (source == null) throw new IllegalStateException("Missing source backpack inventory");
        // Mob Catcher 将实体保存在外部库存中；任一嵌套背包含实体时拒绝整个复制图。
        // 旧版没有此字段，空列表也不限制；异常类型不能当作空库存放行。
        Tag captured = source.get("capturedMobs");
        if (captured != null && (!(captured instanceof ListTag list) || !list.isEmpty()))
            throw new IllegalStateException("Cannot copy a backpack containing captured mobs");
        if (contents.containsKey(pending.id()))
            throw new IllegalStateException("Backpack UUID collision");
        CompoundTag copy = source.copy();
        graph.transform(copy, 0);
        return new CopyStorage.Write() {
            boolean owned;

            public void commit() throws Exception {
                if (contents.containsKey(pending.id()))
                    throw new IllegalStateException("Backpack UUID collision");
                owned = true;
                store.setBackpackContents(pending.id(), copy);
                ((SavedData) store).setDirty();
            }

            public void rollback() throws Exception {
                if (!owned) return;
                store.removeBackpackContents(pending.id());
                ((SavedData) store).setDirty();
            }
        };
    }

    private FluidBackpackStore() {}
}

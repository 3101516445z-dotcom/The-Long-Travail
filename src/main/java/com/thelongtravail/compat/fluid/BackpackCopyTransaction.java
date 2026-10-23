package com.thelongtravail.compat.fluid;

import net.minecraft.nbt.CompoundTag;
import java.util.*;

// 批次只能在服务端游戏线程执行，且不得写入源物品栏。
final class BackpackCopyTransaction {
    interface Store {
        CompoundTag read(UUID id) throws ReflectiveOperationException;

        void remove(UUID id) throws ReflectiveOperationException;

        void write(UUID id, CompoundTag data) throws ReflectiveOperationException;

        void dirty();
    }

    static void commit(Map<UUID, CompoundTag> pending, Store store)
            throws ReflectiveOperationException {
        if (pending.isEmpty()) return;
        var before = new LinkedHashMap<UUID, CompoundTag>();

        for (UUID id : pending.keySet()) {
            CompoundTag previous = store.read(id);
            before.put(id, previous == null ? null : previous.copy());
        }
        var attempted = new ArrayList<UUID>();
        try {
            for (var entry : pending.entrySet()) {
                // 移除或写入操作可能先改变状态再抛出异常，因此必须先登记当前目标。
                attempted.add(entry.getKey());
                store.remove(entry.getKey());
                store.write(entry.getKey(), entry.getValue().copy());
            }
            store.dirty();
        } catch (ReflectiveOperationException | RuntimeException failure) {
            for (int i = attempted.size() - 1; i >= 0; i--) {
                UUID id = attempted.get(i);
                try {
                    store.remove(id);
                    if (before.get(id) != null) store.write(id, before.get(id));
                } catch (ReflectiveOperationException | RuntimeException rollback) {
                    failure.addSuppressed(rollback);
                }
            }
            try {
                store.dirty();
            } catch (RuntimeException dirty) {
                failure.addSuppressed(dirty);
            }
            throw failure;
        }
    }

    private BackpackCopyTransaction() {}
}

package com.thelongtravail.abyss.copy;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.Predicate;

// 外部存储复制契约；图身份与事务由复制内核管理。
public final class CopyStorage {
    public record Key(String system, UUID id) {}

    public record Reference(Key key, CompoundTag identity, String field, String variant) {}

    public record Pending(Reference source, UUID id) {}

    public interface Write {
        void commit() throws Exception;

        void rollback() throws Exception;
    }

    public interface Context {
        void account(Tag root, int depth);

        void transform(Tag node, int depth) throws Exception;

        ItemStack item(ItemStack source) throws Exception;

        ServerLevel level();
    }

    public interface Adapter {
        default Reference reference(CompoundTag data) {
            return null;
        }

        default void transform(Context context, CompoundTag data, int depth) throws Exception {
            throw new IllegalStateException("Missing inline storage transformer");
        }

        default Write prepare(Context context, Pending pending) throws Exception {
            throw new IllegalStateException("Missing external storage writer");
        }
    }

    // detector 独立于实现；停用实现时保留登记，避免外部身份退化为普通 NBT 复制。
    public record Entry(
            String id,
            int identificationOrder,
            Predicate<CompoundTag> detector,
            boolean accountSubtree,
            Adapter implementation) {
        public Entry {
            Objects.requireNonNull(id);
            Objects.requireNonNull(detector);
        }

        public Adapter requireImplementation() {
            if (implementation == null)
                throw new IllegalStateException("Unsupported external storage: " + id);
            return implementation;
        }

        public Entry withoutImplementation() {
            return new Entry(id, identificationOrder, detector, accountSubtree, null);
        }
    }

    private record Registry(List<Entry> transform, List<Entry> identify) {}

    private static volatile Registry registry = new Registry(List.of(), List.of());

    public static void install(List<Entry> value) {
        List<Entry> snapshot = List.copyOf(value);
        Set<String> ids = new HashSet<>();
        for (Entry entry : snapshot)
            if (!ids.add(entry.id()))
                throw new IllegalArgumentException("Duplicate storage adapter: " + entry.id());
        registry =
                new Registry(
                        snapshot,
                        snapshot.stream()
                                .sorted(Comparator.comparingInt(Entry::identificationOrder))
                                .toList());
    }

    public static List<Entry> snapshot() {
        return registry.transform();
    }

    static List<Entry> identificationSnapshot() {
        return registry.identify();
    }

    private CopyStorage() {}
}

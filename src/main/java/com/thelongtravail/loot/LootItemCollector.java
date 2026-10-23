package com.thelongtravail.loot;

import com.thelongtravail.mixin.*;
import java.util.*;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.entries.*;

public final class LootItemCollector {
    public static final int MAX_ITEMS = 128, MAX_NODES = 4096, MAX_TABLES = 64, MAX_DEPTH = 16;

    public record Result(List<Item> items, boolean limited, int skipped) {}

    private static final Map<LootDataManager, Cache> CACHES = new WeakHashMap<>();

    public static final class Cache {
        private static final int MAX_ROOTS = 256;
        private final LinkedHashMap<ResourceLocation, Cached> roots =
                new LinkedHashMap<>(16, .75F, true);

        public Result collect(
                ResourceLocation root, Function<ResourceLocation, LootTable> resolver) {
            Cached old = roots.get(root);
            if (old != null && old.valid(resolver)) return old.result;
            roots.remove(root);
            var reader = new LootItemCollector(resolver);
            reader.dependencies = new ArrayList<>();
            reader.table(root, 0);
            var result = new Result(List.copyOf(reader.items), reader.limited, reader.skipped);
            if (reader.cacheable) {
                if (roots.size() >= MAX_ROOTS) roots.remove(roots.keySet().iterator().next());
                roots.put(root, new Cached(result, List.copyOf(reader.dependencies)));
            }
            return result;
        }
    }

    private record Dependency(ResourceLocation id, LootTable table) {}

    private record Cached(Result result, List<Dependency> dependencies) {
        boolean valid(Function<ResourceLocation, LootTable> resolver) {
            for (var dependency : dependencies)
                if (resolver.apply(dependency.id) != dependency.table) return false;
            return true;
        }
    }

    public static Result collectCached(ResourceLocation root, LootDataManager manager) {
        return CACHES.computeIfAbsent(manager, unused -> new Cache())
                .collect(root, manager::getLootTable);
    }

    public static void clearCache() {
        CACHES.clear();
    }

    private final Function<ResourceLocation, LootTable> resolver;
    private List<Dependency> dependencies;
    private boolean cacheable = true;
    private final Set<ResourceLocation> visited = new HashSet<>();
    private final LinkedHashSet<Item> items = new LinkedHashSet<>();
    private int nodes, skipped;
    private boolean limited;

    private LootItemCollector(Function<ResourceLocation, LootTable> resolver) {
        this.resolver = resolver;
    }

    public static Result collect(
            ResourceLocation root, Function<ResourceLocation, LootTable> resolver) {
        var reader = new LootItemCollector(resolver);
        reader.table(root, 0);
        return new Result(List.copyOf(reader.items), reader.limited, reader.skipped);
    }

    private boolean budget(int depth) {
        if (depth > MAX_DEPTH || nodes >= MAX_NODES || items.size() >= MAX_ITEMS) {
            limited = true;
            return false;
        }
        nodes++;
        return true;
    }

    private void table(ResourceLocation id, int depth) {
        if (visited.contains(id)) return;
        if (visited.size() >= MAX_TABLES) {
            limited = true;
            return;
        }
        if (!budget(depth)) return;
        visited.add(id);
        LootTable table = resolver.apply(id);
        if (dependencies != null) {
            dependencies.add(new Dependency(id, table));
            // 尚未冻结的战利品表可被 Forge API 修改，因此每次调用都须重新解析。
            if (table != null && table != LootTable.EMPTY && !table.isFrozen()) cacheable = false;
        }
        if (table == null || table == LootTable.EMPTY) return;
        for (var pool : ((ExecutionLootTableAccessor) table).travail$pools()) {
            if (!budget(depth)) break;
            for (var entry : ((ExecutionLootPoolAccessor) pool).travail$entries()) {
                if (nodes >= MAX_NODES || items.size() >= MAX_ITEMS) {
                    limited = true;
                    break;
                }
                entry(entry, depth + 1);
            }
        }
    }

    private void entry(LootPoolEntryContainer entry, int depth) {
        if (!budget(depth)) return;
        // 仅匹配原版类的确切类型，避免调用第三方重写的类型查询或展开逻辑。
        if (entry.getClass() == LootItem.class) {
            Item item = ((ExecutionLootItemAccessor) entry).travail$item();
            if (item != Items.AIR) items.add(item);
        } else if (entry.getClass() == LootTableReference.class) {
            table(((ExecutionLootReferenceAccessor) entry).travail$name(), depth + 1);
        } else if (entry.getClass() == AlternativesEntry.class
                || entry.getClass() == SequentialEntry.class
                || entry.getClass() == EntryGroup.class) {
            for (var child : ((ExecutionLootCompositeAccessor) entry).travail$children()) {
                if (nodes >= MAX_NODES || items.size() >= MAX_ITEMS) {
                    limited = true;
                    break;
                }
                entry(child, depth + 1);
            }
        } else if (entry.getClass() != EmptyLootItem.class) skipped++;
    }
}

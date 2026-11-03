package com.thelongtravail.abyss.copy;

import static com.thelongtravail.abyss.copy.CopyStorage.*;
import static com.thelongtravail.diagnostics.RuntimeDiagnostics.Metric.*;

import com.thelongtravail.diagnostics.RuntimeDiagnostics;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

// 每次合成共用一份复制关系；共享引用和循环引用均须指向新副本，不能回指源对象。
public final class FluidCopyGraph implements Context {
    private record Queued(Entry entry, Pending pending) {}

    private final List<Entry> adapters = CopyStorage.snapshot();
    private final ServerLevel level;
    private final Map<Key, UUID> ids = new LinkedHashMap<>();
    private final ArrayDeque<Queued> pending = new ArrayDeque<>();
    private final List<Write> writes = new ArrayList<>();
    private final List<Write> attempted = new ArrayList<>();
    private long visited;
    private final boolean preview;

    private FluidCopyGraph(ServerLevel level, boolean preview) {
        this.level = level;
        this.preview = preview;
    }

    public static boolean hasReferences(Tag node) {
        long started = RuntimeDiagnostics.start();
        try {
            return identify(node);
        } finally {
            RuntimeDiagnostics.end(FLUID_IDENTIFY_NS, started);
        }
    }

    private static boolean identify(Tag node) {
        ArrayDeque<Tag> queue = new ArrayDeque<>();
        queue.add(node);
        long count = 0;
        List<Entry> identification = CopyStorage.identificationSnapshot();
        while (!queue.isEmpty()) {
            if (++count > 4_000_000) return true;
            Tag next = queue.removeLast();
            if (next instanceof CompoundTag c) {
                for (Entry entry : identification) if (entry.detector().test(c)) return true;
                for (String key : c.getAllKeys()) queue.add(c.get(key));
            } else if (next instanceof ListTag l) for (Tag child : l) queue.add(child);
        }
        return false;
    }

    public void account(Tag root, int depth) {
        record Node(Tag tag, int depth) {}
        ArrayDeque<Node> queue = new ArrayDeque<>();
        queue.add(new Node(root, depth));
        while (!queue.isEmpty()) {
            Node node = queue.removeLast();
            if (node.depth > 128 || ++visited > 4_000_000)
                throw new IllegalStateException("Container copy exceeds traversal limit");
            if (node.tag instanceof CompoundTag c)
                for (String key : c.getAllKeys()) queue.add(new Node(c.get(key), node.depth + 1));
            else if (node.tag instanceof ListTag l)
                for (Tag child : l) queue.add(new Node(child, node.depth + 1));
        }
    }

    public static void reserveIds(CompoundTag data) {
        long started = RuntimeDiagnostics.start();
        try {
            new FluidCopyGraph(null, true).transform(data, 0);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot preview external inventory copy", e);
        } finally {
            RuntimeDiagnostics.end(FLUID_PREVIEW_NS, started);
        }
    }

    public static Prepared prepare(ServerLevel level, ItemStack source) throws Exception {
        long started = RuntimeDiagnostics.start();
        try {
            return prepareGraph(level, source);
        } finally {
            RuntimeDiagnostics.end(FLUID_PREPARE_NS, started);
        }
    }

    private static Prepared prepareGraph(ServerLevel level, ItemStack source) throws Exception {
        FluidCopyGraph graph = new FluidCopyGraph(level, false);
        // 此处只复制一件源物品，Count 不代表容器中的库存数量。
        CompoundTag data = source.copyWithCount(1).save(new CompoundTag());
        graph.transform(data, 0);
        while (!graph.pending.isEmpty()) {
            Queued queued = graph.pending.removeFirst();
            graph.writes.add(
                    queued.entry().requireImplementation().prepare(graph, queued.pending()));
        }
        ItemStack output = ItemStack.of(data);
        if (output.isEmpty()) throw new IllegalStateException("Empty copied item");
        RuntimeDiagnostics.observe(FLUID_NODES, graph.visited);
        RuntimeDiagnostics.observe(FLUID_STORES, graph.ids.size());
        return new Prepared(output, graph);
    }

    public void transform(Tag node, int depth) throws Exception {
        if (depth > 128 || ++visited > 4_000_000)
            throw new IllegalStateException("Container copy exceeds traversal limit");
        if (node instanceof CompoundTag c) {
            if (c.contains("Count", Tag.TAG_ANY_NUMERIC)
                    && com.thelongtravail.abyss.PerfectFluidCopies.isProtected(c.getString("id"))) {
                for (String key : new ArrayList<>(c.getAllKeys()))
                    if (!key.equals("Slot")) c.remove(key);
                c.putString("id", "minecraft:air");
                c.putByte("Count", (byte) 0);
                return;
            }
            Entry entry = null;
            for (Entry candidate : adapters)
                if (candidate.detector().test(c)) {
                    entry = candidate;
                    break;
                }
            if (entry != null) {
                Adapter adapter = entry.requireImplementation();
                Reference ref = adapter.reference(c);
                if (ref == null) {
                    if (entry.accountSubtree()) account(c, depth);
                    adapter.transform(this, c, depth);
                    return;
                }
                UUID next = ids.get(ref.key());
                if (next == null) {
                    if (ids.size() >= 4096)
                        throw new IllegalStateException(
                                "Too many external inventories in one copy");
                    next = UUID.randomUUID();
                    ids.put(ref.key(), next);
                    if (!preview)
                        pending.addLast(
                                new Queued(
                                        entry,
                                        new Pending(
                                                new Reference(
                                                        ref.key(),
                                                        ref.identity().copy(),
                                                        ref.field(),
                                                        ref.variant()),
                                                next)));
                }
                ref.identity().putUUID(ref.field(), next);
            }
            // 子节点转换可能修改标签，遍历前须保存键名快照。
            for (String key : c.getAllKeys().toArray(new String[0]))
                transform(c.get(key), depth + 1);
        } else if (node instanceof ListTag list) for (Tag child : list) transform(child, depth + 1);
    }

    public ItemStack item(ItemStack source) throws Exception {
        if (source.isEmpty()
                || com.thelongtravail.abyss.PerfectFluidCopies.isProtected(
                        ForgeRegistries.ITEMS.getKey(source.getItem()).toString()))
            return ItemStack.EMPTY;
        int count = source.getCount();
        CompoundTag data = source.copyWithCount(1).save(new CompoundTag());
        transform(data, 0);
        ItemStack result = ItemStack.of(data);
        if (result.isEmpty()) throw new IllegalStateException("Stored item could not be copied");
        result.setCount(count);
        return result;
    }

    public ServerLevel level() {
        return level;
    }

    public record Prepared(ItemStack output, FluidCopyGraph graph) {
        public void commit() throws Exception {
            long started = RuntimeDiagnostics.start();
            try {
                for (Write write : graph.writes) {
                    graph.attempted.add(write);
                    write.commit();
                }
            } catch (Exception failure) {
                try {
                    rollback();
                } catch (Exception rollback) {
                    failure.addSuppressed(rollback);
                }
                throw failure;
            } finally {
                RuntimeDiagnostics.end(FLUID_COMMIT_NS, started);
            }
        }

        public void rollback() throws Exception {
            long started = RuntimeDiagnostics.start();
            try {
                rollbackWrites();
            } finally {
                RuntimeDiagnostics.end(FLUID_ROLLBACK_NS, started);
            }
        }

        private void rollbackWrites() throws Exception {
            Exception failure = null;
            for (int i = graph.attempted.size() - 1; i >= 0; i--) {
                try {
                    graph.attempted.get(i).rollback();
                } catch (Exception e) {
                    if (failure == null) failure = e;
                    else failure.addSuppressed(e);
                }
            }
            graph.attempted.clear();
            if (failure != null) throw failure;
        }
    }
}

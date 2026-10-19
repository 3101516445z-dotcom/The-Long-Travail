package com.thelongtravail.loot;

import com.thelongtravail.mixin.*;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.*;

import java.util.*;
import java.util.function.*;

public final class WeightedLootSampler {
    public record Result(List<ItemStack> items, int skipped, boolean complete) {}

    private record Gate(Object identity, Predicate<LootContext> test) {}

    private record Candidate(
            LootItem item,
            int weight,
            List<Gate> gates,
            List<BiFunction<ItemStack, LootContext, ItemStack>> functions) {}

    private record Eligible(Candidate candidate, LootPoolEntry expanded) {}

    private final Function<ResourceLocation, LootTable> resolver;
    private final Predicate<Item> excluded;
    private final List<Candidate> candidates = new ArrayList<>();
    private int nodes, skipped;

    private WeightedLootSampler(Function<ResourceLocation, LootTable> resolver, Predicate<Item> excluded) {
        this.resolver = resolver;
        this.excluded = excluded;
    }

    public static Result sample(
            ResourceLocation id,
            Function<ResourceLocation, LootTable> resolver,
            LootContext context,
            int count,
            int fixedCount,
            Predicate<Item> excluded) {
        if (count < 0 || count > 64 || fixedCount < 0) throw new IllegalArgumentException("Invalid sampling count");
        context.setQueriedLootTableId(id);
        var sampler = new WeightedLootSampler(resolver, excluded);
        sampler.table(id, List.of(), List.of(), null, new HashSet<>(), 0, false);
        List<ItemStack> result = new ArrayList<>();
        Set<Candidate> invalid = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int roll = 0; roll < count; roll++) {
            Map<Object, Boolean> gates = new IdentityHashMap<>();
            List<Eligible> eligible = new ArrayList<>();
            for (var c : sampler.candidates) {
                if (invalid.contains(c)) continue;
                boolean allowed = true;
                for (var gate : c.gates)
                    if (!gates.computeIfAbsent(gate.identity, k -> gate.test.test(context))) {
                        allowed = false;
                        break;
                    }
                if (allowed) c.item.expand(context, e -> eligible.add(new Eligible(c, e)));
            }
            boolean produced = false;
            for (int attempt = 0; attempt < 32 && !eligible.isEmpty(); attempt++) {
                long total = 0;
                for (var e : eligible) total += e.candidate.weight;
                if (total <= 0) break;
                long random = bounded(context, total);
                Eligible chosen = eligible.get(eligible.size() - 1);
                for (var e : eligible) {
                    random -= e.candidate.weight;
                    if (random < 0) {
                        chosen = e;
                        break;
                    }
                }
                List<ItemStack> outputs = new ArrayList<>();
                var selected = chosen;
                selected.expanded.createItemStack(
                        stack -> {
                            if (outputs.size() > 1) return;
                            ItemStack value = stack.copy();
                            for (var function : selected.candidate.functions) value = function.apply(value, context);
                            if (!value.isEmpty() && !excluded.test(value.getItem())) outputs.add(value);
                        },
                        context);
                if (outputs.size() == 1) {
                    ItemStack value = outputs.get(0).copy();
                    if (fixedCount > 0) value.setCount(fixedCount);
                    result.add(value);
                    produced = true;
                    break;
                }
                sampler.skipped++;
                invalid.add(selected.candidate);
                eligible.remove(selected);
            }
            if (!produced) return new Result(List.copyOf(result), sampler.skipped, false);
        }
        return new Result(List.copyOf(result), sampler.skipped, true);
    }

    // 使用拒绝采样消除取模偏差，不依赖 RandomSource 的具体实现。
    private static long bounded(LootContext context, long bound) {
        long bits, value;
        do {
            bits = context.getRandom().nextLong() >>> 1;
            value = bits % bound;
        } while (bits - value + (bound - 1) < 0);
        return value;
    }

    private void table(
            ResourceLocation id,
            List<Gate> parent,
            List<BiFunction<ItemStack, LootContext, ItemStack>> after,
            Integer weight,
            Set<ResourceLocation> path,
            int depth,
            boolean simple) {
        if (depth > 8 || nodes++ >= 2048 || !path.add(id)) {
            skipped++;
            return;
        }
        try {
            LootTable table = resolver.apply(id);
            if (table == null || table == LootTable.EMPTY) {
                skipped++;
                return;
            }
            var pools = ((ExecutionLootTableAccessor) table).travail$pools();
            if (simple
                    && (pools.size() != 1
                            || ((ExecutionLootPoolAccessor) pools.get(0)).travail$entries().length != 1)) {
                skipped++;
                return;
            }
            for (var pool : pools) {
                var gates = new ArrayList<>(parent);
                gates.add(new Gate(pool, ((LootPoolAccess) pool).travail$condition()));
                var functions = new ArrayList<BiFunction<ItemStack, LootContext, ItemStack>>();
                functions.add(((LootPoolAccess) pool).travail$function());
                functions.add(((LootTableAccess) table).travail$function());
                functions.addAll(after);
                for (var entry : ((ExecutionLootPoolAccessor) pool).travail$entries()) {
                    if (nodes++ >= 2048) {
                        skipped++;
                        return;
                    }
                    if (entry.getClass() == LootItem.class) {
                        Item item = ((ExecutionLootItemAccessor) entry).travail$item();
                        int w = weight == null ? ((LootSingletonAccess) entry).travail$weight() : weight;
                        if (item != Items.AIR && !excluded.test(item) && w > 0)
                            candidates.add(
                                    new Candidate((LootItem) entry, w, List.copyOf(gates), List.copyOf(functions)));
                    } else if (entry.getClass() == LootTableReference.class) {
                        var nestedGates = new ArrayList<>(gates);
                        nestedGates.add(new Gate(entry, ((LootEntryAccess) entry).travail$condition()));
                        var nestedFunctions = new ArrayList<BiFunction<ItemStack, LootContext, ItemStack>>();
                        nestedFunctions.add(((LootSingletonAccess) entry).travail$function());
                        nestedFunctions.addAll(functions);
                        table(
                                ((ExecutionLootReferenceAccessor) entry).travail$name(),
                                nestedGates,
                                nestedFunctions,
                                weight == null ? ((LootSingletonAccess) entry).travail$weight() : weight,
                                path,
                                depth + 1,
                                true);
                    } else if (entry.getClass() != EmptyLootItem.class) skipped++;
                }
            }
        } finally {
            path.remove(id);
        }
    }
}

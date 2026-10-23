package com.thelongtravail.valley;

import java.util.List;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootTable;
import com.thelongtravail.loot.LootItemCollector;

public final class ExecutionLootTable {
    public static final int MAX_ITEMS = LootItemCollector.MAX_ITEMS,
            MAX_NODES = LootItemCollector.MAX_NODES,
            MAX_TABLES = LootItemCollector.MAX_TABLES,
            MAX_DEPTH = LootItemCollector.MAX_DEPTH;

    public record Result(List<Item> items, boolean limited, int skipped) {}

    public static Result collect(
            ResourceLocation root, Function<ResourceLocation, LootTable> resolver) {
        var result = LootItemCollector.collect(root, resolver);
        return new Result(result.items(), result.limited(), result.skipped());
    }

    private ExecutionLootTable() {}
}

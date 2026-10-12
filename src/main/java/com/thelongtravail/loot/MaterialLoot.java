package com.thelongtravail.loot;

import java.util.Map;
import com.google.gson.JsonObject;
import com.thelongtravail.TheLongTravail;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

// 材料子表与特殊来源的同池精粹规则分开登记，均独立于可配置的饰品奖励。
public final class MaterialLoot {
    public static final String POOL = "the_long_travail:materials";
    private static final Map<ResourceLocation, ResourceLocation> TABLES =
            Map.of(
                    vanilla("pillager_outpost"), material("pillager_outpost"),
                    vanilla("woodland_mansion"), material("woodland_mansion"),
                    vanilla("desert_pyramid"), material("desert_pyramid"),
                    vanilla("village/village_fisher"), material("village_fisher"),
                    vanilla("abandoned_mineshaft"), material("abandoned_mineshaft"));
    private static final Map<ResourceLocation, ResourceLocation> ESSENCE_TABLES =
            Map.of(
                    source("blocks/budding_amethyst"), material("essences/blocks/budding_amethyst"),
                    source("chests/buried_treasure"), material("essences/chests/buried_treasure"),
                    source("chests/jungle_temple"), material("essences/chests/jungle_temple"),
                    source("chests/stronghold_library"),
                            material("essences/chests/stronghold_library"));

    private record SpecialReward(ResourceLocation item, double percent) {}

    private static final Map<ResourceLocation, SpecialReward> SPECIAL_REWARDS =
            Map.ofEntries(
                    special("archaeology/desert_pyramid", "underworld", 15),
                    special("archaeology/desert_well", "far_reach", 15),
                    special("archaeology/ocean_ruin_cold", "abyss", 15),
                    special("archaeology/ocean_ruin_warm", "abyss", 15),
                    special("archaeology/trail_ruins_common", "deep_valley", 15),
                    special("archaeology/trail_ruins_rare", "deep_valley", 15),
                    special("gameplay/cat_morning_gift", "boundless", 20),
                    special("gameplay/fishing/treasure", "abyss", 10),
                    special("gameplay/hero_of_the_village/cartographer_gift", "far_reach", 15),
                    special("gameplay/hero_of_the_village/farmer_gift", "flourishing", 15),
                    special("gameplay/piglin_bartering", "underworld", 0.5),
                    special("gameplay/sniffer_digging", "flourishing", 20));

    private static ResourceLocation source(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }

    private static Map.Entry<ResourceLocation, SpecialReward> special(
            String path, String aspect, double percent) {
        return Map.entry(
                source(path),
                new SpecialReward(
                        ResourceLocation.fromNamespaceAndPath(
                                "the_long_travail", aspect + "_essence"),
                        percent));
    }

    // 特殊来源与原奖励同池抽取，按目标概率缩放权重，保留原物品的函数和相对比例。
    private static void injectSpecial(LootTable table, SpecialReward rule) {
        var gson = Deserializers.createLootTableSerializer().create();
        var json = gson.toJsonTree(table, LootTable.class).getAsJsonObject();
        var pools = json.getAsJsonArray("pools");
        if (pools == null || pools.size() != 1 || !empty(json, "functions")) {
            unsupported(table);
            return;
        }
        var pool = pools.get(0).getAsJsonObject();
        // 其他模组通过构建器创建的池可能没有名称，无法安全按名称替换。
        if (!pool.has("name") || pool.get("name").isJsonNull()) {
            TheLongTravail.LOGGER.warn("特殊来源表 {} 包含无名称奖励池，保留该表并跳过精粹注入", table.getLootTableId());
            return;
        }
        if (!number(pool, "rolls", 1)
                || !number(pool, "bonus_rolls", 0)
                || !empty(pool, "conditions")
                || !empty(pool, "functions")) {
            unsupported(table);
            return;
        }
        var entries = pool.getAsJsonArray("entries");
        if (entries == null || entries.isEmpty()) {
            unsupported(table);
            return;
        }
        int percent = (int) Math.round(rule.percent() * 2);
        int divisor = percent, denominator = 200;
        while (denominator != 0) {
            int remainder = divisor % denominator;
            divisor = denominator;
            denominator = remainder;
        }
        int nativeWeight = (200 - percent) / divisor;
        int essenceWeight = percent / divisor;
        long total = 0;
        for (var element : entries) {
            var entry = element.getAsJsonObject();
            String type = entry.get("type").getAsString();
            if (!(type.equals("minecraft:item")
                            || type.equals("minecraft:loot_table")
                            || type.equals("minecraft:empty"))
                    || !empty(entry, "conditions")
                    || !number(entry, "quality", 0)) {
                unsupported(table);
                return;
            }
            int weight = entry.has("weight") ? entry.get("weight").getAsInt() : 1;
            if (weight <= 0) {
                unsupported(table);
                return;
            }
            total += weight;
        }
        if (total <= 0 || total > Integer.MAX_VALUE / (nativeWeight + essenceWeight)) {
            unsupported(table);
            return;
        }
        for (var element : entries) {
            var entry = element.getAsJsonObject();
            entry.addProperty(
                    "weight",
                    (entry.has("weight") ? entry.get("weight").getAsInt() : 1) * nativeWeight);
        }
        var reward = new JsonObject();
        reward.addProperty("type", "minecraft:item");
        reward.addProperty("name", rule.item().toString());
        reward.addProperty("weight", total * essenceWeight);
        entries.add(reward);
        String oldName = pool.get("name").getAsString();
        var builder =
                LootPool.lootPool()
                        .name(POOL)
                        .setRolls(ConstantValue.exactly(1))
                        .setBonusRolls(ConstantValue.exactly(0));
        for (var element : entries)
            builder.add(new LoadedEntry(gson.fromJson(element, LootPoolEntryContainer.class)));
        var replacement = builder.build();
        table.removePool(oldName);
        table.addPool(replacement);
    }

    private static final class LoadedEntry extends LootPoolEntryContainer.Builder<LoadedEntry> {
        private final LootPoolEntryContainer entry;

        private LoadedEntry(LootPoolEntryContainer entry) {
            this.entry = entry;
        }

        @Override
        protected LoadedEntry getThis() {
            return this;
        }

        @Override
        public LootPoolEntryContainer build() {
            return entry;
        }
    }

    private static boolean empty(JsonObject json, String key) {
        return !json.has(key) || json.getAsJsonArray(key).isEmpty();
    }

    private static boolean number(JsonObject json, String key, int expected) {
        if (!json.has(key)) return expected == 0;
        var value = json.get(key);
        return value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isNumber()
                && value.getAsDouble() == expected;
    }

    private static void unsupported(LootTable table) {
        TheLongTravail.LOGGER.warn("特殊来源表 {} 已被改为非单轮固定权重结构，保留该表并跳过精粹注入", table.getLootTableId());
    }

    private static ResourceLocation vanilla(String path) {
        return new ResourceLocation("minecraft", "chests/" + path);
    }

    private static ResourceLocation material(String path) {
        return new ResourceLocation("the_long_travail", "materials/" + path);
    }

    public static void inject(LootTable table) {
        if (table.getLootTableId() == null || table.getPool(POOL) != null) return;
        var id = table.getLootTableId();
        var rule = SPECIAL_REWARDS.get(id);
        if (rule != null) {
            injectSpecial(table, rule);
            return;
        }
        var extra = ESSENCE_TABLES.getOrDefault(id, TABLES.get(id));
        if (extra == null) return;
        table.addPool(
                LootPool.lootPool()
                        .name(POOL)
                        .setRolls(ConstantValue.exactly(1))
                        .setBonusRolls(ConstantValue.exactly(0))
                        .add(LootTableReference.lootTableReference(extra))
                        .build());
    }

    private MaterialLoot() {}
}

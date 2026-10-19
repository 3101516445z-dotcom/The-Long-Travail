package com.thelongtravail.loot;

import com.google.gson.*;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.*;

public final class SingleRollLootInjector {
    public record Reward(ResourceLocation item, double chance) {}

    public static boolean inject(LootTable table, String name, List<Reward> rewards) {
        if (table.getPool(name) != null) return true;
        var gson = Deserializers.createLootTableSerializer().create();
        var json = gson.toJsonTree(table, LootTable.class).getAsJsonObject();
        var pools = json.getAsJsonArray("pools");
        if (pools == null || pools.size() != 1 || !empty(json, "functions")) return false;
        var pool = pools.get(0).getAsJsonObject();
        if (!pool.has("name")
                || pool.get("name").isJsonNull()
                || !number(pool, "rolls", 1)
                || !number(pool, "bonus_rolls", 0)
                || !empty(pool, "conditions")
                || !empty(pool, "functions")) return false;
        var entries = pool.getAsJsonArray("entries");
        if (entries == null || entries.isEmpty()) return false;
        long total = 0;
        for (var element : entries) {
            var e = element.getAsJsonObject();
            String type = e.get("type").getAsString();
            if (!Set.of("minecraft:item", "minecraft:loot_table", "minecraft:empty").contains(type)
                    || !empty(e, "conditions")
                    || !number(e, "quality", 0)) return false;
            int weight = e.has("weight") ? e.get("weight").getAsInt() : 1;
            if (weight <= 0) return false;
            total += weight;
        }
        long nativeWeight = 1_000_000;
        List<Long> weights = new ArrayList<>();
        for (var reward : rewards) {
            if (!Double.isFinite(reward.chance) || reward.chance < 0 || reward.chance > 1) return false;
            long weight = Math.round(reward.chance * 1_000_000);
            weights.add(weight);
            nativeWeight -= weight;
        }
        if (nativeWeight < 0) return false;
        long divisor = nativeWeight;
        for (long weight : weights) divisor = gcd(divisor, weight);
        if (divisor == 0) return false;
        nativeWeight /= divisor;
        long sum = nativeWeight;
        for (int i = 0; i < weights.size(); i++) {
            weights.set(i, weights.get(i) / divisor);
            sum += weights.get(i);
        }
        if (total <= 0 || sum <= 0 || total > Integer.MAX_VALUE / sum) return false;
        JsonArray rebuilt = new JsonArray();
        if (nativeWeight > 0)
            for (var element : entries) {
                var e = element.getAsJsonObject().deepCopy();
                e.addProperty("weight", (e.has("weight") ? e.get("weight").getAsInt() : 1) * nativeWeight);
                rebuilt.add(e);
            }
        for (int i = 0; i < rewards.size(); i++)
            if (weights.get(i) > 0) {
                var e = new JsonObject();
                e.addProperty("type", "minecraft:item");
                e.addProperty("name", rewards.get(i).item.toString());
                e.addProperty("weight", total * weights.get(i));
                rebuilt.add(e);
            }
        var builder =
                LootPool.lootPool()
                        .name(name)
                        .setRolls(ConstantValue.exactly(1))
                        .setBonusRolls(ConstantValue.exactly(0));
        for (var element : rebuilt) builder.add(new Loaded(gson.fromJson(element, LootPoolEntryContainer.class)));
        LootPool replacement = builder.build();
        table.removePool(pool.get("name").getAsString());
        table.addPool(replacement);
        return true;
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long r = a % b;
            a = b;
            b = r;
        }
        return a;
    }

    private static boolean empty(JsonObject j, String key) {
        return !j.has(key) || j.getAsJsonArray(key).isEmpty();
    }

    private static boolean number(JsonObject j, String key, int value) {
        if (!j.has(key)) return value == 0;
        var e = j.get(key);
        return e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() && e.getAsDouble() == value;
    }

    private static final class Loaded extends LootPoolEntryContainer.Builder<Loaded> {
        private final LootPoolEntryContainer entry;

        Loaded(LootPoolEntryContainer entry) {
            this.entry = entry;
        }

        @Override
        protected Loaded getThis() {
            return this;
        }

        @Override
        public LootPoolEntryContainer build() {
            return entry;
        }
    }

    private SingleRollLootInjector() {}
}

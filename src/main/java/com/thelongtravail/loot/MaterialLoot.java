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
                    source("chests/stronghold_library"), material("essences/chests/stronghold_library"));

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

    private static Map.Entry<ResourceLocation, SpecialReward> special(String path, String aspect, double percent) {
        return Map.entry(
                source(path),
                new SpecialReward(
                        ResourceLocation.fromNamespaceAndPath("the_long_travail", aspect + "_essence"), percent));
    }

    private static void injectSpecial(LootTable table, SpecialReward rule) {
        var rewards = new java.util.ArrayList<SingleRollLootInjector.Reward>();
        rewards.add(new SingleRollLootInjector.Reward(rule.item(), rule.percent() / 100));
        if (source("gameplay/fishing/treasure").equals(table.getLootTableId()))
            rewards.add(
                    new SingleRollLootInjector.Reward(
                            sourceItem("perfect_fluid"), UnifiedLoot.fluidFishingChance * 20));
        if (table.getLootTableId().getPath().startsWith("archaeology/"))
            rewards.add(new SingleRollLootInjector.Reward(sourceItem("don_quixote"), .05));
        if (source("archaeology/trail_ruins_rare").equals(table.getLootTableId()))
            rewards.add(new SingleRollLootInjector.Reward(sourceItem("phosphophyllite"), .05));
        if (!SingleRollLootInjector.inject(table, POOL, rewards)) unsupported(table);
    }

    private static ResourceLocation sourceItem(String path) {
        return new ResourceLocation("the_long_travail", path);
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

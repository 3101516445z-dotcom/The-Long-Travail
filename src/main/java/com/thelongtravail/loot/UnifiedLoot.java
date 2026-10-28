package com.thelongtravail.loot;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.thelongtravail.config.ConfigFileIO;
import com.thelongtravail.config.ConfigProblems;
import com.thelongtravail.config.ConfigValidation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.*;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public final class UnifiedLoot {
    public static volatile double fishingTreasureChance = .1;
    private static double lastTreasureChance = .1;
    private static boolean enabled = true;
    private static boolean loaded;
    public static final String POOL = "the_long_travail:shared_items";

    public record Entry(Item item, int weight, int min, int max) {}

    public record Rule(float chance, List<Entry> items) {}

    private static volatile Map<ResourceLocation, Rule> rules = Map.of();

    public static synchronized void reload() {
        var path = FMLPaths.CONFIGDIR.get().resolve("the_long_travail/general.toml");
        try {
            var config = ConfigFileIO.read(path);
            Object rawEnabled = config.get("loot.enabled");
            if (rawEnabled instanceof Boolean value) enabled = value;
            else
                ConfigProblems.report(
                        path,
                        "loot.enabled: 必须为布尔值，保留有效开关 / Expected a boolean; kept effective setting");
            Object rawTreasure = config.get("loot.fishing_treasure.itemChance");
            try {
                double chance = ConfigValidation.number(rawTreasure).doubleValue();
                if (chance < 0 || chance > .2)
                    throw new IllegalArgumentException("必须为 0~0.2 / Expected 0 to 0.2");
                lastTreasureChance = chance;
            } catch (IllegalArgumentException failure) {
                ConfigProblems.report(
                        path,
                        "loot.fishing_treasure.itemChance: "
                                + failure.getMessage()
                                + "; 保留有效值（首次为默认值） / Kept effective value (default on first load)");
            }
            fishingTreasureChance = enabled ? lastTreasureChance : 0;
            if (!enabled) {
                rules = Map.of();
            } else {
                Object raw = config.get("loot.tables");
                if (raw instanceof UnmodifiableConfig tables) {
                    rules =
                            parseTables(
                                    tables, rules, message -> ConfigProblems.report(path, message));
                } else {
                    ConfigProblems.report(
                            path,
                            "loot.tables: 配置分组无效 / Invalid section; "
                                    + (loaded
                                            ? "保留有效规则 / kept effective rules"
                                            : "共享奖励未启用 / shared rewards disabled"));
                }
            }
            loaded = true;
        } catch (com.electronwill.nightconfig.core.io.ParsingException
                | java.io.UncheckedIOException failure) {
            ConfigProblems.report(
                    path,
                    "战利品配置无法读取 / Cannot read loot configuration: "
                            + failure.getMessage()
                            + (loaded
                                    ? "; 保留有效规则 / kept effective rules"
                                    : "; 共享奖励未启用，钓鱼使用默认值 / shared rewards disabled; fishing uses"
                                            + " default"));
        }
    }

    static Map<ResourceLocation, Rule> parseTables(
            UnmodifiableConfig tables,
            Map<ResourceLocation, Rule> previous,
            java.util.function.Consumer<String> problems) {
        Map<ResourceLocation, Rule> result = new LinkedHashMap<>();
        for (var table : tables.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(table.getKey());
            try {
                Rule rule = parseTable(table.getKey(), table.getValue());
                if (rule != null) result.put(id, rule);
            } catch (IllegalArgumentException failure) {
                Rule old = id == null ? null : previous.get(id);
                if (old != null) result.put(id, old);
                problems.accept(
                        failure.getMessage()
                                + (old == null
                                        ? "; 本表奖励已停用 / rewards from this table disabled"
                                        : "; 保留本表上次有效规则 / kept this table's previous rules"));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public static Map<ResourceLocation, Rule> parse(UnmodifiableConfig config) {
        Object enabled = config.get("loot.enabled");
        if (!(enabled instanceof Boolean)) throw new IllegalArgumentException("loot.enabled必须是布尔值");
        if (Boolean.FALSE.equals(enabled)) return Map.of();
        Object raw = config.get("loot.tables");
        if (!(raw instanceof UnmodifiableConfig tables))
            throw new IllegalArgumentException("缺少loot.tables");
        Map<ResourceLocation, Rule> result = new LinkedHashMap<>();
        for (var table : tables.entrySet()) {
            Rule rule = parseTable(table.getKey(), table.getValue());
            if (rule != null) result.put(new ResourceLocation(table.getKey()), rule);
        }
        return Collections.unmodifiableMap(result);
    }

    private static Rule parseTable(String key, Object raw) {
        String label = "loot.tables.\"" + key + "\"";
        ResourceLocation id = ResourceLocation.tryParse(key);
        if (id == null || !key.contains(":"))
            throw new IllegalArgumentException(label + "：战利品表ID无效");
        if (!(raw instanceof UnmodifiableConfig value))
            throw new IllegalArgumentException(label + "：必须为配置分组");
        Object probability = value.get("chance");
        Number number;
        try {
            number = ConfigValidation.number(probability);
            if (number.doubleValue() < 0 || number.doubleValue() > 1)
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(label + ".chance：必须为0~1的有限数值", failure);
        }
        if (number.doubleValue() == 0) return null;
        Object itemList = value.get("items");
        if (!(itemList instanceof List<?> list))
            throw new IllegalArgumentException(label + ".items：必须为列表");
        List<Entry> entries = new ArrayList<>();
        long total = 0;
        for (int index = 0; index < list.size(); index++) {
            Object line = list.get(index);
            try {
                if (!(line instanceof String text)) throw new IllegalArgumentException("必须是字符串");
                String[] parts = text.split("\\|", -1);
                if (parts.length != 4) throw new IllegalArgumentException("格式为物品ID|权重|最小数量|最大数量");
                var itemId = ResourceLocation.tryParse(parts[0].trim());
                if (itemId == null
                        || !parts[0].contains(":")
                        || !ForgeRegistries.ITEMS.containsKey(itemId))
                    throw new IllegalArgumentException("物品ID不存在");
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                int weight = Integer.parseInt(parts[1].trim());
                int min = Integer.parseInt(parts[2].trim()),
                        max = Integer.parseInt(parts[3].trim());
                if (item == Items.AIR || weight <= 0 || min < 1 || max < min || max > 2304)
                    throw new IllegalArgumentException(
                            "权重须为正整数，数量须满足1 <= 最小 <= 最大 <= 2304，物品不能为空气");
                total += weight;
                if (total > Integer.MAX_VALUE) throw new IllegalArgumentException("总权重超出整数范围");
                entries.add(new Entry(item, weight, min, max));
            } catch (RuntimeException failure) {
                throw new IllegalArgumentException(
                        label + ".items[" + index + "] " + line + "：" + failure.getMessage(),
                        failure);
            }
        }
        return entries.isEmpty() ? null : new Rule(number.floatValue(), List.copyOf(entries));
    }

    public static void inject(LootTable table) {
        if (table.getLootTableId() == null) return;
        Rule rule = rules.get(table.getLootTableId());
        if (rule == null || table.getPool(POOL) != null) return;
        var pool =
                LootPool.lootPool()
                        .name(POOL)
                        .setRolls(ConstantValue.exactly(1))
                        .setBonusRolls(ConstantValue.exactly(0))
                        .when(LootItemRandomChanceCondition.randomChance(rule.chance()));
        for (Entry entry : rule.items()) {
            pool.add(
                    LootItem.lootTableItem(entry.item())
                            .setWeight(entry.weight())
                            .apply(
                                    SetItemCountFunction.setCount(
                                            UniformGenerator.between(entry.min(), entry.max()))));
        }
        table.addPool(pool.build());
    }

    static synchronized void reset() {
        rules = Map.of();
        lastTreasureChance = .1;
        fishingTreasureChance = .1;
        enabled = true;
        loaded = false;
    }

    private UnifiedLoot() {}
}

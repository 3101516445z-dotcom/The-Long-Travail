package com.thelongtravail.loot;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.thelongtravail.TheLongTravail;
import com.thelongtravail.config.ConfigFileIO;
import java.util.*;
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

public final class UnifiedLoot {
    public static final String POOL = "the_long_travail:shared_items";
    public record Entry(Item item, int weight, int min, int max) {}
    public record Rule(float chance, List<Entry> items) {}
    private static volatile Map<ResourceLocation, Rule> rules = Map.of();

    // 在战利品表反序列化前调用一次，包括 /reload；配置文件监听器不调用此方法。
    public static void reload() {
        var path = FMLPaths.CONFIGDIR.get().resolve("the_long_travail/general.toml");
        try {
            rules = parse(ConfigFileIO.read(path));
        } catch (RuntimeException failure) {
            TheLongTravail.LOGGER.error("苦旅战利品配置读取失败，保留上次有效规则：{}", path, failure);
        }
    }

    // 完整校验快照后再发布，避免因忽略无效条目而改变权重分布。
    public static Map<ResourceLocation, Rule> parse(UnmodifiableConfig config) {
        Object enabled = config.get("loot.enabled");
        if (!(enabled instanceof Boolean)) throw new IllegalArgumentException("loot.enabled必须是布尔值");
        if (Boolean.FALSE.equals(enabled)) return Map.of();
        Object raw = config.get("loot.tables");
        if (!(raw instanceof UnmodifiableConfig tables)) throw new IllegalArgumentException("缺少loot.tables");
        Map<ResourceLocation, Rule> result = new LinkedHashMap<>();
        for (var table : tables.entrySet()) {
            String label = "loot.tables.\"" + table.getKey() + "\"";
            ResourceLocation id = ResourceLocation.tryParse(table.getKey());
            if (id == null || !table.getKey().contains(":")) throw new IllegalArgumentException(label + "：战利品表ID无效");
            if (!(table.getValue() instanceof UnmodifiableConfig value)) throw new IllegalArgumentException(label + "：必须为配置分组");
            Object probability = value.get("chance");
            if (!(probability instanceof Number number) || !Double.isFinite(number.doubleValue())
                    || number.doubleValue() < 0 || number.doubleValue() > 1)
                throw new IllegalArgumentException(label + ".chance：必须为0~1的有限数值");
            Object itemList = value.get("items");
            if (!(itemList instanceof List<?> list)) throw new IllegalArgumentException(label + ".items：必须为列表");
            List<Entry> entries = new ArrayList<>();
            long total = 0;
            for (int index = 0; index < list.size(); index++) {
                Object line = list.get(index);
                try {
                    if (!(line instanceof String text)) throw new IllegalArgumentException("必须是字符串");
                    String[] parts = text.split("\\|", -1);
                    if (parts.length != 4) throw new IllegalArgumentException("格式为物品ID|权重|最小数量|最大数量");
                    var itemId = ResourceLocation.tryParse(parts[0].trim());
                    if (itemId == null || !parts[0].contains(":") || !ForgeRegistries.ITEMS.containsKey(itemId))
                        throw new IllegalArgumentException("物品ID不存在");
                    Item item = ForgeRegistries.ITEMS.getValue(itemId);
                    int weight = Integer.parseInt(parts[1].trim());
                    int min = Integer.parseInt(parts[2].trim()), max = Integer.parseInt(parts[3].trim());
                    if (item == Items.AIR || weight <= 0 || min < 1 || max < min || max > 2304)
                        throw new IllegalArgumentException("权重须为正整数，数量须满足1 <= 最小 <= 最大 <= 2304，物品不能为空气");
                    total += weight;
                    if (total > Integer.MAX_VALUE) throw new IllegalArgumentException("总权重超出整数范围");
                    entries.add(new Entry(item, weight, min, max));
                } catch (RuntimeException failure) {
                    throw new IllegalArgumentException(label + ".items[" + index + "] " + line + "：" + failure.getMessage(), failure);
                }
            }
            if (number.doubleValue() > 0 && !entries.isEmpty())
                result.put(id, new Rule(number.floatValue(), List.copyOf(entries)));
        }
        return Collections.unmodifiableMap(result);
    }

    public static void inject(LootTable table) {
        if (table.getLootTableId() == null) return;
        Rule rule = rules.get(table.getLootTableId());
        if (rule == null || table.getPool(POOL) != null) return;
        var pool = LootPool.lootPool().name(POOL).setRolls(ConstantValue.exactly(1))
                .setBonusRolls(ConstantValue.exactly(0))
                .when(LootItemRandomChanceCondition.randomChance(rule.chance()));
        for (Entry entry : rule.items()) {
            pool.add(LootItem.lootTableItem(entry.item()).setWeight(entry.weight())
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(entry.min(), entry.max()))));
        }
        table.addPool(pool.build());
    }

    private UnifiedLoot() {}
}

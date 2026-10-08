package com.thelongtravail.loot;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

// 将整张映射表作为一个配置值，避免 Forge 校正配置时删除用户新增的战利品表 ID。
public final class UnifiedLootConfig {
    public static void define(ForgeConfigSpec.Builder builder) {
        builder.comment("苦旅共享战利品：每张表每次执行最多选中一个条目。保存后执行/reload或重启生效。\nShared Long Travail loot: each table selects at most one entry per execution. After saving, use /reload or restart for changes to take effect.")
                .define("loot.enabled", true);
        builder.comment("按完整战利品表ID分组，可手动增加原版或模组表。ID须用双引号包围。\nGrouped by full loot table ID; vanilla or modded tables can be added manually. IDs must be enclosed in double quotes.",
                "chance为总概率(0~1)；items格式：物品ID|正整数权重|最小数量|最大数量。\nchance is the overall probability (0 to 1); items format: item_id|positive_integer_weight|min_count|max_count.",
                "触发奖励后，按权重选择一个条目；数量为1~1时只出一件；数量范围1~2304。空列表或概率0关闭本表。\nWhen a reward triggers, select one entry by weight. A quantity range of 1 to 1 gives exactly one item; supported quantities are 1 to 2304. An empty list or a chance of 0 disables rewards from this table.",
                "父表与子表独立抽取；不额外附加抢夺、时运、精准采集或玩家击杀条件。\nParent and child tables roll independently. No additional Looting, Fortune, Silk Touch, or player-kill requirements are imposed.")
                .define("loot.tables", UnifiedLootConfig::defaults, value -> value instanceof UnmodifiableConfig);
    }

    public static CommentedConfig defaults() {
        CommentedConfig tables = CommentedConfig.of(java.util.LinkedHashMap::new, com.electronwill.nightconfig.toml.TomlFormat.instance());
        try (var input = UnifiedLootConfig.class.getResourceAsStream("/the_long_travail/default_loot.json")) {
            if (input == null) throw new IllegalStateException("Missing default_loot.json");
            var json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            json.entrySet().forEach(entry -> {
                var value = entry.getValue().getAsJsonObject();
                CommentedConfig table = CommentedConfig.of(java.util.LinkedHashMap::new, com.electronwill.nightconfig.toml.TomlFormat.instance());
                table.set("chance", value.get("chance").getAsDouble());
                table.setComment("chance", "本表苦旅奖励总概率；0表示不触发奖励，1表示必定触发奖励。添加物品不会自动增加总概率。\nOverall chance for a Long Travail reward from this table; 0 disables rewards, and 1 always triggers a reward. Adding items does not automatically increase the overall chance.");
                var items = new java.util.ArrayList<String>();
                value.getAsJsonArray("items").forEach(item -> items.add(item.getAsString()));
                table.set("items", items);
                table.setComment("items", "物品ID|权重|最小数量|最大数量；触发奖励后，只选择一个条目。\nitem_id|weight|min_count|max_count; select only one entry when a reward triggers.");
                tables.set(List.of(entry.getKey()), table);
            });
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Cannot read loot defaults", failure);
        }
        return tables;
    }

    private UnifiedLootConfig() {}
}

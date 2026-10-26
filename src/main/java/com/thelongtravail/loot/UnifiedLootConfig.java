package com.thelongtravail.loot;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.gson.JsonParser;

import net.minecraftforge.common.ForgeConfigSpec;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

// 将整张映射表作为一个配置值，避免 Forge 校正配置时删除用户新增的战利品表 ID。
public final class UnifiedLootConfig {
    public static void define(ForgeConfigSpec.Builder builder) {
        builder.comment(
                        "启用苦旅共享战利品及钓鱼宝藏物品奖励。\n"
                                + "Enable shared Long Travail loot and fishing treasure item rewards.")
                .define("loot.enabled", true);
        builder.comment(
                        "駅、东海道五十三次和完美流体在钓鱼宝藏子表内各自的概率；0.1 表示各 10%。\n"
                                + "Individual chance of Eki, Tokaido and Perfect Fluid within fishing"
                                + " treasure; 0.1 means 10% each.\n"
                                + "范围 0~0.3，0 禁用这三件奖励；同时受 loot.enabled 控制。\n"
                                + "Range 0 to 0.3; 0 disables these three rewards. Also requires"
                                + " loot.enabled.")
                .defineInRange("loot.fishing_treasure.itemChance", .1, 0, .3);
        builder.comment(
                        "按完整战利品表ID分组，可手动增加原版或模组表。ID须用双引号包围。\n"
                                + "Grouped by full loot table ID; vanilla or modded tables can be added"
                                + " manually. IDs must be enclosed in double quotes.",
                        "chance为总概率(0~1)；items格式：物品ID|正整数权重|最小数量|最大数量。\n"
                                + "chance is the overall probability (0 to 1); items format:"
                                + " item_id|positive_integer_weight|min_count|max_count.",
                        "触发奖励后，按权重选择一个条目；数量为1~1时只出一件；数量范围1~2304。空列表或概率0关闭本表。\n"
                                + "When a reward triggers, select one entry by weight. A quantity range"
                                + " of 1 to 1 gives exactly one item; supported quantities are 1 to"
                                + " 2304. An empty list or a chance of 0 disables rewards from this"
                                + " table.",
                        "父表与子表独立抽取；不额外附加抢夺、时运、精准采集或玩家击杀条件。\n"
                                + "Parent and child tables roll independently. No additional Looting,"
                                + " Fortune, Silk Touch, or player-kill requirements are imposed.")
                .define(
                        "loot.tables",
                        UnifiedLootConfig::defaults,
                        value -> value instanceof UnmodifiableConfig);
    }

    public static CommentedConfig defaults() {
        CommentedConfig tables =
                CommentedConfig.of(
                        java.util.LinkedHashMap::new,
                        com.electronwill.nightconfig.toml.TomlFormat.instance());
        try (var input =
                UnifiedLootConfig.class.getResourceAsStream(
                        "/the_long_travail/default_loot.json")) {
            if (input == null) throw new IllegalStateException("Missing default_loot.json");
            var json =
                    JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                            .getAsJsonObject();
            json.entrySet()
                    .forEach(
                            entry -> {
                                var value = entry.getValue().getAsJsonObject();
                                CommentedConfig table =
                                        CommentedConfig.of(
                                                java.util.LinkedHashMap::new,
                                                com.electronwill.nightconfig.toml.TomlFormat
                                                        .instance());
                                table.set("chance", value.get("chance").getAsDouble());
                                table.setComment(
                                        "chance", "本表触发概率。\nTrigger probability for this table.");
                                var items = new java.util.ArrayList<String>();
                                value.getAsJsonArray("items")
                                        .forEach(item -> items.add(item.getAsString()));
                                table.set("items", items);
                                table.setComment(
                                        "items", "本表物品候选。\nItem candidates for this table.");
                                tables.set(List.of(entry.getKey()), table);
                            });
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Cannot read loot defaults", failure);
        }
        return tables;
    }

    private UnifiedLootConfig() {}
}

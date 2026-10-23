# 战利品数据接入说明

玩家配置操作见[配置说明](../玩家查阅/configuration-controls.md)。

精粹加入考古、钓鱼宝藏、嗅探兽、村庄英雄赠礼、猪灵交易和猫礼物的原表单轮加权池，保持原奖励相对权重和物品函数，每次只取一个结果。无名称池、多池、多轮或带动态条件／品质的表跳过注入并警告。箱子额外抽取材料池，保留原战利品。

箱子与母岩使用下述材料数据表，可由数据包覆盖并 `/reload`，不走饰品奖励配置。`MaterialLoot.SPECIAL_REWARDS` 登记实际原版表 ID、精粹物品和同池概率，不能填写虚构材料子表。JEI 母岩页仅展示默认规则。

## 来源与维护位置

- 可配置饰品/道具奖励：`loot/UnifiedLoot` 及 `src/main/resources/the_long_travail/default_loot.json`，按现有 general.toml 配置流程维护。
- 材料和精粹来源：`loot/MaterialLoot.java` 与 `src/main/resources/data/the_long_travail/loot_tables/materials/`。其中材料、精粹及固定考古奖励不受饰品奖励的 `loot.enabled` 控制；完美流体钓鱼是例外，其概率来自 `UnifiedLoot.fluidFishingChance`，受 `loot.enabled` 与 `loot.perfect_fluid.fishingChance` 控制。
- 注入保留原表，使用唯一池名防重复；材料子表不得再次递归注入。支持数据包覆盖及正常重载，已生成过内容的箱子不会补发。多个奖励属于同一互斥池时不能改成独立池，否则会改变联合概率。

`MaterialLoot.injectSpecial` 还将堂吉诃德加入各考古表，将磷叶石加入稀有古迹废墟表，与精粹共享单轮互斥池。完美流体加入钓鱼宝藏表，配置的无幸运基础概率乘以 20 转为宝藏分支内概率；不能把该换算当作改写数据包后的通用概率公式。新增奖励应合并到同次 `SingleRollLootInjector.inject` 调用，保留整池概率约束。

## 抽样与注入

| 入口 | 接入约定 |
|---|---|
| `loot/WeightedLootSampler` | 使用当前加载的表和 `LootContext`，按基础权重取样；调用方须处理 `Result.complete`、`skipped` 及生成异常。不额外执行全局战利品修饰器，不能据此模拟完整开箱概率。 |
| `loot/SingleRollLootInjector` | 仅对满足结构约束的单池、单轮表注入奖励，保持原条目的相对权重和物品函数。不支持的表保留原样，不强行改写。 |

## 强制终结补偿的联动边界

`valley/ExecutionLoot`、`valley/ExecutionLootTable` 与 `loot/LootItemCollector`；补偿读取服务器当前生效的目标战利品表，而非重新打开 JAR 或按实体 ID 拼接表路径。它识别明确物品及支持的组合/引用条目，按物品去重生成基础物品，不执行原表概率、条件、数量及加工函数，也不重放经验、成就、Boss 结算或全局战利品修改器。

补偿资格和次数由现有处决流程维护；清理重试不可重复发奖，解析或生成异常不能重放部分结果。观察限于同步调用中的成功物品加入；异步发奖、直接塞入背包及非标准物品存储不能通用识别，接入方需单独评估与补偿同时发生的可能性。

标签、动态/自定义条目及代码额外奖励不应假定会被补偿识别。遍历有物品种类、表数量、节点和深度预算；不支持或到达预算时可能只得到部分条目。不得把“强制终结成功”视作原模组完整奖励已结算。

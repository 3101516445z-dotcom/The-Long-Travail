# 战利品数据接入说明

字段格式见[随包配置说明](../../src/main/resources/assets/the_long_travail/config/configuration-guide.zh_cn.txt)。

## 数据位置

- 可配置奖励：`src/main/resources/the_long_travail/default_loot.json`，运行设置位于 `general.toml`，由 `loot/UnifiedLoot` 读取。
- 材料数据表：`src/main/resources/data/the_long_travail/loot_tables/materials/`，可由数据包覆盖并重载。

注入保留原表，使用唯一池名防止重复；材料子表不能递归注入自身。多个奖励属于同一互斥池时，应合并到同次注入调用以保持联合概率。

## 抽样与注入

| 入口 | 接入约定 |
|---|---|
| `loot/WeightedLootSampler` | 使用当前加载的表和 `LootContext`，按基础权重取样；调用方须处理 `Result.complete`、`skipped` 及生成异常。抽样过程不执行全局战利品修饰器。 |
| `loot/SingleRollLootInjector` | 仅对满足结构约束的单池、单轮表注入奖励，保持原条目的相对权重和物品函数。不支持的表保留原样，不强行改写。 |

## 考古抽取后的处理

`general.toml` 的 `compatibility.protectArchaeologyLoot` 默认开启。对声明为 `minecraft:archaeology` 类型的表，`ArchaeologyLootMixin` 跳过 `ForgeHooks.modifyLoot`，保留表内注入、条件、函数与子表抽取。全局修改器的替换、追加、过滤和NBT修改均被跳过；关闭开关后恢复原流程。

按表类型识别，不按模组或表路径识别。该开关只改变 Forge 全局战利品修改器调用，不撤销直接改写表或考古方块的 Mixin。

## 配置错误处理

`UnifiedLoot.reload()` 分别更新总开关、钓鱼宝藏子表单件概率和共享奖励表。`parseTables` 逐表校验并构建规则，错误表整体沿用本次运行的上一份有效规则；首次没有旧规则则停用，其他有效表正常更新。不得只删去非法物品条目后继续抽取，否则会改变剩余条目的相对概率。

数值概率接受合法数字字符串，但仍要求有限且在范围内。删除表、概率为 0 和合法空列表均表示停用，不能因回退逻辑恢复已经删除的规则。钓鱼概率非法时沿用有效值（首次为默认值），不阻断箱子奖励；总开关关闭时两者停止。语法或编码错误保留整份已加载规则，文件交由统一配置流程处理。

所有规则准备在战利品资源重载前完成，仍由 `LootReloadEvents.beforeResourceReload` 驱动；文件监听本身不重新构建战利品表。服务器停止时清理战利品有效状态，避免下一次进入世界沿用上一个世界的规则。

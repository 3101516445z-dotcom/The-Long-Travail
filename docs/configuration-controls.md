# 配置说明

配置位于游戏目录的 `config/the_long_travail/`，首次启动时自动生成。各项设置的含义、默认值和范围，可查看配置文件中的注释，或同目录下的 `配置说明.txt`（英文版为 `Configuration Guide.txt`）。

## 到哪里修改

| 文件 | 主要内容 |
| --- | --- |
| `general.toml` | 初始赠送、探索目标数量、受击恶意、效果与奖励数量限制、Sounds 提示音、战利品 |
| `client.toml` | 名称光晕、视觉效果、技能台词等本机显示设置 |
| `utility_items.toml` | 路引的搜索范围、时限与搜索开销 |
| `aspects/flourishing.toml` | 繁茂主题 |
| `aspects/abyss.toml` | 归墟主题 |
| `aspects/far_reach.toml` | 穷遐主题 |
| `aspects/deep_valley.toml` | 幽谷主题，包括视觉剥夺 |
| `aspects/underworld.toml` | 冥府主题，包括额外钓鱼奖励 |
| `aspects/boundless.toml` | 无垠主题，包括幻翼奖励 |

每份主题配置都包含对应的恶意、见证、探索条件和相关物品设置：

- `travail.malice` / `travail.witness`：恶意与见证。
- `travail.requirements`：可抽取的探索目标。
- `items`：该主题的物品效果。

## 修改后如何生效

保存后重启游戏或服务器，可确保配置生效；饰品与道具战利品设置也可通过 `/reload` 更新。联机时，玩法设置以服务端为准，`client.toml` 只影响本机显示。

修改探索目标池或抽取数量，只影响之后新初始化或重置的旅程；已有旅程、已释放技能和正在进行的冷却不会重新计算。从旧配置目录升级时，需要手动重新调整设置。

## 常用调整

- **效果强度与触发条件**：在对应主题文件中修改。概率通常用 `0`～`1` 表示，例如 `0.5` 为 50%；具体单位和范围以注释为准。
- **名称光晕**：在 `client.toml` 的 `nameEffects` 中调整，可选 `HIGH`、`MEDIUM`、`LOW` 或 `OFF`。
- **路引搜索**：在 `utility_items.toml` 的 `items.wayguide` 中调整。扩大搜索范围、缩小采样间隔会增加搜索开销；搜索失败不代表整个维度都没有目标。
- **大量效果或奖励造成卡顿**：查看 `general.toml` 的 `effectLimits` 和 `rewardDelivery`，限制效果触发次数或分批发放奖励。

## 创造与观察者模式的负面效果

以下配置键控制负面效果是否生效，默认均为 `false`，即豁免这两种模式的玩家；改为 `true` 才使对应负面作用生效，其他触发条件不变。生存与冒险模式不受这些开关影响。

| 文件 | 配置键 | 控制内容 |
| --- | --- | --- |
| `general.toml` | `malice.affectCreativeAndSpectator` | 苦旅的全部恶意惩罚 |
| `aspects/deep_valley.toml` | `items.azrael.selfExecutionAffectsCreativeAndSpectator` | Azrael 处决佩戴者自身 |
| `aspects/deep_valley.toml` | `items.azrael.targetExecutionAffectsCreativeAndSpectator` | Azrael 处决其他玩家 |
| `aspects/boundless.toml` | `items.boundless.star.affectCreativeAndSpectator` | 星之声冻结范围内玩家 |
| `aspects/flourishing.toml` | `items.spring_game.harmfulEffectsAffectCreativeAndSpectator` | 春日的游戏施加中毒、凋零，以及丁香延长负面效果 |
| `aspects/flourishing.toml` | `items.spring_game.retaliationAffectsCreativeAndSpectator` | 春日的游戏的玫瑰丛反伤 |
| `aspects/far_reach.toml` | `items.icarus.ignitionAffectsCreativeAndSpectator` | 伊卡洛斯点燃目标 |

苦旅的模式豁免不会改变旅程进度、恶意／见证状态或饰品联动。进入豁免模式后，现有的僵硬、视觉剥夺与飞行减速会解除。饰品开关按受到负面效果的玩家判断；不会清除已经存在的原版中毒、凋零或燃烧，以免影响其他来源的效果。

## 战利品设置

饰品与道具奖励在 `general.toml` 的 `loot` 中调整，按战利品表 ID 分别设置：

| 配置项 | 含义 |
| --- | --- |
| `enabled` | 饰品与道具奖励池开关，不控制材料与精粹来源 |
| 每张表的 `chance` | 该表抽中苦旅奖池的概率，`0`～`1`，设为 `0` 关闭 |
| 每张表的 `items` | 奖池条目，格式为 `物品ID\|权重\|最小数量\|最大数量` |

每次抽中奖池后，按权重选中一种物品，不替换原版奖励。增加条目只会改变各物品的占比，不会提高奖池的总概率。

保存后执行 `/reload` 或重启；已经生成内容的箱子不会重新抽取。配置有误时可查看日志中的提示，修正后再次加载。更新模组不会自动将新增的默认来源或物品补入已有配置。

日记能力带来的额外钓鱼奖励、幻翼奖励，仍在各自主题文件中调整。

## 材料与精粹来源

三叉戟、鹦鹉螺壳、干海绵及主题精粹不使用上述饰品与道具奖励池配置。材料与精粹的获取途径及本次概率调整见 [0.1.1-fix 更新日志](../更新日志/0.1.1-fix.md)。

考古、钓鱼宝藏、嗅探兽挖掘、村庄英雄赠礼、猪灵交易和猫的晨间礼物均将精粹作为普通物品条目直接加入原表的单轮加权池，不再引用材料子表。按各来源的目标概率扩大原奖励权重，各原有奖励之间的比例及物品函数保持不变，每次仍只抽取一个结果。若数据包将目标表改为无名称池、多池、多轮或带动态条件／品质的结构，则保留该表、跳过注入并记录警告，避免改变其规则。箱子来源继续额外抽取材料池，保留原有战利品。

箱子与母岩奖励由 `data/the_long_travail/loot_tables/materials/` 下的数据表定义，数据包可覆盖并通过 `/reload` 更新，不使用饰品的 `general.toml` 奖励配置。特殊来源在 `MaterialLoot.SPECIAL_REWARDS` 中直接登记原版表 ID、精粹物品与同池概率，不再建立材料子表映射。旧的 `the_long_travail:materials/essences/archaeology/*` 与 `the_long_travail:materials/essences/gameplay/*` 子表已移除；外部数据包或命令若引用这些旧 ID，须同步调整。JEI 母岩页展示默认规则，不随外部数据包改写自动更新。

## 可选性能诊断

默认关闭。排查性能时，可在启动 JVM 参数中加入 `-Dtravail.hotpath.metrics=true`，重启后生效；移除参数并重启即可关闭。

服务器停止时输出两组统计：`Travail hot path metrics` 是累计次数、累计纳秒及账本规模；`Travail hot path peaks` 是单队列记录数峰值、单批候选与恢复数量峰值，以及各阶段单次耗时峰值（纳秒）。队列峰值按单个队列计算，不是所有维度队列之和。该统计不改变处理预算或机制，不在每个 tick 写日志。

`STORAGE_INDEX_SEED_VISITS` 统计每个维度首次使用掉落物索引时的补录扫描；`STORAGE_ENTITY_VISITS` 统计校正时实际遍历的掉落物。`RELEASE_DENSE_BATCHES` 与 `RELEASE_SPARSE_BATCHES` 分别统计按已有插入顺序筛选及按候选排序的批次数。两条路径在同一时刻恢复，均保留原有恢复顺序。

## 旅程到访容差

`general.toml` 的 `[journeyDetection]` 控制结构和群系的到访判定：

| 配置 | 默认 | 范围及用途 |
| --- | --- | --- |
| `structureHorizontalTolerance` | 4 | 0～16格；结构范围沿X/Z方向扩展 |
| `structureVerticalTolerance` | 4 | 0～16格；结构范围沿Y方向扩展 |
| `biomeSampleRadius` | 4 | 0～8格；当前位置未命中需求时，补查四个水平邻点；0关闭补查 |
| `pieceOnlyStructures` | `[]` | 指定结构ID使用部件范围，其余使用整体范围；两者都应用上述容差 |

例如过大的稀疏结构可以加入 `pieceOnlyStructures = ["minecraft:stronghold"]`，避免将部件之间的大片空地算作到访。整体范围采用结构自身提供的包围盒，可能已包含地形适配扩展。

每20游戏刻检查一次，只检查尚未完成的需求。邻近补查不加载新区块；当前位置缺失结构数据仍沿用原有有界异步确认。附近群系可完成需求，但不会写入实际到访群系历史。修改配置后重载或重启以清除缓存。

群系采样结果按维度和精确方块坐标缓存100游戏刻，每维度最多2048个位置，超出后淘汰最近最少使用的条目。结构每20刻的刷新不会强制重采群系。相关区块加载/卸载、配置或数据重载会清理对应缓存；原地动态修改群系而未触发这些事件时，最多100刻后刷新。缓存保存群系ID，日记目标改变时直接重新匹配。检测范围保持当前位置加四个水平邻点，不增加垂直采样。

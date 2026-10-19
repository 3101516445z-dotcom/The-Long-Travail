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

- **駅与东海道的天气条件**：固定为主世界的雨天、雪天或雷暴天气，不能再选择“任意天气”或“仅雷暴”。旧 `items.eki.weather`、`items.tokaido.weather` 会在加载新版配置时自动移除；其他合法设置保留。当地降水与露天要求仍由原有选项控制，唤雨期间也使用同一套被动生效条件。

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
| `aspects/flourishing.toml` | `items.spring_corner.harmfulEffectsAffectCreativeAndSpectator` | 春日一隅施加中毒、凋零，以及丁香延长负面效果 |
| `aspects/flourishing.toml` | `items.spring_corner.retaliationAffectsCreativeAndSpectator` | 春日一隅的玫瑰丛反伤 |
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

三叉戟、鹦鹉螺壳、干海绵及主题精粹不使用上述饰品与道具奖励池配置。材料与精粹的获取途径及本次概率调整见 [0.1.1-fix 更新日志](更新日志zh/0.1.1-fix.md)。

箱子与母岩奖励可以通过数据包修改，并通过 `/reload` 更新，不使用饰品奖励的 `general.toml` 配置。特殊来源的注入可能因其他数据包改变表结构而跳过；JEI 母岩页展示默认规则，不会随外部数据包自动改写。需要制作数据包时参见 [战利品数据接入说明](../开发者查阅/loot-integration.md)。

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


## 完美流体与午夜拉菲

`aspects/abyss.toml` 的 `items.perfect_fluid.itemBlacklist` 填写禁止复制的完整物品 ID，默认空；精确匹配，不检查容器内部物品。拾遗仅在列表非空时显示名称。

`items.midnight_lafite` 可分别调整生命恢复与饱和额外等级、中毒与反胃时长倍率，以及普通午夜峰值、非露天且非晴天的午夜峰值和见证夜间增伤。等级加成和增伤为 0 时禁用对应加成；时长倍率为 1 时不修改。说明同步显示服务器数值。饥饿与漂浮免疫固定保留，等级叠加和已有状态处理沿用静默规则。

完美流体在 `general.toml` 的 `loot.perfect_fluid.fishingChance` 默认 0.001，表示无幸运修正、满足开放水域时每次成功钓鱼基础概率 0.1%。宝藏分支内占 2%，正常受幸运与海之眷顾影响。0 禁用，最大 0.045，用于保留宝藏中的归墟精粹份额；遵循 `loot.enabled`，修改后执行 `/reload` 或重启。

午夜拉菲默认加入沉船补给箱、林地府邸各 10%，古城冰箱 17.5%，每次一件，使用既有 `loot.tables`。已有配置需要补入新增来源，不会因更新源码自动覆盖。


### 冥府新增饰品

`aspects/underworld.toml` 中 `items.relics.king` 配置第六天魔王的燃烧增伤、吸血、亡灵减伤、霸主及环境增伤；`items.relics.banner` 配置万魂幡容量、每魂必杀与见切系数、见切上限。收益系数为0时禁用该项。降低容量仅限制有效魂数，不删除已经保存的累计值；恢复容量后可重新使用原有魂数。实际生效值与物品详情使用服务端配置。

第六天魔王出货率由 `general.toml` 共享战利品配置管理。默认要塞总概率21%、三个等权条目；堡垒四类箱子各总概率14%、两个等权条目，单件均为7%。已有配置不会因为源码默认值改变而自动加入新物品，部署时需合并对应条目并调整总概率。

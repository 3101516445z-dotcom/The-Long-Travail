# 配置说明

配置位于游戏目录的 `config/the_long_travail/`，首次启动时自动生成。各项设置的含义、默认值和范围，可查看配置文件中的注释，或同目录下的 `配置说明.txt`（英文版为 `Configuration Guide.txt`）。

随配置生成的说明书按“配置写法 → 预设 → 其他机制”排列。注释中的 `[C04]`、`[P01]` 等编号对应说明书章节；在文本编辑器中同时按 `Ctrl` 和 `F`，输入编号即可查找。基础写法与单位见 `[C04]`，各类列表格式见 `[C05]`，预设的含义见 `[P00]`。

主题配置先列群系池、结构池，再列主题能力和物品；联动预设处理记录集中在文件末尾，同类型归为一组，群系与结构记录在前。点号键保留原有层级，例如 `[travail.witness.compatInitialization]` 下的 `goety.positiveEffectsAdded` 仍是同一配置路径。

预设记录的 `true` 表示已处理或手动跳过补入，`false` 表示等待处理，均不是功能开关。普通玩家通常无需修改；整合包作者重试、跳过或管理已有条目时参阅 `[P01]`。完美流体的处理时机及物品注册条件另见 `[P08]`。

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

修改探索目标池或抽取数量，只影响之后新初始化或重置的旅程；已有旅程、已释放技能和正在进行的冷却不会重新计算。旧目录中的设置不会自动整体迁入；当前目录中的合法设置会保留，缺失项按配置定义补齐。末地城奖池、完美流体联动黑名单与联动探索预设另有下述一次性处理。

## 联动群系预设

世界加载时，根据实际注册的群系识别联动；Terralith 与 Incendium 的模组版、数据包版均可使用。只加入当前存在且对应池内没有的预设 ID，使用 `@翻译键`，权重统一为 `1`。已有条目的名称、权重和顺序保留，译名由模组或资源包提供。

| 联动 | 主题与候选数量 | 初始化标记 |
| --- | --- | --- |
| Terralith | 繁茂 16、归墟 10、穷遐 19、幽谷 9、冥府 4、无垠 7 | `terralithBiomesAdded` |
| Incendium | 冥府 8 | `incendiumBiomesAdded` |
| Alex 的洞穴 | 繁茂 2、归墟 1、幽谷 2、冥府 1 | `alexsCavesBiomesAdded` |

各标记位于对应主题文件的 `travail.requirements.compatInitialization`，按联动和主题独立记录，无对应预设的主题不生成该联动标记。检测到联动并处理后设为 `true`，即使新增数量为零也会记录；未检测到时不标记。标记存在期间不会因手动删除、换世界或预设更新而再次补入。

需要重新补充时，将对应标记设为 `false` 或删除该键，再重新加载世界；也可预先设为 `true` 跳过初始化。标记由使用同一份配置的存档共用。预设不调整结构池和抽取数量，只影响之后新初始化或重置的旅程。

## 原版结构默认池

新配置的六个主题各有两个原版结构，权重均为 1；general.toml 的 requirements.structureDrawCount 默认为 2。

| 主题 | 默认结构（minecraft 命名空间） |
| --- | --- |
| 繁茂 | 平原村庄（village_plains）、林地府邸（mansion） |
| 归墟 | 海底神殿（monument）、海洋废弃传送门（ruined_portal_ocean） |
| 穷遐 | 掠夺者前哨站（pillager_outpost）、沙漠村庄（village_desert） |
| 幽谷 | 废弃矿井（mineshaft）、远古城市（ancient_city） |
| 冥府 | 下界要塞（fortress）、堡垒遗迹（bastion_remnant） |
| 无垠 | 要塞（stronghold）、末地城（end_city） |

抽取不重复：只有默认两项时，两项都会成为目标；联动补入更多候选后，从扩展池中按权重抽取两项。已有配置中的空池、抽取数及自定义条目保留，不自动改为新默认值；需要采用默认组合时，手动填写上述结构并将抽取数设为 2。已有旅程不会重新抽取。

## 联动结构预设

结构池采用相同的一次性补充规则，按世界中实际注册的结构识别来源，支持通过模组或数据包提供结构。当前接入以下 14 个模组、82 个候选结构，新条目使用 `@structure.<命名空间>.<路径>`，权重为 `1`；已有同 ID 设置保留。

| 联动 | 主题与候选数量 | 结构初始化标记 |
| --- | --- | --- |
| Terralith | 繁茂 8、归墟 1、穷遐 3、幽谷 5、冥府 1 | `terralithStructuresAdded` |
| Goety Awaken | 繁茂 2、归墟 1、穷遐 4、幽谷 1、冥府 1 | `goetyAwakenStructuresAdded` |
| Towns and Towers | 归墟 7、无垠 1 | `townsAndTowersStructuresAdded` |
| Goety | 繁茂 1、穷遐 4、幽谷 1、冥府 1、无垠 2 | `goetyStructuresAdded` |
| Incendium | 冥府 5、无垠 1 | `incendiumStructuresAdded` |
| Iron's Spells and Spellbooks | 繁茂 1、归墟 1、穷遐 2、幽谷 1、冥府 1、无垠 1 | `ironsSpellbooksStructuresAdded` |
| Touhou Little Maid: Spell | 繁茂 2、冥府 1 | `maidSpellStructuresAdded` |
| Ars Nouveau | 幽谷 3 | `arsNouveauStructuresAdded` |
| Aquamirae | 归墟 2 | `aquamiraeStructuresAdded` |
| Mowzie's Mobs | 繁茂 1、穷遐 1、幽谷 1 | `mowziesMobsStructuresAdded` |
| T.O Magic and Extras | 冥府 1 | `travelOpticsStructuresAdded` |
| 灾变 | 归墟 2、穷遐 2、幽谷 1、冥府 2、无垠 1 | `cataclysmStructuresAdded` |
| 传奇怪物 | 繁茂 1、无垠 2 | `legendarymonstersStructuresAdded` |
| EEEAB 生物 | 幽谷 1、冥府 1 | `eeeabsmobsStructuresAdded` |

各标记同样位于对应主题文件的 `travail.requirements.compatInitialization`，只在有预设的主题中生成；结构标记与群系标记独立，群系已初始化不会阻止结构补充。检测到对应结构命名空间后处理一次，即使没有新增也记录完成；删除条目不会触发补回。将相应标记设为 `false` 或删除该键后重新打开世界，可再尝试一次。

新生魔艺、灾变、Iron、Goety、万法皆通、传奇怪物、EEEAB 和 Mowzie 的结构由 TLT 补充五种语言的显示文本；其余结构仍可使用外部语言资源，缺少译文时显示结构 ID。

预设只扩充候选池，不修改 `general.toml` 的 `requirements.structureDrawCount`。新配置的默认值为 `2`，已有配置值保留；已有旅程不重新抽取。

## 见证奖励池与联动预设

冥府钓鱼物品奖励使用 `aspects/underworld.toml` 的 `travail.witness.fishing.fishingSpecialItems`；无垠幻翼奖励使用 `aspects/boundless.toml` 的 `travail.witness.phantom.phantomDrops`。条目格式为 `物品ID|权重|最小数量|最大数量`，没有独立名称字段。

两个池的原版默认奖励分别维护，目前均为以下 12 项。每次选中一种物品，再在数量范围内随机；权重表示池内相对占比。

| 原版物品 | 权重 | 数量 |
| --- | ---: | --- |
| 苹果、绿宝石 | 各 6 | 1～5 |
| 铁锭、金锭、钻石、鹦鹉螺壳 | 各 6 | 1～3 |
| 附魔金苹果 | 3 | 1 |
| 下界合金锭 | 2 | 1 |
| 下界之星、龙蛋 | 各 1 | 1 |
| 下界合金碎片、回响碎片 | 各 3 | 1～3 |

默认值用于新建或缺失的配置项，已有完整列表不会自动替换成新默认值。世界启动时，以下模组的已注册物品会按一次性规则补入两个池，保持各自预设权重，数量均为 1～3：

| 联动命名空间 | 每池候选数 | 权重 |
| --- | ---: | --- |
| `l2complements` | 17 | 2 |
| `curseofpandora` | 16 | 2 |
| `celestial_core` | 8 | 3 |
| `irons_spellbooks` | 3 | `raw_mithril`、`ancient_knowledge_fragment` 为 3；`legendary_ink` 为 2 |
| `apprenticecodex` | 2 | 2 |
| `enigmaticlegacy` | 1 | 3 |
| `enigmaticaddons` | 1 | 3 |
| `enigmaticdelicacy` | 1 | 3 |

钓鱼和幻翼的联动候选也分别维护，目前各有 8 个模组、49 个候选物品；全部存在时，新配置每池共 61 项。今后可以分别调整物品、权重与数量。ROR 不加入预设，已有手动 ROR 条目保留。已有同 ID 配置优先，不覆盖权重、数量和顺序；仅补入当前实际注册的缺失 ID。

各主题的 `travail.witness.compatInitialization.<命名空间>` 下分别使用 `fishingItemsAdded`、`phantomDropsAdded`，与群系、结构标记独立。检测到来源命名空间后处理一次，即使零新增也设为 `true`；未检测到则保持未处理。完成后手动删除、清空池或更换存档不会自动补回。将对应标记设为 `false` 或删除该键，再重新打开世界可重试；预先设为 `true` 可跳过注入。标记随配置保存，使用同一份配置的存档共用。

联动补充只改变池内候选与占比，不修改钓鱼奖励触发概率、幻翼检测与击杀规则、奖励发放限制；钓出生物池另行配置，不受本次预设影响。

## 钓出生物默认配置池与联动预设

冥府 `aspects/underworld.toml` 的 `travail.witness.fishing.fishingEntities` 使用 `实体ID|权重`，每次选中一种实体，在鱼钩处生成。原版默认配置池为猪、牛、羊各权重 100，村民、流浪商人、猫各 50，狐狸、美西螈、海龟各 25，共 9 项。新建或缺失的配置项使用这些默认值；已有合法列表保留。

世界启动时按实体注册表补充以下联动预设，仅追加当前存在且池中没有的 ID：

| 联动 | 候选实体 | 权重 |
| --- | --- | ---: |
| 妖怪们的飨宴 `youkaisfeasts` | 鹿、螃蟹、八目鳗、金枪鱼、野猪 | 各 40 |
| 妖怪们的归家 `youkaishomecoming` | 鹿、螃蟹、八目鳗、金枪鱼、野猪 | 各 40 |
| 新生魔艺 `ars_nouveau` | 星宝石兽、风转草、德格米 | 各 25 |
| Ars Elemental `ars_elemental` | Siren | 25 |
| 车万女仆 `touhou_little_maid` | 女仆妖精 | 15 |

共 5 个来源、15 项候选；归家与飨宴各有独立的 5 项，按完整 ID 分别处理。其他来源齐全、两者只安装其一时，新配置共 19 项；两个命名空间的候选全部存在时共 24 项。亚小妹 `starfantasy_goety:apollyon` 不自动加入，已有手动条目保留。已有同 ID 的权重与顺序优先。

各联动在 `travail.witness.compatInitialization.<命名空间>.fishingEntitiesAdded` 独立记录，与钓鱼物品、幻翼奖励及探索池标记分开。来源缺席不标记；处理后即使零新增也设为 `true`，手动删除或清空后不再补回。设为 `false` 或删除对应标记后重新打开世界可重试，预先设为 `true` 可跳过初始化；使用同一配置的存档共用标记。

生物联动只补充候选，不改变 `fishingEntityChance`、`fishingSpecialItemChance` 或现有实体生成方式，也不修改两个物品奖励池。

## 增益效果联动预设

`aspects/far_reach.toml` 与 `aspects/deep_valley.toml` 各自的 `travail.witness.positiveEffects` 使用 `效果ID|最高等级|权重`。两个池的默认值、联动预设和初始化记录分别维护。

原版默认值保留：跳跃提升 `5|5`、生命恢复 `5|3`、抗性提升 `4|2`、急迫 `3|2`、力量 `3|2`，后两列依次为最高等级和权重。已有合法配置保持原值。

| 来源命名空间 | 每池候选数 |
| --- | ---: |
| `goety` | 9 |
| `more_potion_effects`、`youkaisfeasts`、`youkaishomecoming` | 各 5 |
| `irons_spells_delight` | 3 |
| `goetyawaken`、`cataclysm` | 各 2 |
| `enigmaticaddons`、`irons_spellbooks`、`alexsmobs`、`attributeslib`、`ars_nouveau` | 各 1 |

共 12 个来源、每池 36 项联动候选，权重均为 1，最高等级按各效果预设。归家与飨宴各提供 `craby|2|1`、`enjoyable|2|1`、`earthy|1|1`、`breathing|2|1`、`refreshing|1|1`，使用各自命名空间。其他来源齐全、两者只安装其一时，新配置每池共 36 项；两个命名空间的候选全部可用时每池共 41 项。AD（`astral_dimension`）、`esoteric_spell`、`projectile_damage`、`gtbcs_geomancy_plus` 不自动加入，已有手动条目保留。

加载世界时按效果注册表的命名空间检测来源，只补入已注册、属于增益类型且本池缺少的 ID；已有同 ID 的最高等级、权重和顺序优先。各主题文件中的 `travail.witness.compatInitialization.<命名空间>.positiveEffectsAdded` 独立记录处理状态。

来源缺席不标记；来源存在时，即使没有新增项也标记为 `true`。手动删除、清空列表或更换存档不会再次补入；设为 `false` 或删除标记并重新打开世界可重试，预先设为 `true` 可跳过。使用同一配置的存档共用标记。

联动只补充效果候选，沿用原有效果触发、持续时间与等级处理规则；穷遐等级加成的上限表仍取两个效果池中同 ID 的较高上限。

归家与飨宴的来源检测、完整 ID 和初始化标记相互独立，不替换或迁移已有条目。飨宴的初始化记录不会阻止后来安装归家的预设补充。

## 黄金时代联动黑名单

`aspects/far_reach.toml` 中，`items.golden_age.foodBlacklist` 排除黄金时代可产出的食物，`items.golden_age.enchantmentBlacklist` 排除可选附魔。两个列表的基础默认值为空；首次检测到以下模组时，分别补入对应黑名单。

| 模组 | 黑名单 | 自动补入 |
| --- | --- | --- |
| Enigmatic Legacy | 食物 | 天体果实 `enigmaticlegacy:astral_fruit` |
| Enigmatic Delicacy | 食物 | 深渊乱炖 `enigmaticdelicacy:abyssal_stew` |
| L2 Complements | 附魔 | 弹射物免疫 `projectile_reject`、火焰免疫 `fire_reject`、环境免疫 `environment_reject`、爆炸免疫 `explosion_reject`、魔法免疫 `magic_reject`、伤害免疫（创造）`invincible`，均使用 `l2complements:` 前缀 |
| Celestial Enchantments | 附魔 | 死亡刀刃 `celestial_enchantments:death_blade`、星月庇护 `celestial_enchantments:celestial_shelter` |

加载世界时检查模组是否安装，并分别检查物品、附魔注册表；仅补入已注册且列表中缺少的 ID，保留手动条目与顺序。食物和附魔分别使用 `items.golden_age.compatInitialization.<模组ID>.foodBlacklistAdded`、`enchantmentBlacklistAdded` 记录处理状态，与见证奖励及效果池的标记独立。

模组未安装不标记；已安装时即使零新增也标记为 `true`。之后删除条目、清空黑名单或更换存档均不会自动补回；将对应标记设为 `false` 或删除后重新打开世界，可再次尝试。预先设为 `true` 可跳过，使用同一配置的存档共用标记。

黑名单仅作用于黄金时代的产出候选，不禁止通过其他途径获取或使用这些物品和附魔。其他食物筛选条件与附魔等级规则保持原有配置。

## 配置写错时如何处理

配置使用 UTF-8 编码，可带文件开头的 UTF-8 BOM。启动时遇到缺值、括号未闭合、重复键或无法读取的编码，会先将原始文件保存到 `invalid/<时间与唯一编号>/`，再只恢复出错文件的默认设置。子目录结构保留，`errors.txt` 记录原因；其他文件的设置不变。原件保存失败时不覆盖配置。

普通配置项填错类型或超出范围时，启动使用该项默认值；运行中保留本次运行的有效值，其他合法修改继续生效。合法数字字符串（如 `"0.07"`）会转换为数值。自动纠正已有值前保留原件；正常启动、补充缺失项不产生错误备份。备份不自动删除。

运行中语法或编码错误会拒绝本次文件重载，继续使用已加载配置，错误文件保留原处供修正。支持直接保存和替换文件的保存方式；共享战利品仍需 `/reload` 才应用到战利品表。

战利品按整张表校验。某表概率、物品 ID、权重或数量无效时，该表沿用本次运行的有效规则；没有旧规则则暂停该表的苦旅奖励，其他有效表和钓鱼宝藏子表概率独立更新。合法的删除、概率为 `0` 或空物品列表会停止对应奖励。原版战利品不因此被移除。

配置问题会向单人玩家或服务器管理员汇总提醒一次。日志会抑制重复错误，每 60 秒最多输出 20 条详细警告；超出部分约每分钟汇总数量和涉及文件，停服时补齐尚未输出的汇总。修复后保存并按需 `/reload`；重启后不会从 `invalid/` 自动恢复历史设置。

## 常用调整

- **駅与东海道五十三次的天气条件**：固定为主世界的雨天、雪天或雷暴天气，不能再选择“任意天气”或“仅雷暴”。旧 `items.eki.weather`、`items.tokaido.weather` 会在加载新版配置时自动移除；其他合法设置保留。当地降水与露天要求仍由原有选项控制，唤雨期间也使用同一套被动生效条件。

- **效果强度与触发条件**：在对应主题文件中修改。概率通常用 `0`～`1` 表示，例如 `0.5` 为 50%；具体单位和范围以注释为准。
- **名称光晕**：在 `client.toml` 的 `nameEffects` 中调整，可选 `HIGH`、`MEDIUM`、`LOW` 或 `OFF`。
- **路引搜索**：在 `utility_items.toml` 的 `items.wayguide` 中调整。扩大搜索范围、缩小采样间隔会增加搜索开销；搜索失败不代表整个维度都没有目标。
- **大量效果或奖励造成卡顿**：查看 `general.toml` 的 `effectLimits` 和 `rewardDelivery`，限制效果触发次数或分批发放奖励。

## 创造与观察者模式的负面效果

以下配置键控制负面效果是否生效，默认均为 `false`，即豁免这两种模式的玩家；改为 `true` 才使对应负面作用生效，其他触发条件不变。生存与冒险模式不受这些开关影响。

| 文件 | 配置键 | 控制内容 |
| --- | --- | --- |
| `general.toml` | `malice.affectCreativeAndSpectator` | 苦旅的全部恶意惩罚 |
| `aspects/deep_valley.toml` | `items.phosphophyllite.damageAffectsCreativeAndSpectator` | 磷叶石的受伤倍率 |
| `aspects/abyss.toml` | `items.midnight_lafite.nauseaExtensionAffectsCreativeAndSpectator` | 午夜拉菲延长反胃时长 |
| `aspects/deep_valley.toml` | `items.azrael.selfExecutionAffectsCreativeAndSpectator` | 死告天使 处决佩戴者自身 |
| `aspects/deep_valley.toml` | `items.azrael.targetExecutionAffectsCreativeAndSpectator` | 死告天使 处决其他玩家 |
| `aspects/boundless.toml` | `items.boundless.star.affectCreativeAndSpectator` | 星之声冻结范围内玩家 |
| `aspects/flourishing.toml` | `items.spring_corner.harmfulEffectsAffectCreativeAndSpectator` | 春日一隅施加中毒、凋零，以及丁香延长负面效果 |
| `aspects/flourishing.toml` | `items.spring_corner.retaliationAffectsCreativeAndSpectator` | 春日一隅的玫瑰丛反伤 |
| `aspects/far_reach.toml` | `items.icarus.ignitionAffectsCreativeAndSpectator` | 伊卡洛斯点燃目标 |

苦旅的模式豁免不会改变旅程进度、恶意／见证状态或饰品联动。进入豁免模式后，现有的僵硬、视觉剥夺与飞行减速会解除。饰品开关按受到负面效果的玩家判断；不会清除已经存在的原版中毒、凋零或燃烧，以免影响其他来源的效果。

## 战利品设置

饰品与道具奖励在 `general.toml` 的 `loot` 中调整，按战利品表 ID 分别设置：

| 配置项 | 含义 |
| --- | --- |
| `enabled` | 饰品与道具奖励池开关，不控制材料与精粹来源；同时控制完美流体钓鱼 |
| 每张表的 `chance` | 该表抽中苦旅奖池的概率，`0`～`1`，设为 `0` 关闭 |
| 每张表的 `items` | 奖池条目，格式为 `物品ID\|权重\|最小数量\|最大数量` |

每次抽中奖池后，按权重选中一种物品，不替换原版奖励。增加条目只会改变各物品的占比，不会提高奖池的总概率。

保存后执行 `/reload` 或重启；已经生成内容的箱子不会重新抽取。配置有误时可查看日志中的提示，修正后再次加载。新增默认来源和物品通常需手动合并。末地城奖池有一次性迁移：仅当条目列表与旧版默认三项完全一致时，自动加入洛伦兹幻想，保留原总概率；自定义列表保持不变。迁移前备份为 `general.toml.pre-boundless-relics.bak`，完成后记录 `upgrades.boundlessRelics = 1`。

日记能力带来的额外钓鱼奖励、幻翼奖励，仍在各自主题文件中调整。

## 材料与精粹来源

三叉戟、鹦鹉螺壳、干海绵及主题精粹不使用上述饰品与道具奖励池配置。材料与精粹的获取途径及当时的概率调整见 [0.1.1-fix 更新日志](更新日志zh/0.1.1-fix.md)。

箱子与母岩奖励可以通过数据包修改，并通过 `/reload` 更新，不使用饰品奖励的 `general.toml` 配置。特殊来源的注入可能因其他数据包改变表结构而跳过；JEI 母岩页展示默认规则，不会随外部数据包自动改写。需要制作数据包时参见 [战利品数据接入说明](../开发者查阅/loot-integration.md)。

## 可选性能诊断

以下热路径统计默认关闭。排查性能时，可在启动 JVM 参数中加入 `-Dtravail.hotpath.metrics=true`，重启后生效；移除参数并重启即可关闭。

服务器停止时输出两组统计：`Travail hot path metrics` 是累计次数、累计纳秒及账本规模；`Travail hot path peaks` 是单队列记录数峰值、单批候选与恢复数量峰值，以及各阶段单次耗时峰值（纳秒）。队列峰值按单个队列计算，不是所有维度队列之和。该统计不改变处理预算或机制，不在每个 tick 写日志。

`STORAGE_INDEX_SEED_VISITS` 统计每个维度首次使用掉落物索引时的补录扫描；`STORAGE_ENTITY_VISITS` 统计校正时实际遍历的掉落物。`RELEASE_DENSE_BATCHES` 与 `RELEASE_SPARSE_BATCHES` 分别统计按已有插入顺序筛选及按候选排序的批次数。两条路径在同一时刻恢复，均保留原有恢复顺序。

另有 `-Dthelongtravail.diagnostics=true`，记录完美流体复制、食物候选查询和无穷花追加队列的统计。它通过开发接口 `RuntimeDiagnostics.snapshot()` 读取，不会随上述两组日志自动输出；一般玩家无需启用。

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

`aspects/abyss.toml` 的 `items.perfect_fluid.itemBlacklist` 按完整物品 ID 精确匹配。禁止复制名单中的物品，同时清除复制品中受支持容器及嵌套容器里的禁用物品，原件不变。《苦旅》和完美流体自身始终禁止复制，不受该列表控制。拾遗显示服务器当前名单；为空时显示“无”。

基础名单为空，检测到对应联动模组及物品后会分别补入一次：

- 车万女仆：初始魂符、已收纳女仆的魂符，不包含空魂符。
- 车万女仆：性格：怪兽战斗仪。

初始化标记位于 `items.perfect_fluid.compatInitialization`，分别为 `maidSoulAdded` 和 `monsterBattleDeviceAdded`，同一配置下的存档共用。完成后可自行删改名单，不会自动补回；将对应标记改为 `false` 后重启可重新初始化。

`items.midnight_lafite` 可分别调整生命恢复与饱和额外等级、中毒与反胃时长倍率，以及普通午夜峰值、非露天且非晴天的午夜峰值和见证夜间增伤。等级加成和增伤为 0 时禁用对应加成；时长倍率为 1 时不修改。说明同步显示服务器数值。饥饿与漂浮免疫没有单独配置开关，仅在佩戴午夜拉菲且归墟处于见证状态时生效。生命恢复等级可与穷遐加成叠加，共用效果等级上限；不会降低已有的更高等级，无限持续的中毒与反胃不受时长倍率影响。

钓鱼奖励使用 `general.toml` 的 `loot.fishing_treasure.itemChance`，默认 0.1：駅、东海道五十三次、完美流体在宝藏子表中各占 10%。归墟精粹仍占 10%，其余 60% 由原表物品按原权重分配。范围 0~0.3，0 禁用这三件物品奖励；遵循 `loot.enabled`，修改后执行 `/reload` 或重启。

午夜拉菲默认加入沉船补给箱、林地府邸各 7%，古城冰箱 10%，每次一件，使用既有 `loot.tables`。已有配置需要补入新增来源，不会因更新源码自动覆盖。


## 第六天魔王与万魂幡

`aspects/underworld.toml` 中 `items.relics.king` 配置第六天魔王的燃烧增伤、吸血、亡灵减伤、霸主及环境增伤；`items.relics.banner` 配置万魂幡容量、每魂必杀与见切系数、见切上限。收益系数为0时禁用该项。降低容量仅限制有效魂数，不删除已经保存的累计值；恢复容量后可重新使用原有魂数。实际生效值与物品详情使用服务端配置。

第六天魔王出货率由 `general.toml` 共享战利品配置管理。默认下界要塞总概率21%、三个等权条目；堡垒四类箱子各总概率14%、两个等权条目，单件均为7%。已有配置不会因为源码默认值改变而自动加入新物品，部署时需合并对应条目并调整总概率。
